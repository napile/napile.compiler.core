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
import org.napile.asm.adapters.InstructionAdapter;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.tree.members.ConstructorNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.impl.InvokeSpecialInstruction;
import org.napile.asm.tree.members.bytecode.impl.LoadInstruction;
import org.napile.asm.tree.members.bytecode.impl.PopInstruction;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileDeclarationWithBody;
import org.napile.compiler.lang.psi.NapileDelegationSpecifier;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.types.TypeUtils;

/**
 * @author VISTALL
 * @date 18:47/07.09.12
 */
public class MethodGenerator
{
	public static ConstructorNode gen(@NotNull NapileConstructor napileConstructor, @NotNull ConstructorDescriptor constructorDescriptor)
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
						constructorNode.instructions.add(new InvokeSpecialInstruction(new MethodRef(NapileLangPackage.ANY.child(ConstructorDescriptor.NAME), Collections.<TypeNode>emptyList(), TypeConstants.NULL)));
						constructorNode.instructions.add(new PopInstruction());
						break;
					default:
						throw new UnsupportedOperationException();
				}
			}
		}
		else
		{
			throw new UnsupportedOperationException();
		}

		//TODO [VISTALL] super calls

		return constructorNode;
	}

	@NotNull
	public static MethodNode gen(@NotNull MethodDescriptor methodDescriptor)
	{
		MethodNode methodNode = new MethodNode(ModifierGenerator.gen(methodDescriptor), methodDescriptor.getName().getName());
		methodNode.returnType = TypeUtils.isEqualFqName(methodDescriptor.getReturnType(), NapileLangPackage.NULL) ? null : TypeTransformer.toAsmType(methodDescriptor.getReturnType());

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
}
