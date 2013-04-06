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

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.AsmConstants;
import org.napile.asm.Modifier;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.CodeInfo;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.adapter.ReservedInstruction;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.codegen.processors.visitors.BinaryCodegenVisitor;
import org.napile.compiler.lang.NapileConstants;
import org.napile.compiler.lang.descriptors.VariableAccessorDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.psi.NapileVariableAccessor;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTrace;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @since 13:32/29.09.12
 */
public class VariableCodegen
{
	public static void getSetterAndGetter(@NotNull VariableDescriptor variableDescriptor, @Nullable NapileVariable variable, @NotNull ClassNode classNode, @NotNull BindingTrace bindingTrace, boolean noSetter)
	{
		Map<IElementType, NapileVariableAccessor> map = new HashMap<IElementType, NapileVariableAccessor>(2);
		if(variable != null)
			for(NapileVariableAccessor variableAccessor : variable.getAccessors())
				map.put(variableAccessor.getAccessorElementType(), variableAccessor);

		getGetter(variableDescriptor, classNode, bindingTrace, map.get(NapileTokens.GET_KEYWORD), variable);
		if(!noSetter)
			getSetter(variableDescriptor, classNode, bindingTrace, map.get(NapileTokens.SET_KEYWORD), variable);
	}

	private static void getSetter(@NotNull VariableDescriptor variableDescriptor, @NotNull ClassNode classNode, @NotNull BindingTrace bindingTrace, @Nullable NapileVariableAccessor variableAccessor, @Nullable NapileVariable variable)
	{
		Name accessorFq = Name.identifier(variableDescriptor.getName() + AsmConstants.ANONYM_SPLITTER + "set");

		if(variableAccessor == null)
			getSetterCode(bindingTrace, classNode, new MethodNode(ModifierCodegen.gen(variableDescriptor), accessorFq, AsmConstants.NULL_TYPE), variableDescriptor);
		else
		{
			final NapileExpression bodyExpression = variableAccessor.getBodyExpression();

			if(bodyExpression == null)
			{
				final VariableAccessorDescriptor descriptor = bindingTrace.safeGet(BindingTraceKeys.VARIABLE_SET_ACCESSOR, variableAccessor);
				final MethodNode methodNode = new MethodNode(ModifierCodegen.gen(descriptor), accessorFq, AsmConstants.NULL_TYPE);
				AnnotationCodegen.gen(bindingTrace, descriptor, methodNode, classNode);

				getSetterCode(bindingTrace, classNode, methodNode, variableDescriptor);
			}
			else
			{
				VariableAccessorDescriptor descriptor = bindingTrace.safeGet(BindingTraceKeys.VARIABLE_SET_ACCESSOR, variableAccessor);

				ExpressionCodegen codegen = new ExpressionCodegen(bindingTrace, descriptor, classNode, ExpressionCodegenContext.empty(), null);
				codegen.returnExpression(bodyExpression, false);

				MethodNode methodNode = new MethodNode(ModifierCodegen.gen(descriptor), accessorFq, AsmConstants.NULL_TYPE);
				AnnotationCodegen.gen(bindingTrace, descriptor, methodNode, classNode);

				methodNode.parameters.add(new MethodParameterNode(Modifier.list(Modifier.FINAL), NapileConstants.VARIABLE_SET_PARAMETER_NAME, TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode)));
				methodNode.code = new CodeInfo(codegen.instructs);

				classNode.addMember(methodNode);
			}
		}
	}

	private static void getSetterCode(@NotNull BindingTrace bindingTrace, @NotNull ClassNode classNode, @NotNull MethodNode setterMethodNode, @NotNull VariableDescriptor variableDescriptor)
	{
		setterMethodNode.parameters.add(new MethodParameterNode(Modifier.list(Modifier.FINAL), NapileConstants.VARIABLE_SET_PARAMETER_NAME, TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode)));

		InstructionAdapter adapter = new InstructionAdapter();

		if(!variableDescriptor.isStatic())
			adapter.visitLocalVariable("this");
		adapter.visitLocalVariable("p");

		if(variableDescriptor.isStatic())
		{
			adapter.localGet(0);
			adapter.putToStaticVar(AsmNodeUtil.ref(variableDescriptor, bindingTrace, classNode));
			adapter.putNull();
			adapter.returnValues(1);
		}
		else
		{
			adapter.localGet(0);
			adapter.localGet(1);
			adapter.putToVar(AsmNodeUtil.ref(variableDescriptor, bindingTrace, classNode));
			adapter.putNull();
			adapter.returnValues(1);
		}

		setterMethodNode.code = new CodeInfo(adapter);
		classNode.addMember(setterMethodNode);
	}

	private static void getGetter(VariableDescriptor variableDescriptor, ClassNode classNode, BindingTrace bindingTrace, NapileVariableAccessor variableAccessor, @Nullable NapileVariable variable)
	{
		Name accessorFq = Name.identifier(variableDescriptor.getName() + AsmConstants.ANONYM_SPLITTER + "get");

		if(variableAccessor == null)
			getGetterCode(classNode, new MethodNode(ModifierCodegen.gen(variableDescriptor), accessorFq, TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode)), variableDescriptor, bindingTrace, variable);
		else
		{
			final NapileExpression bodyExpression = variableAccessor.getBodyExpression();
			if(bodyExpression == null)
			{
				final VariableAccessorDescriptor descriptor = bindingTrace.safeGet(BindingTraceKeys.VARIABLE_GET_ACCESSOR, variableAccessor);
				final MethodNode methodNode = new MethodNode(ModifierCodegen.gen(descriptor), accessorFq, TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode));

				AnnotationCodegen.gen(bindingTrace, descriptor, methodNode, classNode);
				getGetterCode(classNode, methodNode, variableDescriptor, bindingTrace, variable);
			}
			else
			{
				VariableAccessorDescriptor descriptor = bindingTrace.safeGet(BindingTraceKeys.VARIABLE_GET_ACCESSOR, variableAccessor);

				ExpressionCodegen codegen = new ExpressionCodegen(bindingTrace, descriptor, classNode, ExpressionCodegenContext.empty(), null);
				codegen.returnExpression(bodyExpression, false);

				MethodNode methodNode = new MethodNode(ModifierCodegen.gen(descriptor), accessorFq, codegen.toAsmType(descriptor.getVariable().getType()));
				AnnotationCodegen.gen(bindingTrace, descriptor, methodNode, classNode);
				methodNode.code = new CodeInfo(codegen.instructs);

				classNode.addMember(methodNode);
			}
		}
	}

	private static void getGetterCode(@NotNull ClassNode classNode, @NotNull MethodNode getterMethodNode, @NotNull VariableDescriptor variableDescriptor, @NotNull BindingTrace bindingTrace, @Nullable NapileVariable variable)
	{
		//TODO [VISTALL] make LazyType, current version is not thread safe
		if(variable != null && variable.hasModifier(NapileTokens.LAZY_KEYWORD))
		{
			InstructionAdapter adapter = new InstructionAdapter();
			if(!variableDescriptor.isStatic())
				adapter.visitLocalVariable("this");

			final StackValue varStackValue = StackValue.variable(variable, bindingTrace, classNode, variableDescriptor);

			if(!variableDescriptor.isStatic())
				adapter.localGet(0);

			varStackValue.put(getterMethodNode.returnType, adapter, PositionMarker.EMPTY);

			adapter.dup();

			adapter.putNull();

			adapter.invokeVirtual(BinaryCodegenVisitor.ANY_EQUALS, false);

			adapter.putTrue();

			ReservedInstruction reservedInstruction = adapter.reserve();

			ExpressionCodegen expressionCodegen = new ExpressionCodegen(bindingTrace, null, classNode, ExpressionCodegenContext.empty(), adapter);
			if(!variableDescriptor.isStatic())
				expressionCodegen.instructs.localGet(0);

			expressionCodegen.gen(variable.getInitializer(), getterMethodNode.returnType);

			varStackValue.store(getterMethodNode.returnType, adapter, PositionMarker.EMPTY);

			if(!variableDescriptor.isStatic())
				adapter.localGet(0);

			varStackValue.put(getterMethodNode.returnType, adapter, PositionMarker.EMPTY);

			adapter.returnValues(1);

			adapter.replace(reservedInstruction).jumpIf(adapter.size());

			adapter.returnValues(1);

			getterMethodNode.code = new CodeInfo(adapter);
			classNode.addMember(getterMethodNode);
		}
		else
		{
			InstructionAdapter adapter = new InstructionAdapter();
			if(!variableDescriptor.isStatic())
				adapter.visitLocalVariable("this");

			if(variableDescriptor.isStatic())
			{
				adapter.getStaticVar(AsmNodeUtil.ref(variableDescriptor, bindingTrace, classNode));
				adapter.returnValues(1);
			}
			else
			{
				adapter.localGet(0);
				adapter.getVar(AsmNodeUtil.ref(variableDescriptor, bindingTrace, classNode));
				adapter.returnValues(1);
			}

			getterMethodNode.code = new CodeInfo(adapter);

			classNode.addMember(getterMethodNode);
		}
	}
}
