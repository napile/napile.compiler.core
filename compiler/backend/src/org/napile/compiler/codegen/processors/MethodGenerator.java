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

import org.jetbrains.annotations.NotNull;
import org.napile.asm.adapters.InstructionAdapter;
import org.napile.asm.tree.members.ConstructorNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.psi.NapileDeclarationWithBody;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.rt.NapileLangPackage;
import org.napile.compiler.lang.types.TypeUtils;

/**
 * @author VISTALL
 * @date 18:47/07.09.12
 */
public class MethodGenerator
{
	public static ConstructorNode gen(@NotNull ConstructorDescriptor constructorDescriptor)
	{
		ConstructorNode constructorNode = new ConstructorNode(ModifierGenerator.gen(constructorDescriptor));
		for(ParameterDescriptor declaration : constructorDescriptor.getValueParameters())
		{
			MethodParameterNode methodParameterNode = new MethodParameterNode(ModifierGenerator.gen(declaration), declaration.getName().getName(), TypeTransformer.toAsmType(declaration.getType()));

			constructorNode.parameters.add(methodParameterNode);
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

			int val = adapter.getMaxLocals();
			if(!methodDescriptor.isStatic())
				val ++;
			methodNode.visitMaxs(val, val);

			methodNode.instructions.addAll(adapter.getInstructions());
		}

		return methodNode;
	}
}
