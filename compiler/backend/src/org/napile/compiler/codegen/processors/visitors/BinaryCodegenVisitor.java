/*
 * Copyright 2010-2013 napile.org
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

package org.napile.compiler.codegen.processors.visitors;

import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.AsmConstants;
import org.napile.asm.lib.NapileConditionPackage;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.adapter.ReservedInstruction;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.compiler.codegen.processors.AsmNodeUtil;
import org.napile.compiler.codegen.processors.ExpressionCodegen;
import org.napile.compiler.codegen.processors.TypeTransformer;
import org.napile.compiler.codegen.processors.codegen.CallTransformer;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;
import org.napile.compiler.codegen.processors.codegen.stackValue.SimpleVariableAccessor;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileBinaryExpression;
import org.napile.compiler.lang.psi.NapileBinaryExpressionWithTypeRHS;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapilePostfixExpression;
import org.napile.compiler.lang.psi.NapilePrefixExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.expressions.OperatorConventions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @since 10:26/24.01.13
 */
public class BinaryCodegenVisitor extends CodegenVisitor
{
	public static final MethodRef ANY_EQUALS = new MethodRef(NapileLangPackage.ANY.child(Name.identifier("equals")), Collections.<MethodParameterNode>singletonList(AsmNodeUtil.parameterNode("object", TypeConstants.ANY_NULLABLE)), Collections.<TypeNode>emptyList(), AsmConstants.BOOL_TYPE);
	private static final SimpleVariableAccessor GREATER = new SimpleVariableAccessor(NapileConditionPackage.COMPARE_RESULT.child(Name.identifier("GREATER")), TypeConstants.COMPARE_RESULT, CallableMethod.CallType.STATIC);
	private static final SimpleVariableAccessor EQUAL = new SimpleVariableAccessor(NapileConditionPackage.COMPARE_RESULT.child(Name.identifier("EQUAL")), TypeConstants.COMPARE_RESULT, CallableMethod.CallType.STATIC);
	private static final SimpleVariableAccessor LOWER = new SimpleVariableAccessor(NapileConditionPackage.COMPARE_RESULT.child(Name.identifier("LOWER")), TypeConstants.COMPARE_RESULT, CallableMethod.CallType.STATIC);

	public BinaryCodegenVisitor(ExpressionCodegen gen)
	{
		super(gen);
	}

	@Override
	public StackValue visitBinaryWithTypeRHSExpression(NapileBinaryExpressionWithTypeRHS expression, StackValue data)
	{
		PsiElement refElement = expression.getOperationSign().getReferencedNameElement();
		IElementType elementType = refElement.getNode().getElementType();
		TypeNode expType = gen.expressionType(expression);

		NapileExpression leftExp = expression.getLeft();

		//FIXME [VISTALL] currently VM prototype not supported CASTs
		TypeNode leftType = gen.expressionType(expression.getLeft());
		TypeNode rightType = gen.toAsmType(gen.bindingTrace.safeGet(BindingContext.TYPE, expression.getRight()));
		if(elementType == NapileTokens.AS_KEYWORD)
		{
			gen.gen(leftExp, leftType);

			InstructionAdapter marker = gen.marker(expression.getOperationSign());
			marker.dup();

			marker.is(rightType);
			marker.putFalse();
			ReservedInstruction ifReserve = marker.reserve();
			marker.putNull();
			marker.newObject(new TypeNode(false, new ClassTypeNode(new FqName("napile.lang.ClassCastException"))), Collections.singletonList(TypeConstants.STRING_NULLABLE));
			marker.throwVal();
			marker.replace(ifReserve).jumpIf(marker.size());
		}
		else if(elementType == NapileTokens.AS_SAFE)
		{
			InstructionAdapter marker = gen.marker(expression.getOperationSign());

			gen.gen(leftExp, leftType);
			marker.dup();

			marker.is(rightType);
			marker.putFalse();
			ReservedInstruction ifReserve = marker.reserve();
			marker.putNull();
			marker.replace(ifReserve).jumpIf(gen.instructs.size());
		}
		else if(elementType == NapileTokens.COLON)
		{
			gen.gen(leftExp, leftType);
			//FIXME [VISTALL] this cast VM ill be check
		}
		return StackValue.onStack(expType);
	}

	@Override
	public StackValue visitPrefixExpression(NapilePrefixExpression expression, StackValue receiver)
	{
		DeclarationDescriptor op = gen.bindingTrace.safeGet(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
		ResolvedCall<? extends CallableDescriptor> resolvedCall = gen.bindingTrace.safeGet(BindingContext.RESOLVED_CALL, expression.getOperationReference());

		final CallableMethod callableMethod = CallTransformer.transformToCallable(gen, resolvedCall, false, false, false);

		if(!(op.getName().getName().equals("inc") || op.getName().getName().equals("dec")))
			return gen.invokeOperation(expression, (MethodDescriptor) op, callableMethod);
		else
		{
			StackValue value = gen.gen(expression.getBaseExpression());
			value.dupReceiver(gen.instructs);
			value.dupReceiver(gen.instructs);

			TypeNode type = gen.expressionType(expression.getBaseExpression());
			value.put(type, gen.instructs, gen);
			callableMethod.invoke(gen.instructs, gen, expression.getOperationReference());

			MethodDescriptor methodDescriptor = gen.bindingTrace.get(BindingContext.VARIABLE_CALL, expression);
			if(methodDescriptor != null)
				StackValue.variableAccessor(methodDescriptor, value.getType(), gen, false, expression.getBaseExpression()).store(callableMethod.getReturnType(), gen.instructs, gen);
			else
				value.store(callableMethod.getReturnType(), gen.instructs, gen);
			value.put(type, gen.instructs, gen);
			return StackValue.onStack(type);
		}
	}

	@Override
	public StackValue visitPostfixExpression(NapilePostfixExpression expression, StackValue receiver)
	{
		if(expression.getOperationReference().getReferencedNameElementType() == NapileTokens.EXCLEXCL)
			return genSure(expression, receiver);

		DeclarationDescriptor op = gen.bindingTrace.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
		if(op instanceof MethodDescriptor)
		{
			if(op.getName().getName().equals("inc") || op.getName().getName().equals("dec"))
			{
				ResolvedCall<? extends CallableDescriptor> resolvedCall = gen.bindingTrace.safeGet(BindingContext.RESOLVED_CALL, expression.getOperationReference());

				final CallableMethod callable = CallTransformer.transformToCallable(gen, resolvedCall, false, false, false);

				StackValue value = gen.gen(expression.getBaseExpression());
				value.dupReceiver(gen.instructs);

				TypeNode type = gen.expressionType(expression.getBaseExpression());
				value.put(type, gen.instructs, gen);

				switch(value.receiverSize())
				{
					case 0:
						gen.instructs.dup();
						break;
					case 1:
						gen.instructs.dup1x1();
						break;
					default:
						throw new UnsupportedOperationException("Unknown receiver size " + value.receiverSize());
				}

				callable.invoke(gen.instructs, gen, expression.getOperationReference());

				MethodDescriptor methodDescriptor = gen.bindingTrace.get(BindingContext.VARIABLE_CALL, expression);
				if(methodDescriptor != null)
					value = StackValue.variableAccessor(methodDescriptor, value.getType(), gen, false, null);
				value.store(callable.getReturnType(), gen.instructs, gen);

				return StackValue.onStack(type);
			}
		}
		throw new UnsupportedOperationException("Don't know how to generate this postfix expression");
	}

	@Override
	public StackValue visitBinaryExpression(NapileBinaryExpression expression, StackValue receiver)
	{
		final IElementType opToken = expression.getOperationReference().getReferencedNameElementType();
		if(opToken == NapileTokens.EQ)
			return genEq(expression);
		else if(OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.containsKey(opToken))
			return genAugmentedAssignment(expression);
		else if(opToken == NapileTokens.ANDAND)
			return genAndAnd(expression);
		else if(opToken == NapileTokens.OROR)
			return genOrOr(expression);
		else if(opToken == NapileTokens.EQEQ || opToken == NapileTokens.EXCLEQ)
			return genEqEq(expression);
		else if(opToken == NapileTokens.LT || opToken == NapileTokens.LTEQ || opToken == NapileTokens.GT || opToken == NapileTokens.GTEQ)
			return genGeLe(expression);
		else if(opToken == NapileTokens.ELVIS)
			return genElvis(expression);
		/*else if(opToken == NapileTokens.IN_KEYWORD || opToken == NapileTokens.NOT_IN)
		{
				return final Type exprType = expressionType(expression);
        JetType type = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression.getLeft());(expression);
		}
		else */
		else
		{
			DeclarationDescriptor op = gen.bindingTrace.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
			final CallableMethod callable = CallTransformer.transformToCallable(gen, (MethodDescriptor) op, Collections.<TypeNode>emptyList(), false, false, false);

			return gen.invokeOperation(expression, (MethodDescriptor) op, callable);
		}
	}

	public StackValue genSure(@NotNull NapilePostfixExpression expression, @NotNull StackValue receiver)
	{
		NapileExpression baseExpression = expression.getBaseExpression();
		JetType type = gen.bindingTrace.get(BindingContext.EXPRESSION_TYPE, baseExpression);
		StackValue base = gen.genQualified(receiver, baseExpression);
		if(type != null && type.isNullable())
		{
			InstructionAdapter marker = gen.marker(expression.getOperationReference());

			base.put(base.getType(), marker, gen);
			marker.dup();

			marker.putNull();

			marker.invokeVirtual(ANY_EQUALS, false);

			marker.putTrue();

			ReservedInstruction jump = marker.reserve();

			marker.newString("'" + baseExpression.getText() + "' cant be null");
			marker.newObject(TypeConstants.NULL_POINTER_EXCEPTION, Collections.<TypeNode>singletonList(TypeConstants.STRING_NULLABLE));
			marker.throwVal();

			marker.replace(jump).jumpIf(marker.size());

			return StackValue.onStack(base.getType());
		}
		else
			return base;
	}

	public StackValue genGeLe(@NotNull NapileBinaryExpression expression)
	{
		final IElementType opToken = expression.getOperationReference().getReferencedNameElementType();

		TypeNode leftType = gen.expressionType(expression.getLeft());
		TypeNode rightType = gen.expressionType(expression.getRight());

		gen.gen(expression.getLeft(), leftType);

		gen.gen(expression.getRight(), rightType);

		ClassTypeNode leftClassType = (ClassTypeNode) leftType.typeConstructorNode;
		//ClassTypeNode rightClassType = (ClassTypeNode) rightType.typeConstructorNode;

		gen.instructs.invokeVirtual(new MethodRef(leftClassType.className.child(OperatorConventions.COMPARE_TO), Collections.singletonList(AsmNodeUtil.parameterNode("object", rightType)), Collections.<TypeNode>emptyList(), TypeConstants.COMPARE_RESULT), false);

		if(opToken == NapileTokens.GT)
			gtOrLt(GREATER, gen.instructs);
		else if(opToken == NapileTokens.LT)
			gtOrLt(LOWER, gen.instructs);
		else if(opToken == NapileTokens.GTEQ)
			gtOrLtEq(GREATER, gen.instructs);
		else if(opToken == NapileTokens.LTEQ)
			gtOrLtEq(LOWER, gen.instructs);

		return StackValue.onStack(AsmConstants.BOOL_TYPE);
	}

	private void gtOrLtEq(@NotNull SimpleVariableAccessor simpleVariableAccessor, @NotNull InstructionAdapter instructs)
	{
		instructs.dup();

		simpleVariableAccessor.put(TypeConstants.COMPARE_RESULT, instructs, gen);

		instructs.invokeVirtual(ANY_EQUALS, false);

		instructs.putTrue();

		ReservedInstruction ifSlot = instructs.reserve();

		// if is equal - property - put true and jump over
		instructs.putTrue();

		ReservedInstruction jumpSlot = instructs.reserve();

		instructs.replace(ifSlot).jumpIf(instructs.size());

		// else check is is equal

		gtOrLt(EQUAL, instructs);

		instructs.replace(jumpSlot).jump(instructs.size());
	}

	private void gtOrLt(@NotNull SimpleVariableAccessor simpleVariableAccessor, @NotNull InstructionAdapter instructs)
	{
		simpleVariableAccessor.put(TypeConstants.COMPARE_RESULT, instructs, gen);

		instructs.invokeVirtual(ANY_EQUALS, false);

		instructs.putTrue();

		ReservedInstruction ifSlot = instructs.reserve();

		instructs.putTrue();

		ReservedInstruction jumpSlot = instructs.reserve();

		instructs.replace(ifSlot).jumpIf(instructs.size());

		instructs.putFalse();

		// jump - ignored else
		instructs.replace(jumpSlot).jump(instructs.size());
	}

	public StackValue genEq(@NotNull NapileBinaryExpression expression)
	{
		StackValue leftStackValue = gen.gen(expression.getLeft());

		gen.gen(expression.getRight(), leftStackValue.getType());

		MethodDescriptor methodDescriptor = gen.bindingTrace.get(BindingContext.VARIABLE_CALL, expression);
		if(methodDescriptor != null)
			leftStackValue = StackValue.variableAccessor(methodDescriptor, leftStackValue.getType(), gen, false, expression.getOperationReference());

		leftStackValue.store(leftStackValue.getType(), gen.instructs, gen);
		return StackValue.none();
	}

	public StackValue genEqEq(@NotNull NapileBinaryExpression expression)
	{
		final IElementType opToken = expression.getOperationReference().getReferencedNameElementType();

		NapileExpression left = expression.getLeft();
		NapileExpression right = expression.getRight();

		JetType leftJetType = gen.bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, left);
		TypeNode leftType = TypeTransformer.toAsmType(gen.bindingTrace, leftJetType, gen.classNode);

		JetType rightJetType = gen.bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, right);
		TypeNode rightType = TypeTransformer.toAsmType(gen.bindingTrace, rightJetType, gen.classNode);

		gen.gen(left, leftType);

		gen.gen(right, rightType);

		DeclarationDescriptor op = gen.bindingTrace.safeGet(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
		final CallableMethod callable = CallTransformer.transformToCallable(gen, (MethodDescriptor) op, Collections.<TypeNode>emptyList(), false, false, false);
		callable.invoke(gen.instructs, gen, expression.getOperationReference());

		// revert bool
		if(opToken == NapileTokens.EXCLEQ)
			gen.marker(expression.getOperationReference()).invokeVirtual(new MethodRef(NapileLangPackage.BOOL.child(Name.identifier("not")), Collections.<MethodParameterNode>emptyList(), Collections.<TypeNode>emptyList(), AsmConstants.BOOL_TYPE), false);

		return StackValue.onStack(AsmConstants.BOOL_TYPE);
	}

	public StackValue genAugmentedAssignment(@NotNull NapileBinaryExpression expression)
	{
		InstructionAdapter instructs = gen.instructs;

		final NapileExpression lhs = expression.getLeft();

		TypeNode lhsType = gen.expressionType(lhs);

		ResolvedCall<? extends CallableDescriptor> resolvedCall = gen.bindingTrace.safeGet(BindingContext.RESOLVED_CALL, expression.getOperationReference());

		final CallableMethod callable = CallTransformer.transformToCallable(gen, resolvedCall, false, false, false);

		StackValue value = gen.gen(expression.getLeft());
		value.dupReceiver(instructs);
		value.put(lhsType, instructs, gen);
		StackValue receiver = StackValue.onStack(lhsType);

		if(!(resolvedCall.getResultingDescriptor() instanceof ConstructorDescriptor))
		{ // otherwise already
			receiver = StackValue.receiver(resolvedCall, receiver, gen, callable);
			receiver.put(receiver.getType(), instructs, gen);
		}

		gen.pushMethodArguments(resolvedCall, callable.getValueParameterTypes());
		callable.invoke(instructs, gen, expression.getOperationReference());

		MethodDescriptor methodDescriptor = gen.bindingTrace.get(BindingContext.VARIABLE_CALL, expression);
		if(methodDescriptor != null)
			value = StackValue.variableAccessor(methodDescriptor, value.getType(), gen, false, expression.getOperationReference());

		value.store(callable.getReturnType(), instructs, gen);

		return StackValue.none();
	}

	public StackValue genElvis(@NotNull NapileBinaryExpression expression)
	{
		final TypeNode exprType = gen.expressionType(expression);
		JetType type = gen.bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, expression.getLeft());
		final TypeNode leftType = TypeTransformer.toAsmType(gen.bindingTrace, type, gen.classNode);

		gen.gen(expression.getLeft(), leftType);

		gen.instructs.dup();

		gen.instructs.putNull();

		gen.instructs.invokeVirtual(ANY_EQUALS, false);

		gen.instructs.putTrue();

		ReservedInstruction ifSlot = gen.instructs.reserve();

		gen.instructs.pop(); // remove null from stack(value of left exp)

		gen.gen(expression.getRight(), exprType);

		gen.instructs.replace(ifSlot).jumpIf(gen.instructs.size());

		return StackValue.onStack(exprType);
	}

	public StackValue genAndAnd(@NotNull NapileBinaryExpression expression)
	{
		InstructionAdapter instructs = gen.instructs;

		gen.gen(expression.getLeft(), AsmConstants.BOOL_TYPE);

		instructs.putTrue();

		ReservedInstruction ifSlot = instructs.reserve();

		gen.gen(expression.getRight(), AsmConstants.BOOL_TYPE);

		instructs.putTrue();

		ReservedInstruction ifSlot2 = instructs.reserve();

		instructs.putTrue();

		ReservedInstruction ignoreFalseSlot = instructs.reserve();

		// if left of right exp failed jump to false
		instructs.replace(ifSlot).jumpIf(instructs.size());
		instructs.replace(ifSlot2).jumpIf(instructs.size());

		instructs.putFalse();

		instructs.replace(ignoreFalseSlot).jump(instructs.size());

		return StackValue.onStack(AsmConstants.BOOL_TYPE);
	}

	public StackValue genOrOr(@NotNull NapileBinaryExpression expression)
	{
		InstructionAdapter instructs = gen.instructs;

		gen.gen(expression.getLeft(), AsmConstants.BOOL_TYPE);

		instructs.putTrue();

		ReservedInstruction ifSlot = instructs.reserve();

		// result
		instructs.putTrue();

		ReservedInstruction skipNextSlot = instructs.reserve();

		// is first is failed - jump to right part
		instructs.replace(ifSlot).jumpIf(instructs.size());

		gen.gen(expression.getRight(), AsmConstants.BOOL_TYPE);

		instructs.putTrue();

		ReservedInstruction ifSlot2 = instructs.reserve();

		instructs.putTrue();

		ReservedInstruction skipNextSlot2 = instructs.reserve();

		// jump to false
		instructs.replace(ifSlot2).jumpIf(instructs.size());

		// result
		instructs.putFalse();

		// skips instructions - jump over expression
		instructs.replace(skipNextSlot).jump(instructs.size());
		instructs.replace(skipNextSlot2).jump(instructs.size());

		return StackValue.onStack(AsmConstants.BOOL_TYPE);
	}
}
