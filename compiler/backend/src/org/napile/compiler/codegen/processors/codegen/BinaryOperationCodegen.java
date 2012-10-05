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

import java.util.Arrays;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.adapter.ReservedInstruction;
import org.napile.asm.tree.members.bytecode.impl.JumpIfInstruction;
import org.napile.asm.tree.members.bytecode.impl.JumpInstruction;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.compiler.codegen.processors.ExpressionGenerator;
import org.napile.compiler.codegen.processors.TypeTransformer;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.psi.NapileBinaryExpression;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.types.JetType;

/**
 * @author VISTALL
 * @date 10:50/05.10.12
 */
public class BinaryOperationCodegen
{
	private static final MethodRef ANY_EQUALS = new MethodRef(NapileLangPackage.ANY.child(Name.identifier("equals")), Arrays.asList(new TypeNode(true, new ClassTypeNode(NapileLangPackage.ANY))), Collections.<TypeNode>emptyList(), TypeConstants.BOOL);

	public static StackValue genAugmentedAssignment(@NotNull NapileBinaryExpression expression, @NotNull ExpressionGenerator gen, @NotNull InstructionAdapter instructs)
	{
		final NapileExpression lhs = expression.getLeft();

		TypeNode lhsType = gen.expressionType(lhs);

		ResolvedCall<? extends CallableDescriptor> resolvedCall = gen.bindingTrace.safeGet(BindingContext.RESOLVED_CALL, expression.getOperationReference());

		final CallableMethod callable = CallTransformer.transformToCallable(resolvedCall);

		StackValue value = gen.gen(expression.getLeft());
		value.dupReceiver(instructs);
		value.put(lhsType, instructs);
		StackValue receiver = StackValue.onStack(lhsType);

		if(!(resolvedCall.getResultingDescriptor() instanceof ConstructorDescriptor))
		{ // otherwise already
			receiver = StackValue.receiver(resolvedCall, receiver, gen, callable);
			receiver.put(receiver.getType(), instructs);
		}

		gen.pushMethodArguments(resolvedCall, callable.getValueParameterTypes());
		callable.invoke(instructs);
		value.store(callable.getReturnType(), instructs);

		return StackValue.none();
	}

	public static StackValue genElvis(@NotNull NapileBinaryExpression expression, @NotNull ExpressionGenerator gen, @NotNull InstructionAdapter instructs)
	{
		final TypeNode exprType = gen.expressionType(expression);
		JetType type = gen.bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, expression.getLeft());
		final TypeNode leftType = TypeTransformer.toAsmType(type);

		gen.gen(expression.getLeft(), leftType);

		instructs.dup();

		StackValue.putNull(instructs);

		instructs.invokeVirtual(ANY_EQUALS);

		StackValue.putTrue(instructs);

		ReservedInstruction ifSlot = instructs.reserve();

		gen.gen(expression.getRight(), exprType);

		instructs.replace(ifSlot, new JumpIfInstruction(instructs.size()));

		return StackValue.onStack(exprType);
	}

	public static StackValue genAndAnd(@NotNull NapileBinaryExpression expression, @NotNull ExpressionGenerator gen, @NotNull InstructionAdapter instructs)
	{
		gen.gen(expression.getLeft(), TypeConstants.BOOL);

		StackValue.putTrue(instructs);

		ReservedInstruction ifSlot = instructs.reserve();

		gen.gen(expression.getRight(), TypeConstants.BOOL);

		StackValue.putTrue(instructs);

		ReservedInstruction ifSlot2 = instructs.reserve();

		StackValue.putTrue(instructs);

		ReservedInstruction ignoreFalseSlot = instructs.reserve();

		// if left of right exp failed jump to false
		instructs.replace(ifSlot, new JumpIfInstruction(instructs.size()));
		instructs.replace(ifSlot2, new JumpIfInstruction(instructs.size()));

		StackValue.putFalse(instructs);

		instructs.replace(ignoreFalseSlot, new JumpInstruction(instructs.size()));

		return StackValue.onStack(TypeConstants.BOOL);
	}

	public static StackValue genOrOr(@NotNull NapileBinaryExpression expression, @NotNull ExpressionGenerator gen, @NotNull InstructionAdapter instructs)
	{
		gen.gen(expression.getLeft(), TypeConstants.BOOL);

		StackValue.putTrue(instructs);

		ReservedInstruction ifSlot = instructs.reserve();

		// result
		StackValue.putTrue(instructs);

		ReservedInstruction skipNextSlot = instructs.reserve();

		// is first is failed - jump to right part
		instructs.replace(ifSlot, new JumpIfInstruction(instructs.size()));

		gen.gen(expression.getRight(), TypeConstants.BOOL);

		StackValue.putTrue(instructs);

		ReservedInstruction ifSlot2 = instructs.reserve();

		StackValue.putTrue(instructs);

		ReservedInstruction skipNextSlot2 = instructs.reserve();

		// jump to false
		instructs.replace(ifSlot2, new JumpIfInstruction(instructs.size()));

		// result
		StackValue.putFalse(instructs);

		// skips instructions - jump over expression
		instructs.replace(skipNextSlot, new JumpInstruction(instructs.size()));
		instructs.replace(skipNextSlot2, new JumpInstruction(instructs.size()));

		return StackValue.onStack(TypeConstants.BOOL);
	}
}
