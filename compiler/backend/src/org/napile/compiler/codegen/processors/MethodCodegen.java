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
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.CodeInfo;
import org.napile.asm.tree.members.MacroNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
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
import org.napile.compiler.lang.psi.NapileCallParameter;
import org.napile.compiler.lang.psi.NapileCallParameterAsReference;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileDeclarationWithBody;
import org.napile.compiler.lang.psi.NapileDelegationSpecifierListOwner;
import org.napile.compiler.lang.psi.NapileDelegationToSuperCall;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import com.intellij.openapi.util.Pair;

/**
 * @author VISTALL
 * @date 18:47/07.09.12
 */
public class MethodCodegen
{
	public static void genSuperCalls(@NotNull InstructionAdapter adapter, @NotNull NapileDelegationSpecifierListOwner owner, @NotNull BindingTrace bindingTrace, @NotNull ClassNode classNode)
	{
		ConstructorDescriptor constructorDescriptor = bindingTrace.safeGet(BindingContext.CONSTRUCTOR, owner);
		List<NapileDelegationToSuperCall> delegationSpecifiers = owner.getDelegationSpecifiers();
		// delegation list is empty - if no extends
		for(NapileDelegationToSuperCall specifier : delegationSpecifiers)
		{
			ResolvedCall<? extends CallableDescriptor> call = bindingTrace.get(BindingContext.RESOLVED_CALL, specifier.getCalleeExpression());
			if(call == null)
				continue;

			adapter.localGet(0);

			ExpressionCodegen generator = new ExpressionCodegen(bindingTrace, constructorDescriptor, classNode, ExpressionCodegenContext.empty(), adapter);

			CallableMethod method = CallTransformer.transformToCallable(bindingTrace, classNode, call, false, false, false);

			generator.invokeMethodWithArguments(method, specifier, StackValue.none());

			adapter.pop();
		}
	}

	public static Pair<MethodNode, InstructionAdapter> genConstructor(@NotNull NapileConstructor constructor, @NotNull MethodDescriptor methodDescriptor, @NotNull BindingTrace bindingTrace, @NotNull ClassNode classNode)
	{
		MethodNode methodNode = MethodNode.constructor(ModifierCodegen.gen(methodDescriptor));

		InstructionAdapter adapter = prepareMethodToCodegen(constructor, methodDescriptor, methodNode, bindingTrace, classNode);

		genSuperCalls(adapter, constructor, bindingTrace, classNode);

		genReferenceParameters(constructor, methodDescriptor, adapter, bindingTrace, classNode);

		return new Pair<MethodNode, InstructionAdapter>(methodNode, adapter);
	}

	public static MethodNode genMethodOrMacro(@NotNull NapileNamedMethodOrMacro method, @NotNull MethodDescriptor methodDescriptor, @NotNull BindingTrace bindingTrace, @NotNull ClassNode classNode, @NotNull ExpressionCodegenContext gen)
	{
		MethodNode methodNode = methodDescriptor.isMacro() ? new MacroNode(ModifierCodegen.gen(methodDescriptor), methodDescriptor.getName(), TypeTransformer.toAsmType(bindingTrace, methodDescriptor.getReturnType(), classNode)) : new MethodNode(ModifierCodegen.gen(methodDescriptor), methodDescriptor.getName(), TypeTransformer.toAsmType(bindingTrace, methodDescriptor.getReturnType(), classNode));

		InstructionAdapter adapter = prepareMethodToCodegen(method, methodDescriptor, methodNode, bindingTrace, classNode);

		genReferenceParameters(method, methodDescriptor, adapter, bindingTrace, classNode);

		genBody(adapter, methodDescriptor, method, bindingTrace, classNode, gen);

		// hack
		if(methodDescriptor.isNative())
			methodNode.code = null;
		else
			methodNode.code = new CodeInfo(adapter);

		return methodNode;
	}

	public static InstructionAdapter prepareMethodToCodegen(@NotNull NapileDeclarationWithBody declarationWithBody, @NotNull MethodDescriptor methodDescriptor, @NotNull MethodNode methodNode, @NotNull BindingTrace bindingTrace, @NotNull ClassNode classNode)
	{
		InstructionAdapter instructionAdapter = new InstructionAdapter();
		if(!methodDescriptor.isStatic())
			instructionAdapter.visitLocalVariable("this");

		TypeParameterCodegen.gen(methodDescriptor.getTypeParameters(), methodNode, bindingTrace, classNode);

		final NapileCallParameter[] callParameters = declarationWithBody.getCallParameters();
		final List<CallParameterDescriptor> valueParameters = methodDescriptor.getValueParameters();
		for(int i = 0; i < callParameters.length; i++)
		{
			NapileCallParameter parameter = callParameters[i];
			CallParameterDescriptor descriptor = valueParameters.get(i);

			methodNode.parameters.add(new MethodParameterNode(ModifierCodegen.gen(descriptor), descriptor.getName(), TypeTransformer.toAsmType(bindingTrace, descriptor.getType(), classNode), ExpressionToQualifiedExpressionVisitor.convert(parameter.getDefaultValue())));
		}
		return instructionAdapter;
	}

	public static void genBody(@NotNull InstructionAdapter adapter, @NotNull MethodDescriptor methodDescriptor, @NotNull NapileDeclarationWithBody declarationWithBody, @NotNull BindingTrace bindingTrace, @NotNull ClassNode classNode, @NotNull ExpressionCodegenContext gen)
	{
		NapileExpression expression = declarationWithBody.getBodyExpression();
		if(expression != null)
		{
			ExpressionCodegen expressionCodegen = new ExpressionCodegen(bindingTrace, methodDescriptor, classNode, gen, adapter);
			expressionCodegen.returnExpression(expression, methodDescriptor.isMacro());
		}
	}

	public static void genReferenceParameters(@NotNull NapileDeclarationWithBody declarationWithBody, @NotNull CallableDescriptor callableDescriptor, InstructionAdapter adapter, @NotNull BindingTrace bindingTrace, @NotNull ClassNode classNode)
	{
		for(CallParameterDescriptor parameterDescriptor : callableDescriptor.getValueParameters())
			if(parameterDescriptor instanceof CallParameterAsReferenceDescriptorImpl)
			{
				NapileCallParameterAsReference refParameter = (NapileCallParameterAsReference) declarationWithBody.getCallParameters()[parameterDescriptor.getIndex()];
				MethodDescriptor resolvedSetter = bindingTrace.safeGet(BindingContext.VARIABLE_CALL, refParameter.getReferenceExpression());
				VariableDescriptor variableDescriptor = (VariableDescriptor) bindingTrace.safeGet(BindingContext.REFERENCE_TARGET, refParameter.getReferenceExpression());

				TypeNode typeNode = TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode);

				if(!variableDescriptor.isStatic())
					StackValue.local(null, 0, typeNode).put(AsmConstants.ANY_TYPE, adapter, PositionMarker.EMPTY);

				StackValue.local(null, callableDescriptor.isStatic() ? 0 : 1 + parameterDescriptor.getIndex(), typeNode).put(typeNode, adapter, PositionMarker.EMPTY);

				StackValue.variableAccessor(null, resolvedSetter, typeNode, bindingTrace, classNode, false).store(typeNode, adapter, PositionMarker.EMPTY);
			}
	}
}
