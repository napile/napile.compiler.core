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
import org.napile.asm.AsmConstants;
import org.napile.asm.Modifier;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.adapter.ReservedInstruction;
import org.napile.asm.tree.members.bytecode.impl.GetStaticVariableInstruction;
import org.napile.asm.tree.members.bytecode.impl.GetVariableInstruction;
import org.napile.asm.tree.members.bytecode.impl.JumpIfInstruction;
import org.napile.asm.tree.members.bytecode.impl.LoadInstruction;
import org.napile.asm.tree.members.bytecode.impl.ReturnInstruction;
import org.napile.compiler.codegen.processors.codegen.BinaryOperationCodegen;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.VariableAccessorDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptorImpl;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.psi.NapileVariableAccessor;
import org.napile.compiler.lang.resolve.BindingTrace;

/**
 * @author VISTALL
 * @date 13:32/29.09.12
 */
public class VariableCodegen
{
	public static void getSetterAndGetter(@NotNull VariableDescriptorImpl variableDescriptor, @NotNull NapileVariable variable, @NotNull ClassNode classNode, @NotNull BindingTrace bindingTrace)
	{
		for(NapileVariableAccessor variableAccessor : variable.getAccessors())
		{
			if(variableAccessor.getAccessorElementType() == NapileTokens.SET_KEYWORD)
				getSetter(variableDescriptor, classNode, bindingTrace, variableAccessor, variable);
			else if(variableAccessor.getAccessorElementType() == NapileTokens.GET_KEYWORD)
				getGetter(variableDescriptor, classNode, bindingTrace, variableAccessor, variable);
		}
	}

	private static void getSetter(VariableDescriptorImpl variableDescriptor, ClassNode classNode, BindingTrace bindingTrace, NapileVariableAccessor variableAccessor, NapileVariable variable)
	{
		VariableAccessorDescriptor accessorDescriptor = variableDescriptor.getSetter();
		FqName setterFq = bindingTrace.safeGet(BindingContext2.DECLARATION_TO_FQ_NAME, variableAccessor);

		if(accessorDescriptor.isDefault())
		{
			MethodNode setterMethodNode = new MethodNode(ModifierCodegen.gen(variableDescriptor), setterFq.shortName());
			setterMethodNode.returnType = AsmConstants.NULL_TYPE;
			setterMethodNode.parameters.add(new MethodParameterNode(Modifier.list(Modifier.FINAL), Name.identifier("value"), TypeTransformer.toAsmType(variableDescriptor.getType())));

			InstructionAdapter instructions = new InstructionAdapter();
			if(variableDescriptor.isStatic())
			{
				instructions.load(0);
				instructions.putToStaticVar(NodeRefUtil.ref(variableDescriptor));
				instructions.putNull();
				instructions.returnVal();
			}
			else
			{
				instructions.load(0);
				instructions.load(1);
				instructions.putToVar(NodeRefUtil.ref(variableDescriptor));
				instructions.putNull();
				instructions.returnVal();
			}

			setterMethodNode.instructions.addAll(instructions.getInstructions());
			setterMethodNode.maxLocals = variableDescriptor.isStatic() ? 1 : 2;

			classNode.members.add(setterMethodNode);
		}
		else
		{
			//TODO [VISTALL] make it
			throw new UnsupportedOperationException("Variable accessors if not supported");
		}
	}

	private static void getGetter(VariableDescriptorImpl variableDescriptor, ClassNode classNode, BindingTrace bindingTrace, NapileVariableAccessor variableAccessor, NapileVariable variable)
	{
		VariableAccessorDescriptor accessorDescriptor = variableDescriptor.getGetter();
		FqName getterFq = bindingTrace.get(BindingContext2.DECLARATION_TO_FQ_NAME, variableAccessor);

		if(accessorDescriptor.isDefault())
		{
			//TODO [VISTALL] make LazyType, current version is not thread safe
			if(variable.hasModifier(NapileTokens.LAZY_KEYWORD))
			{
				MethodNode getterMethodNode = new MethodNode(ModifierCodegen.gen(variableDescriptor), getterFq.shortName());
				getterMethodNode.returnType = TypeTransformer.toAsmType(variableDescriptor.getType());

				InstructionAdapter adapter = new InstructionAdapter();
				if(!variableDescriptor.isStatic())
					adapter.visitLocalVariable("this");

				final StackValue varStackValue = StackValue.variable(variableDescriptor);

				if(!variableDescriptor.isStatic())
					adapter.load(0);

				varStackValue.put(getterMethodNode.returnType, adapter);

				adapter.dup();

				adapter.putNull();

				adapter.invokeVirtual(BinaryOperationCodegen.ANY_EQUALS, false);

				adapter.putTrue();

				ReservedInstruction reservedInstruction = adapter.reserve();

				ExpressionCodegen expressionCodegen = new ExpressionCodegen(bindingTrace, variableDescriptor);
				if(!variableDescriptor.isStatic())
					expressionCodegen.getInstructs().load(0);

				expressionCodegen.gen(variable.getInitializer(), getterMethodNode.returnType);

				adapter.getInstructions().addAll(expressionCodegen.getInstructs().getInstructions());

				varStackValue.store(getterMethodNode.returnType, adapter);

				if(!variableDescriptor.isStatic())
					adapter.load(0);

				varStackValue.put(getterMethodNode.returnType, adapter);

				adapter.returnVal();

				adapter.replace(reservedInstruction, new JumpIfInstruction(adapter.size()));

				adapter.returnVal();

				getterMethodNode.putInstructions(adapter);
				classNode.members.add(getterMethodNode);
			}
			else
			{
				MethodNode getterMethodNode = new MethodNode(ModifierCodegen.gen(variableDescriptor), getterFq.shortName());
				getterMethodNode.returnType = TypeTransformer.toAsmType(variableDescriptor.getType());

				if(variableDescriptor.isStatic())
				{
					getterMethodNode.instructions.add(new GetStaticVariableInstruction(NodeRefUtil.ref(variableDescriptor)));
					getterMethodNode.instructions.add(new ReturnInstruction());
				}
				else
				{
					getterMethodNode.instructions.add(new LoadInstruction(0));
					getterMethodNode.instructions.add(new GetVariableInstruction(NodeRefUtil.ref(variableDescriptor)));
					getterMethodNode.instructions.add(new ReturnInstruction());
				}

				getterMethodNode.maxLocals = variableDescriptor.isStatic() ? 0 : 1;

				classNode.members.add(getterMethodNode);
			}
		}
		else
		{
			//TODO [VISTALL] make it
			throw new UnsupportedOperationException("Variable accessors if not supported");
		}
	}
}
