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
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.AsmConstants;
import org.napile.asm.Modifier;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.adapter.ReservedInstruction;
import org.napile.asm.tree.members.bytecode.impl.GetStaticVariableInstruction;
import org.napile.asm.tree.members.bytecode.impl.GetVariableInstruction;
import org.napile.asm.tree.members.bytecode.impl.LoadInstruction;
import org.napile.asm.tree.members.bytecode.impl.ReturnInstruction;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.psi.NapileVariableAccessor;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @date 13:32/29.09.12
 */
public class VariableCodegen
{
	public static void getSetterAndGetter(@NotNull VariableDescriptor variableDescriptor, @Nullable NapileVariable variable, @NotNull ClassNode classNode, @NotNull BindingTrace bindingTrace)
	{
		Map<IElementType, NapileVariableAccessor> map = new HashMap<IElementType, NapileVariableAccessor>(2);
		if(variable != null)
			for(NapileVariableAccessor variableAccessor : variable.getAccessors())
				map.put(variableAccessor.getAccessorElementType(), variableAccessor);

		getGetter(variableDescriptor, classNode, bindingTrace, map.get(NapileTokens.GET_KEYWORD), variable);
		getSetter(variableDescriptor, classNode, bindingTrace, map.get(NapileTokens.SET_KEYWORD), variable);
	}

	private static void getSetter(@NotNull VariableDescriptor variableDescriptor, @NotNull ClassNode classNode, @NotNull BindingTrace bindingTrace, @Nullable NapileVariableAccessor variableAccessor, @Nullable NapileVariable variable)
	{
		Name accessorFq = Name.identifier(variableDescriptor.getName() + AsmConstants.ANONYM_SPLITTER + "set");

		if(variableAccessor == null)
			getSetterCode(classNode, new MethodNode(ModifierCodegen.gen(variableDescriptor), accessorFq, AsmConstants.NULL_TYPE), variableDescriptor);
		else if(variableAccessor.getBodyExpression() == null)
			getSetterCode(classNode, new MethodNode(ModifierCodegen.gen(bindingTrace.safeGet(BindingContext.VARIABLE_SET_ACCESSOR, variableAccessor)), accessorFq, AsmConstants.NULL_TYPE), variableDescriptor);
		else
			throw new UnsupportedOperationException("Variable accessors with body is not supported");
	}

	private static void getSetterCode(@NotNull ClassNode classNode, @NotNull MethodNode setterMethodNode, @NotNull VariableDescriptor variableDescriptor)
	{
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

		classNode.addMember(setterMethodNode);
	}

	private static void getGetter(VariableDescriptor variableDescriptor, ClassNode classNode, BindingTrace bindingTrace, NapileVariableAccessor variableAccessor, @Nullable NapileVariable variable)
	{
		Name accessorFq = Name.identifier(variableDescriptor.getName() + AsmConstants.ANONYM_SPLITTER + "get");

		if(variableAccessor == null)
			getGetterCode(classNode, new MethodNode(ModifierCodegen.gen(variableDescriptor), accessorFq, TypeTransformer.toAsmType(variableDescriptor.getType())), variableDescriptor, bindingTrace, variable);
		else if(variableAccessor.getBodyExpression() == null)
			getGetterCode(classNode, new MethodNode(ModifierCodegen.gen(bindingTrace.safeGet(BindingContext.VARIABLE_SET_ACCESSOR, variableAccessor)), accessorFq, TypeTransformer.toAsmType(variableDescriptor.getType())), variableDescriptor, bindingTrace, variable);
		else
			throw new UnsupportedOperationException("Variable accessors with body is not supported");
	}

	private static void getGetterCode(@NotNull ClassNode classNode, @NotNull MethodNode getterMethodNode, @NotNull VariableDescriptor variableDescriptor, @NotNull BindingTrace bindingTrace, @Nullable NapileVariable variable)
	{
		//TODO [VISTALL] make LazyType, current version is not thread safe
		if(variable != null && variable.hasModifier(NapileTokens.LAZY_KEYWORD))
		{
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

			ExpressionCodegen expressionCodegen = new ExpressionCodegen(bindingTrace, variableDescriptor, classNode, Collections.<VariableDescriptor, StackValue>emptyMap());
			if(!variableDescriptor.isStatic())
				expressionCodegen.getInstructs().load(0);

			expressionCodegen.gen(variable.getInitializer(), getterMethodNode.returnType);

			adapter.getInstructions().addAll(expressionCodegen.getInstructs().getInstructions());

			varStackValue.store(getterMethodNode.returnType, adapter);

			if(!variableDescriptor.isStatic())
				adapter.load(0);

			varStackValue.put(getterMethodNode.returnType, adapter);

			adapter.returnVal();

			adapter.replace(reservedInstruction).jumpIf(adapter.size());

			adapter.returnVal();

			getterMethodNode.putInstructions(adapter);
			classNode.addMember(getterMethodNode);
		}
		else
		{
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

			classNode.addMember(getterMethodNode);
		}
	}
}
