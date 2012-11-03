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
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.TypeTransformer;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.Visibility;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.resolve.calls.inference.ConstraintSystem;
import org.napile.compiler.lang.resolve.calls.inference.TypeConstraints;
import org.napile.compiler.lang.types.JetType;

/**
 * @author VISTALL
 * @date 14:59/18.09.12
 */
public class CallTransformer
{
	public static CallableMethod transformToCallable(ResolvedCall<? extends CallableDescriptor> resolvedCall)
	{
		MethodDescriptor fd = (MethodDescriptor) resolvedCall.getResultingDescriptor();
		fd = unwrapFakeOverride(fd);

		List<TypeNode> typeArguments = new ArrayList<TypeNode>(fd.getTypeParameters().size());

		for(JetType type : resolvedCall.getTypeArguments().values())
			typeArguments.add(TypeTransformer.toAsmType(type));

		ConstraintSystem constraintSystem = resolvedCall.getConstraintSystem();
		if(constraintSystem != null && constraintSystem.isSuccessful())
		{
			assert typeArguments.size() == 0;

			for(TypeParameterDescriptor typeParameterDescriptor : constraintSystem.getTypeVariables())
			{
				TypeConstraints typeConstants = constraintSystem.getTypeConstraints(typeParameterDescriptor);

				assert typeConstants != null && typeConstants.getUpperBounds().size() == 1;

				typeArguments.add(TypeTransformer.toAsmType(typeConstants.getUpperBounds().iterator().next()));
			}
		}

		return transformToCallable(fd, typeArguments);
	}

	@SuppressWarnings("unchecked")
	public static <T extends CallableMemberDescriptor> T unwrapFakeOverride(T member)
	{
		while(member.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
			member = (T) member.getOverriddenDescriptors().iterator().next();
		return member;
	}

	@NotNull
	public static CallableMethod transformToCallable(MethodDescriptor methodDescriptor, List<TypeNode> typeArguments)
	{
		CallableMethod.CallType type = CallableMethod.CallType.VIRTUAL;
		if(methodDescriptor instanceof ConstructorDescriptor || methodDescriptor.getVisibility() == Visibility.LOCAL)
			type = CallableMethod.CallType.SPECIAL;
		else if(methodDescriptor.isStatic())
			type = CallableMethod.CallType.STATIC;

		FqName fqName = DescriptorUtils.getFQName(methodDescriptor).toSafe();

		MethodDescriptor originalMethodDescriptor = unwrapFakeOverride(methodDescriptor).getOriginal();

		// it used for save in bytecode/checks - for example, original 'E'(type parameter) and caller is 'napile.lang.Int'
		List<TypeNode> parametersToByteCode = new ArrayList<TypeNode>(originalMethodDescriptor.getValueParameters().size());
		List<TypeNode> parametersToChecks = new ArrayList<TypeNode>(originalMethodDescriptor.getValueParameters().size());

		for(ParameterDescriptor p : methodDescriptor.getValueParameters())
			parametersToChecks.add(TypeTransformer.toAsmType(p.getType()));

		for(ParameterDescriptor p : originalMethodDescriptor.getValueParameters())
			parametersToByteCode.add(TypeTransformer.toAsmType(p.getType()));

		return new CallableMethod(new MethodRef(fqName, parametersToByteCode, typeArguments, TypeTransformer.toAsmType(originalMethodDescriptor.getReturnType())), type, TypeTransformer.toAsmType(methodDescriptor.getReturnType()), parametersToChecks);
	}
}
