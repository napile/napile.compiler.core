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

package org.napile.compiler.codegen.processors;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.tree.members.ConstructorNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.TypeParameterNode;
import org.napile.asm.tree.members.bytecode.Instruction;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.impl.InvokeSpecialInstruction;
import org.napile.asm.tree.members.bytecode.impl.LoadInstruction;
import org.napile.asm.tree.members.bytecode.impl.PopInstruction;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.codegen.CallTransformer;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.ReferenceParameterDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.psi.NapileCallElement;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileDeclarationWithBody;
import org.napile.compiler.lang.psi.NapileDelegationSpecifier;
import org.napile.compiler.lang.psi.NapileDelegatorToSuperCall;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeUtils;

/**
 * @author VISTALL
 * @date 18:47/07.09.12
 */
public class MethodGenerator
{
	public static ConstructorNode gen(@NotNull NapileConstructor napileConstructor, @NotNull ConstructorDescriptor constructorDescriptor, @NotNull BindingTrace bindingTrace)
	{
		ConstructorNode constructorNode = new ConstructorNode(ModifierGenerator.gen(constructorDescriptor));
		for(ParameterDescriptor declaration : constructorDescriptor.getValueParameters())
		{
			MethodParameterNode methodParameterNode = new MethodParameterNode(ModifierGenerator.gen(declaration), declaration.getName().getName(), TypeTransformer.toAsmType(declaration.getType()));

			constructorNode.parameters.add(methodParameterNode);
		}

		List<NapileDelegationSpecifier> delegationSpecifiers = napileConstructor.getDelegationSpecifiers();
		// delegation list is empty - if no extends
		if(delegationSpecifiers.isEmpty())
		{
			ClassDescriptor classDescriptor = constructorDescriptor.getContainingDeclaration();
			// napile.lang.Any cant call self constructor
			if(!DescriptorUtils.getFQName(classDescriptor).equals(NapileLangPackage.ANY))
			{
				switch(classDescriptor.getKind())
				{
					case CLASS:
						constructorNode.instructions.add(new LoadInstruction(0));
						constructorNode.instructions.add(new InvokeSpecialInstruction(new MethodRef(NapileLangPackage.ANY.child(ConstructorDescriptor.NAME), Collections.<TypeNode>emptyList(),Collections.<TypeNode>emptyList(), TypeConstants.NULL)));
						constructorNode.instructions.add(new PopInstruction());
						break;
					default:
						throw new UnsupportedOperationException();
				}
			}
		}
		else
		{
			for(NapileDelegationSpecifier specifier : delegationSpecifiers)
			{
				if(specifier instanceof NapileDelegatorToSuperCall)
				{
					ResolvedCall<? extends CallableDescriptor> call = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, ((NapileDelegatorToSuperCall) specifier).getCalleeExpression());

					ExpressionGenerator generator = new ExpressionGenerator(bindingTrace, constructorDescriptor);

					CallableMethod method = CallTransformer.transformToCallable(call);

					generator.invokeMethodWithArguments(method, (NapileCallElement) specifier, StackValue.none());

					constructorNode.instructions.add(new LoadInstruction(0));
					constructorNode.instructions.addAll(generator.getInstructs().getInstructions());
					constructorNode.instructions.add(new PopInstruction());
				}
				else
					throw new UnsupportedOperationException(specifier.getClass().toString());
			}
		}

		return constructorNode;
	}

	@NotNull
	public static MethodNode gen(@NotNull MethodDescriptor methodDescriptor)
	{
		MethodNode methodNode = new MethodNode(ModifierGenerator.gen(methodDescriptor), methodDescriptor.getName().getName());
		methodNode.returnType = TypeUtils.isEqualFqName(methodDescriptor.getReturnType(), NapileLangPackage.NULL) ? null : TypeTransformer.toAsmType(methodDescriptor.getReturnType());

		for(TypeParameterDescriptor typeParameterDescriptor : methodDescriptor.getTypeParameters())
		{
			TypeParameterNode typeParameterNode = new TypeParameterNode(typeParameterDescriptor.getName().getName());
			for(JetType superType : typeParameterDescriptor.getUpperBounds())
				typeParameterNode.supers.add(TypeTransformer.toAsmType(superType));

			methodNode.typeParameters.add(typeParameterNode);
		}

		for(ParameterDescriptor declaration : methodDescriptor.getValueParameters())
		{
			MethodParameterNode methodParameterNode = new MethodParameterNode(ModifierGenerator.gen(declaration), declaration.getName().getName(), TypeTransformer.toAsmType(declaration.getType()));

			methodNode.parameters.add(methodParameterNode);
		}

		return methodNode;
	}

	@NotNull
	public static MethodNode gen(@NotNull MethodDescriptor methodDescriptor, @NotNull NapileDeclarationWithBody declarationWithBody, @NotNull BindingTrace bindingTrace)
	{
		MethodNode methodNode = gen(methodDescriptor);

		genReferenceParameters(methodDescriptor, methodNode.instructions);

		NapileExpression expression = declarationWithBody.getBodyExpression();
		if(expression != null)
		{
			ExpressionGenerator expressionGenerator = new ExpressionGenerator(bindingTrace, methodDescriptor);
			expressionGenerator.returnExpression(expression);

			InstructionAdapter adapter = expressionGenerator.getInstructs();

			int val = adapter.getMaxLocals() + methodDescriptor.getValueParameters().size();
			if(!methodDescriptor.isStatic())
				val ++;
			methodNode.visitMaxs(val, val);

			methodNode.instructions.addAll(adapter.getInstructions());
		}

		return methodNode;
	}

	public static void genReferenceParameters(@NotNull CallableDescriptor callableDescriptor, List<Instruction> instructions)
	{
		InstructionAdapter adapter = new InstructionAdapter();
		for(ParameterDescriptor parameterDescriptor : callableDescriptor.getValueParameters())
			if(parameterDescriptor instanceof ReferenceParameterDescriptor)
			{
				PropertyDescriptor propertyDescriptor = ((ReferenceParameterDescriptor) parameterDescriptor).getReferenceProperty();
				if(propertyDescriptor == null)
					continue;

				TypeNode typeNode = TypeTransformer.toAsmType(propertyDescriptor.getType());

				if(!propertyDescriptor.isStatic())
					StackValue.local(0, typeNode).put(TypeConstants.ANY, adapter);
				StackValue.local(callableDescriptor.isStatic() ? 0 : 1 + parameterDescriptor.getIndex(), typeNode).put(typeNode, adapter);
				StackValue.property(propertyDescriptor).store(typeNode, adapter);
			}

		instructions.addAll(adapter.getInstructions());
	}
}
