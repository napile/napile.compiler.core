/*
 * Copyright 2010-2012 napile.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.napile.compiler.codegen.processors.codegen;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.ExpressionCodegen;
import org.napile.compiler.codegen.processors.FqNameGenerator;
import org.napile.compiler.codegen.processors.ModifierCodegen;
import org.napile.compiler.codegen.processors.TypeTransformer;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.Visibility;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileSafeQualifiedExpression;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.resolve.calls.inference.ConstraintSystem;
import org.napile.compiler.lang.resolve.calls.inference.TypeConstraints;
import org.napile.compiler.lang.types.JetType;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @date 14:59/18.09.12
 */
public class CallTransformer
{
	public static CallableMethod transformToCallable(ExpressionCodegen gen, ResolvedCall<? extends CallableDescriptor> resolvedCall, boolean nullable, boolean anonym, boolean requireSpecialCall)
	{
		return transformToCallable(gen.bindingTrace, gen.classNode, resolvedCall, nullable, anonym, requireSpecialCall);
	}

	public static CallableMethod transformToCallable(BindingTrace bindingTrace, ClassNode classNode, ResolvedCall<? extends CallableDescriptor> resolvedCall, boolean nullable, boolean anonym, boolean requireSpecialCall)
	{
		MethodDescriptor fd = (MethodDescriptor) resolvedCall.getResultingDescriptor();
		fd = unwrapFakeOverride(fd);

		List<TypeNode> typeArguments = new ArrayList<TypeNode>(fd.getTypeParameters().size());

		for(JetType type : resolvedCall.getTypeArguments().values())
			typeArguments.add(TypeTransformer.toAsmType(bindingTrace, type, classNode));

		ConstraintSystem constraintSystem = resolvedCall.getConstraintSystem();
		if(constraintSystem != null && constraintSystem.isSuccessful())
		{
			assert typeArguments.size() == 0;

			for(TypeParameterDescriptor typeParameterDescriptor : constraintSystem.getTypeVariables())
			{
				TypeConstraints typeConstants = constraintSystem.getTypeConstraints(typeParameterDescriptor);

				assert typeConstants != null;

				typeArguments.add(TypeTransformer.toAsmType(bindingTrace, typeConstants.getUpperBounds().iterator().next(), classNode));
			}
		}

		return transformToCallable(bindingTrace, classNode, fd, typeArguments, nullable, anonym, requireSpecialCall);
	}

	@SuppressWarnings("unchecked")
	public static <T extends CallableMemberDescriptor> T unwrapFakeOverride(T member)
	{
		while(member.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
			member = (T) member.getOverriddenDescriptors().iterator().next();
		return member;
	}

	@NotNull
	public static CallableMethod transformToCallable(ExpressionCodegen gen, MethodDescriptor methodDescriptor, List<TypeNode> typeArguments, boolean nullable, boolean anonym, boolean requireSpecialCall)
	{
		return transformToCallable(gen.bindingTrace, gen.classNode, methodDescriptor, typeArguments, nullable, anonym, requireSpecialCall);
	}

	@NotNull
	public static CallableMethod transformToCallable(BindingTrace bindingTrace, ClassNode classNode, MethodDescriptor methodDescriptor, List<TypeNode> typeArguments, boolean nullable, boolean anonym, boolean requireSpecialCall)
	{
		CallableMethod.CallType type = CallableMethod.CallType.VIRTUAL;
		if(methodDescriptor instanceof ConstructorDescriptor || requireSpecialCall)
			type = CallableMethod.CallType.SPECIAL;
		else if(methodDescriptor.isStatic())
			type = CallableMethod.CallType.STATIC;

		if(!methodDescriptor.isStatic() && methodDescriptor.getVisibility() == Visibility.LOCAL)
			type = CallableMethod.CallType.SPECIAL;
		if(anonym)
			type = CallableMethod.CallType.ANONYM;

		FqName fqName = FqName.ROOT;

		if(type != CallableMethod.CallType.ANONYM)
			fqName = FqNameGenerator.getFqName(methodDescriptor, bindingTrace);

		MethodDescriptor originalMethodDescriptor = unwrapFakeOverride(methodDescriptor).getOriginal();

		// it used for save in bytecode/checks - for example, original 'E'(type parameter) and caller is 'napile.lang.Int'
		List<MethodParameterNode> parametersToByteCode = new ArrayList<MethodParameterNode>(originalMethodDescriptor.getValueParameters().size());
		List<TypeNode> parametersToChecks = new ArrayList<TypeNode>(originalMethodDescriptor.getValueParameters().size());

		for(CallParameterDescriptor p : methodDescriptor.getValueParameters())
			parametersToChecks.add(TypeTransformer.toAsmType(bindingTrace, p.getType(), classNode));

		for(CallParameterDescriptor p : originalMethodDescriptor.getValueParameters())
			parametersToByteCode.add(new MethodParameterNode(ModifierCodegen.gen(p), p.getName(), TypeTransformer.toAsmType(bindingTrace, p.getType(), classNode)));

		return new CallableMethod(new MethodRef(fqName, parametersToByteCode, typeArguments, TypeTransformer.toAsmType(bindingTrace, originalMethodDescriptor.getReturnType(), classNode)), type, TypeTransformer.toAsmType(bindingTrace, methodDescriptor.getReturnType(), classNode), parametersToChecks, methodDescriptor.isMacro(), nullable);
	}

	public static boolean isNullable(NapileExpression expression)
	{
		PsiElement parent = expression.getParent();
		return parent instanceof NapileSafeQualifiedExpression && ((NapileSafeQualifiedExpression) parent).getSelectorExpression() == expression;
	}
}
