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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.AsmConstants;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MacroNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.bytecode.Instruction;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.impl.LoadInstruction;
import org.napile.asm.tree.members.bytecode.impl.PopInstruction;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.codegen.CallTransformer;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.CallParameterAsReferenceDescriptorImpl;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileCallParameterAsReference;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileDeclarationWithBody;
import org.napile.compiler.lang.psi.NapileDelegationToSuperCall;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;

/**
 * @author VISTALL
 * @date 18:47/07.09.12
 */
public class MethodCodegen
{
	public static MethodNode gen(@NotNull NapileConstructor napileConstructor, @NotNull ConstructorDescriptor constructorDescriptor, @NotNull BindingTrace bindingTrace, @NotNull ClassNode classNode)
	{
		MethodNode constructorNode = MethodNode.constructor(ModifierCodegen.gen(constructorDescriptor));
		for(CallParameterDescriptor declaration : constructorDescriptor.getValueParameters())
		{
			MethodParameterNode methodParameterNode = new MethodParameterNode(ModifierCodegen.gen(declaration), declaration.getName(), TypeTransformer.toAsmType(declaration.getType()));

			constructorNode.parameters.add(methodParameterNode);
		}

		List<NapileDelegationToSuperCall> delegationSpecifiers = napileConstructor.getDelegationSpecifiers();
		// delegation list is empty - if no extends
		for(NapileDelegationToSuperCall specifier : delegationSpecifiers)
		{
			ResolvedCall<? extends CallableDescriptor> call = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, specifier.getCalleeExpression());

			ExpressionCodegen generator = new ExpressionCodegen(bindingTrace, constructorDescriptor, classNode);

			CallableMethod method = CallTransformer.transformToCallable(call, false, false, false);

			generator.invokeMethodWithArguments(method, specifier, StackValue.none());

			constructorNode.instructions.add(new LoadInstruction(0));
			constructorNode.instructions.addAll(generator.getInstructs().getInstructions());
			constructorNode.instructions.add(new PopInstruction());
		}

		return constructorNode;
	}

	@NotNull
	public static MethodNode gen(@NotNull MethodDescriptor methodDescriptor, @NotNull Name realName)
	{
		MethodNode methodNode = methodDescriptor.isMacro() ? new MacroNode(ModifierCodegen.gen(methodDescriptor), realName, TypeTransformer.toAsmType(methodDescriptor.getReturnType())) : new MethodNode(ModifierCodegen.gen(methodDescriptor), realName, TypeTransformer.toAsmType(methodDescriptor.getReturnType()));

		TypeParameterCodegen.gen(methodDescriptor.getTypeParameters(), methodNode);

		for(CallParameterDescriptor declaration : methodDescriptor.getValueParameters())
		{
			MethodParameterNode methodParameterNode = new MethodParameterNode(ModifierCodegen.gen(declaration), declaration.getName(), TypeTransformer.toAsmType(declaration.getType()));

			methodNode.parameters.add(methodParameterNode);
		}

		return methodNode;
	}

	@NotNull
	public static MethodNode gen(@NotNull MethodDescriptor methodDescriptor, @NotNull Name name, @NotNull NapileDeclarationWithBody declarationWithBody, @NotNull BindingTrace bindingTrace, @NotNull ClassNode classNode)
	{
		MethodNode methodNode = gen(methodDescriptor, name);

		genReferenceParameters(declarationWithBody, bindingTrace, methodDescriptor, methodNode.instructions);

		NapileExpression expression = declarationWithBody.getBodyExpression();
		if(expression != null)
		{
			ExpressionCodegen expressionCodegen = new ExpressionCodegen(bindingTrace, methodDescriptor, classNode);
			expressionCodegen.returnExpression(expression, methodDescriptor.isMacro());

			InstructionAdapter adapter = expressionCodegen.getInstructs();

			int val = adapter.getMaxLocals() + methodDescriptor.getValueParameters().size();
			if(!methodDescriptor.isStatic())
				val ++;
			methodNode.maxLocals = val;

			methodNode.instructions.addAll(adapter.getInstructions());
			methodNode.tryCatchBlockNodes.addAll(adapter.getTryCatchBlockNodes());
		}

		return methodNode;
	}

	public static void genReferenceParameters(@NotNull NapileDeclarationWithBody declarationWithBody, @NotNull BindingTrace trace, @NotNull CallableDescriptor callableDescriptor, List<Instruction> instructions)
	{
		InstructionAdapter adapter = new InstructionAdapter();
		for(CallParameterDescriptor parameterDescriptor : callableDescriptor.getValueParameters())
			if(parameterDescriptor instanceof CallParameterAsReferenceDescriptorImpl)
			{
				NapileCallParameterAsReference refParameter = (NapileCallParameterAsReference) declarationWithBody.getValueParameters()[parameterDescriptor.getIndex()];
				MethodDescriptor resolvedSetter = trace.safeGet(BindingContext.VARIABLE_CALL, refParameter.getReferenceExpression());
				VariableDescriptor variableDescriptor = (VariableDescriptor) trace.safeGet(BindingContext.REFERENCE_TARGET, refParameter.getReferenceExpression());

				TypeNode typeNode = TypeTransformer.toAsmType(variableDescriptor.getType());

				if(!variableDescriptor.isStatic())
					StackValue.local(0, typeNode).put(AsmConstants.ANY_TYPE, adapter);

				StackValue.local(callableDescriptor.isStatic() ? 0 : 1 + parameterDescriptor.getIndex(), typeNode).put(typeNode, adapter);

				StackValue.variableAccessor(resolvedSetter, typeNode).store(typeNode, adapter);
			}

		instructions.addAll(adapter.getInstructions());
	}
}
