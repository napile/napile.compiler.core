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
import org.napile.asm.lib.NapileConditionPackage;
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
import org.napile.compiler.codegen.processors.codegen.stackValue.Property;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.psi.NapileBinaryExpression;
import org.napile.compiler.psi.NapileExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.expressions.OperatorConventions;
import org.napile.compiler.lexer.NapileTokens;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @date 10:50/05.10.12
 */
public class BinaryOperationCodegen
{
	private static final MethodRef ANY_EQUALS = new MethodRef(NapileLangPackage.ANY.child(Name.identifier("equals")), Arrays.asList(new TypeNode(true, new ClassTypeNode(NapileLangPackage.ANY))), Collections.<TypeNode>emptyList(), TypeConstants.BOOL);

	private static final Property GREATER = new Property(NapileConditionPackage.COMPARE_RESULT.child(Name.identifier("GREATER")), TypeConstants.COMPARE_RESULT, true);
	private static final Property EQUAL = new Property(NapileConditionPackage.COMPARE_RESULT.child(Name.identifier("EQUAL")), TypeConstants.COMPARE_RESULT, true);
	private static final Property LOWER = new Property(NapileConditionPackage.COMPARE_RESULT.child(Name.identifier("LOWER")), TypeConstants.COMPARE_RESULT, true);

	public static StackValue genGeLe(@NotNull NapileBinaryExpression expression, @NotNull ExpressionGenerator gen, @NotNull InstructionAdapter instructs)
	{
		final IElementType opToken = expression.getOperationReference().getReferencedNameElementType();

		TypeNode leftType = gen.expressionType(expression.getLeft());
		TypeNode rightType = gen.expressionType(expression.getRight());

		gen.gen(expression.getLeft(), leftType);

		gen.gen(expression.getRight(), rightType);

		ClassTypeNode leftClassType = (ClassTypeNode) leftType.typeConstructorNode;
		//ClassTypeNode rightClassType = (ClassTypeNode) rightType.typeConstructorNode;

		instructs.invokeVirtual(new MethodRef(leftClassType.className.child(OperatorConventions.COMPARE_TO), Collections.singletonList(rightType), Collections.<TypeNode>emptyList(), TypeConstants.COMPARE_RESULT));

		if(opToken == NapileTokens.GT)
			gtOrLt(GREATER, instructs);
		else if(opToken == NapileTokens.LT)
			gtOrLt(LOWER, instructs);
		else if(opToken == NapileTokens.GTEQ)
			gtOrLtEq(GREATER, instructs);
		else if(opToken == NapileTokens.LTEQ)
			gtOrLtEq(LOWER, instructs);

		return StackValue.onStack(TypeConstants.BOOL);
	}

	private static void gtOrLtEq(@NotNull Property property, @NotNull InstructionAdapter instructs)
	{
		instructs.dup();

		property.put(TypeConstants.COMPARE_RESULT, instructs);

		instructs.invokeVirtual(ANY_EQUALS);

		instructs.putTrue();

		ReservedInstruction ifSlot = instructs.reserve();

		// if is equal - property - put true and jump over
		instructs.putTrue();

		ReservedInstruction jumpSlot = instructs.reserve();

		instructs.replace(ifSlot, new JumpIfInstruction(instructs.size()));

		// else check is is equal

		gtOrLt(EQUAL, instructs);

		instructs.replace(jumpSlot, new JumpInstruction(instructs.size()));
	}

	private static void gtOrLt(@NotNull Property property, @NotNull InstructionAdapter instructs)
	{
		property.put(TypeConstants.COMPARE_RESULT, instructs);

		instructs.invokeVirtual(ANY_EQUALS);

		instructs.putTrue();

		ReservedInstruction ifSlot = instructs.reserve();

		instructs.putTrue();

		ReservedInstruction jumpSlot = instructs.reserve();

		instructs.replace(ifSlot, new JumpIfInstruction(instructs.size()));

		instructs.putFalse();

		// jump - ignored else
		instructs.replace(jumpSlot, new JumpInstruction(instructs.size()));
	}

	public static StackValue genEq(@NotNull NapileBinaryExpression expression, @NotNull ExpressionGenerator gen, @NotNull InstructionAdapter instructs)
	{
		StackValue stackValue = gen.gen(expression.getLeft());
		gen.gen(expression.getRight(), stackValue.getType());
		stackValue.store(stackValue.getType(), instructs);
		return StackValue.none();
	}

	public static StackValue genEqEq(@NotNull NapileBinaryExpression expression, @NotNull ExpressionGenerator gen, @NotNull InstructionAdapter instructs)
	{
		final IElementType opToken = expression.getOperationReference().getReferencedNameElementType();

		NapileExpression left = expression.getLeft();
		NapileExpression right = expression.getRight();

		JetType leftJetType = gen.bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, left);
		TypeNode leftType = TypeTransformer.toAsmType(leftJetType);

		JetType rightJetType = gen.bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, right);
		TypeNode rightType = TypeTransformer.toAsmType(rightJetType);

		gen.gen(left, leftType);

		gen.gen(right, rightType);

		DeclarationDescriptor op = gen.bindingTrace.safeGet(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
		final CallableMethod callable = CallTransformer.transformToCallable((MethodDescriptor) op, Collections.<TypeNode>emptyList());
		callable.invoke(instructs);

		// revert bool
		if(opToken == NapileTokens.EXCLEQ)
			instructs.invokeVirtual(new MethodRef(NapileLangPackage.BOOL.child(Name.identifier("not")), Collections.<TypeNode>emptyList(), Collections.<TypeNode>emptyList(), TypeConstants.BOOL));

		return StackValue.onStack(TypeConstants.BOOL);
	}

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

		instructs.putNull();

		instructs.invokeVirtual(ANY_EQUALS);

		instructs.putTrue();

		ReservedInstruction ifSlot = instructs.reserve();

		gen.gen(expression.getRight(), exprType);

		instructs.replace(ifSlot, new JumpIfInstruction(instructs.size()));

		return StackValue.onStack(exprType);
	}

	public static StackValue genAndAnd(@NotNull NapileBinaryExpression expression, @NotNull ExpressionGenerator gen, @NotNull InstructionAdapter instructs)
	{
		gen.gen(expression.getLeft(), TypeConstants.BOOL);

		instructs.putTrue();

		ReservedInstruction ifSlot = instructs.reserve();

		gen.gen(expression.getRight(), TypeConstants.BOOL);

		instructs.putTrue();

		ReservedInstruction ifSlot2 = instructs.reserve();

		instructs.putTrue();

		ReservedInstruction ignoreFalseSlot = instructs.reserve();

		// if left of right exp failed jump to false
		instructs.replace(ifSlot, new JumpIfInstruction(instructs.size()));
		instructs.replace(ifSlot2, new JumpIfInstruction(instructs.size()));

		instructs.putFalse();

		instructs.replace(ignoreFalseSlot, new JumpInstruction(instructs.size()));

		return StackValue.onStack(TypeConstants.BOOL);
	}

	public static StackValue genOrOr(@NotNull NapileBinaryExpression expression, @NotNull ExpressionGenerator gen, @NotNull InstructionAdapter instructs)
	{
		gen.gen(expression.getLeft(), TypeConstants.BOOL);

		instructs.putTrue();

		ReservedInstruction ifSlot = instructs.reserve();

		// result
		instructs.putTrue();

		ReservedInstruction skipNextSlot = instructs.reserve();

		// is first is failed - jump to right part
		instructs.replace(ifSlot, new JumpIfInstruction(instructs.size()));

		gen.gen(expression.getRight(), TypeConstants.BOOL);

		instructs.putTrue();

		ReservedInstruction ifSlot2 = instructs.reserve();

		instructs.putTrue();

		ReservedInstruction skipNextSlot2 = instructs.reserve();

		// jump to false
		instructs.replace(ifSlot2, new JumpIfInstruction(instructs.size()));

		// result
		instructs.putFalse();

		// skips instructions - jump over expression
		instructs.replace(skipNextSlot, new JumpInstruction(instructs.size()));
		instructs.replace(skipNextSlot2, new JumpInstruction(instructs.size()));

		return StackValue.onStack(TypeConstants.BOOL);
	}
}
