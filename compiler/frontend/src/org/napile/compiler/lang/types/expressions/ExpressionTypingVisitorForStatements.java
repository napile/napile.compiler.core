/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.napile.compiler.lang.types.expressions;

import static org.napile.compiler.lang.diagnostics.Errors.ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT;
import static org.napile.compiler.lang.diagnostics.Errors.ASSIGN_OPERATOR_AMBIGUITY;
import static org.napile.compiler.lang.diagnostics.Errors.LOCAL_VARIABLE_WITH_GETTER;
import static org.napile.compiler.lang.diagnostics.Errors.LOCAL_VARIABLE_WITH_SETTER;
import static org.napile.compiler.lang.diagnostics.Errors.UNRESOLVED_REFERENCE;
import static org.napile.compiler.lang.diagnostics.Errors.UNSUPPORTED;
import static org.napile.compiler.lang.resolve.BindingContext.AMBIGUOUS_REFERENCE_TARGET;
import static org.napile.compiler.lang.resolve.BindingContext.VARIABLE_REASSIGNMENT;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.FunctionDescriptorUtil;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;
import org.napile.compiler.lang.resolve.TopDownAnalyzer;
import org.napile.compiler.lang.resolve.calls.OverloadResolutionResults;
import org.napile.compiler.lang.resolve.calls.OverloadResolutionResultsUtil;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.JetTypeInfo;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lexer.JetTokens;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

/**
 * @author abreslav
 */
@SuppressWarnings("SuspiciousMethodCalls")
public class ExpressionTypingVisitorForStatements extends ExpressionTypingVisitor
{
	private final WritableScope scope;
	private final BasicExpressionTypingVisitor basic;
	private final ControlStructureTypingVisitor controlStructures;
	private final PatternMatchingTypingVisitor patterns;

	public ExpressionTypingVisitorForStatements(@NotNull ExpressionTypingInternals facade, @NotNull WritableScope scope, BasicExpressionTypingVisitor basic, @NotNull ControlStructureTypingVisitor controlStructures, @NotNull PatternMatchingTypingVisitor patterns)
	{
		super(facade);
		this.scope = scope;
		this.basic = basic;
		this.controlStructures = controlStructures;
		this.patterns = patterns;
	}

	@Nullable
	private JetType checkAssignmentType(@Nullable JetType assignmentType, @NotNull NapileBinaryExpression expression, @NotNull ExpressionTypingContext context)
	{
		if(assignmentType != null && !TypeUtils.isEqualFqName(assignmentType, NapileLangPackage.NULL) && context.expectedType != TypeUtils.NO_EXPECTED_TYPE &&
				TypeUtils.equalTypes(context.expectedType, assignmentType))
		{
			context.trace.report(Errors.ASSIGNMENT_TYPE_MISMATCH.on(expression, context.expectedType));
			return null;
		}
		return DataFlowUtils.checkStatementType(expression, context);
	}

	@Override
	public JetTypeInfo visitAnonymClass(NapileAnonymClass declaration, ExpressionTypingContext context)
	{
		TopDownAnalyzer.processClassOrObject(context.expressionTypingServices.getProject(), context.trace, scope, scope.getContainingDeclaration(), declaration);
		ClassDescriptor classDescriptor = context.trace.getBindingContext().get(BindingContext.CLASS, declaration);
		if(classDescriptor != null)
		{
			VariableDescriptor variableDescriptor = context.expressionTypingServices.getDescriptorResolver().resolveObjectDeclaration(scope.getContainingDeclaration(), declaration, classDescriptor, context.trace);
			scope.addVariableDescriptor(variableDescriptor);
		}
		return DataFlowUtils.checkStatementType(declaration, context, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitProperty(NapileProperty property, ExpressionTypingContext context)
	{
		NapilePropertyAccessor getter = property.getGetter();
		if(getter != null)
		{
			context.trace.report(LOCAL_VARIABLE_WITH_GETTER.on(getter));
		}

		NapilePropertyAccessor setter = property.getSetter();
		if(setter != null)
		{
			context.trace.report(LOCAL_VARIABLE_WITH_SETTER.on(setter));
		}

		VariableDescriptor propertyDescriptor = context.expressionTypingServices.getDescriptorResolver().resolveLocalVariableDescriptor(scope.getContainingDeclaration(), scope, property, context.dataFlowInfo, context.trace);
		NapileExpression initializer = property.getInitializer();
		if(property.getPropertyTypeRef() != null && initializer != null)
		{
			JetType outType = propertyDescriptor.getType();
			JetType initializerType = facade.getTypeInfo(initializer, context.replaceExpectedType(outType).replaceScope(scope)).getType();
		}

		{
			VariableDescriptor olderVariable = scope.getLocalVariable(propertyDescriptor.getName());
			if(olderVariable != null && DescriptorUtils.isLocal(propertyDescriptor.getContainingDeclaration(), olderVariable))
			{
				PsiElement declaration = BindingContextUtils.descriptorToDeclaration(context.trace.getBindingContext(), propertyDescriptor);
				context.trace.report(Errors.NAME_SHADOWING.on(declaration, propertyDescriptor.getName().getName()));
			}
		}

		scope.addVariableDescriptor(propertyDescriptor);
		return DataFlowUtils.checkStatementType(property, context, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitNamedFunction(NapileNamedFunction function, ExpressionTypingContext context)
	{
		SimpleMethodDescriptor functionDescriptor = context.expressionTypingServices.getDescriptorResolver().resolveFunctionDescriptor(scope.getContainingDeclaration(), scope, function, context.trace);
		scope.addFunctionDescriptor(functionDescriptor);
		JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace);
		context.expressionTypingServices.checkFunctionReturnType(functionInnerScope, function, functionDescriptor, context.dataFlowInfo, null, context.trace);
		return DataFlowUtils.checkStatementType(function, context, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitConstructor(NapileConstructor constructor, ExpressionTypingContext context)
	{
		ConstructorDescriptor functionDescriptor = context.expressionTypingServices.getDescriptorResolver().resolveConstructorDescriptor(scope, (ClassDescriptor) scope.getContainingDeclaration(), constructor, context.trace);
		scope.addConstructorDescriptor(functionDescriptor);
		JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace);
		context.expressionTypingServices.checkFunctionReturnType(functionInnerScope, constructor, functionDescriptor, context.dataFlowInfo, null, context.trace);
		return DataFlowUtils.checkStatementType(constructor, context, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitClass(NapileClass klass, ExpressionTypingContext context)
	{
		TopDownAnalyzer.processClassOrObject(context.expressionTypingServices.getProject(), context.trace, scope, scope.getContainingDeclaration(), klass);
		ClassDescriptor classDescriptor = context.trace.getBindingContext().get(BindingContext.CLASS, klass);
		if(classDescriptor != null)
		{
			scope.addClassifierDescriptor(classDescriptor);
		}
		return DataFlowUtils.checkStatementType(klass, context, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitDeclaration(NapileDeclaration dcl, ExpressionTypingContext context)
	{
		return DataFlowUtils.checkStatementType(dcl, context, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitBinaryExpression(NapileBinaryExpression expression, ExpressionTypingContext context)
	{
		NapileSimpleNameExpression operationSign = expression.getOperationReference();
		IElementType operationType = operationSign.getReferencedNameElementType();
		JetType result;
		if(operationType == JetTokens.EQ)
		{
			result = visitAssignment(expression, context);
		}
		else if(OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType))
		{
			result = visitAssignmentOperation(expression, context);
		}
		else
		{
			return facade.getTypeInfo(expression, context);
		}
		return DataFlowUtils.checkType(result, expression, context, context.dataFlowInfo);
	}

	protected JetType visitAssignmentOperation(NapileBinaryExpression expression, ExpressionTypingContext contextWithExpectedType)
	{
		NapileExpression right = expression.getRight();
		if(right == null)
			return null;

		//There is a temporary binding trace for an opportunity to resolve set method for array if needed (the initial trace should be used there)
		TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace.create(contextWithExpectedType.trace);
		ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceBindingTrace(temporaryBindingTrace);

		NapileSimpleNameExpression operationSign = expression.getOperationReference();
		IElementType operationType = operationSign.getReferencedNameElementType();
		NapileExpression left = NapilePsiUtil.deparenthesize(expression.getLeft());
		if(left == null)
			return null;

		JetType leftType = facade.getTypeInfo(left, context).getType();
		if(leftType == null)
		{
			facade.getTypeInfo(right, context);
			context.trace.report(UNRESOLVED_REFERENCE.on(operationSign));
			temporaryBindingTrace.commit();
			return null;
		}
		ExpressionReceiver receiver = new ExpressionReceiver(left, leftType);

		// We check that defined only one of '+=' and '+' operations, and call it (in the case '+' we then also assign)
		// Check for '+='
		Name name = OperatorConventions.ASSIGNMENT_OPERATIONS.get(operationType);
		TemporaryBindingTrace assignmentOperationTrace = TemporaryBindingTrace.create(context.trace);
		OverloadResolutionResults<MethodDescriptor> assignmentOperationDescriptors = basic.getResolutionResultsForBinaryCall(scope, name, context.replaceBindingTrace(assignmentOperationTrace), expression, receiver);
		JetType assignmentOperationType = OverloadResolutionResultsUtil.getResultType(assignmentOperationDescriptors);

		// Check for '+'
		Name counterpartName = OperatorConventions.BINARY_OPERATION_NAMES.get(OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.get(operationType));
		TemporaryBindingTrace binaryOperationTrace = TemporaryBindingTrace.create(context.trace);
		OverloadResolutionResults<MethodDescriptor> binaryOperationDescriptors = basic.getResolutionResultsForBinaryCall(scope, counterpartName, context.replaceBindingTrace(binaryOperationTrace), expression, receiver);
		JetType binaryOperationType = OverloadResolutionResultsUtil.getResultType(binaryOperationDescriptors);

		JetType type = assignmentOperationType != null ? assignmentOperationType : binaryOperationType;
		if(assignmentOperationType != null && binaryOperationType != null)
		{
			OverloadResolutionResults<MethodDescriptor> ambiguityResolutionResults = OverloadResolutionResultsUtil.ambiguity(assignmentOperationDescriptors, binaryOperationDescriptors);
			context.trace.report(ASSIGN_OPERATOR_AMBIGUITY.on(operationSign, ambiguityResolutionResults.getResultingCalls()));
			Collection<DeclarationDescriptor> descriptors = Sets.newHashSet();
			for(ResolvedCall<? extends MethodDescriptor> call : ambiguityResolutionResults.getResultingCalls())
			{
				descriptors.add(call.getResultingDescriptor());
			}
			facade.getTypeInfo(right, context);
			context.trace.record(AMBIGUOUS_REFERENCE_TARGET, operationSign, descriptors);
		}
		else if(assignmentOperationType != null)
		{
			assignmentOperationTrace.commit();
			if(!TypeUtils.isEqualFqName(assignmentOperationType, NapileLangPackage.NULL))
			{
				context.trace.report(ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT.on(operationSign, assignmentOperationDescriptors.getResultingDescriptor(), operationSign));
			}
		}
		else
		{
			binaryOperationTrace.commit();
			context.trace.record(VARIABLE_REASSIGNMENT, expression);
			if(left instanceof NapileArrayAccessExpression)
			{
				ExpressionTypingContext contextForResolve = context.replaceScope(scope).replaceBindingTrace(TemporaryBindingTrace.create(contextWithExpectedType.trace));
				basic.resolveArrayAccessSetMethod((NapileArrayAccessExpression) left, right, contextForResolve, context.trace);
			}
		}
		basic.checkLValue(context.trace, expression.getLeft());
		temporaryBindingTrace.commit();
		return checkAssignmentType(type, expression, contextWithExpectedType);
	}

	protected JetType visitAssignment(NapileBinaryExpression expression, ExpressionTypingContext contextWithExpectedType)
	{
		ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
		NapileExpression left = NapilePsiUtil.deparenthesize(expression.getLeft());
		NapileExpression right = expression.getRight();
		if(left instanceof NapileArrayAccessExpression)
		{
			NapileArrayAccessExpression arrayAccessExpression = (NapileArrayAccessExpression) left;
			if(right == null)
				return null;
			JetType assignmentType = basic.resolveArrayAccessSetMethod(arrayAccessExpression, right, context.replaceScope(scope), context.trace);
			basic.checkLValue(context.trace, arrayAccessExpression);
			return checkAssignmentType(assignmentType, expression, contextWithExpectedType);
		}
		JetType leftType = facade.getTypeInfo(expression.getLeft(), context.replaceScope(scope)).getType();
		if(right != null)
		{
			JetType rightType = facade.getTypeInfo(right, context.replaceExpectedType(leftType).replaceScope(scope)).getType();
		}
		if(leftType != null)
		{ //if leftType == null, some another error has been generated
			basic.checkLValue(context.trace, expression.getLeft());
		}
		return DataFlowUtils.checkStatementType(expression, contextWithExpectedType);
	}


	@Override
	public JetTypeInfo visitExpression(NapileExpression expression, ExpressionTypingContext context)
	{
		return facade.getTypeInfo(expression, context);
	}

	@Override
	public JetTypeInfo visitJetElement(NapileElement element, ExpressionTypingContext context)
	{
		context.trace.report(UNSUPPORTED.on(element, "in a block"));
		return JetTypeInfo.create(null, context.dataFlowInfo);
	}

	@Override
	public JetTypeInfo visitWhileExpression(NapileWhileExpression expression, ExpressionTypingContext context)
	{
		return controlStructures.visitWhileExpression(expression, context, true);
	}

	@Override
	public JetTypeInfo visitDoWhileExpression(NapileDoWhileExpression expression, ExpressionTypingContext context)
	{
		return controlStructures.visitDoWhileExpression(expression, context, true);
	}

	@Override
	public JetTypeInfo visitForExpression(NapileForExpression expression, ExpressionTypingContext context)
	{
		return controlStructures.visitForExpression(expression, context, true);
	}

	@Override
	public JetTypeInfo visitIfExpression(NapileIfExpression expression, ExpressionTypingContext context)
	{
		return controlStructures.visitIfExpression(expression, context, true);
	}

	@Override
	public JetTypeInfo visitWhenExpression(final NapileWhenExpression expression, ExpressionTypingContext context)
	{
		return patterns.visitWhenExpression(expression, context, true);
	}

	@Override
	public JetTypeInfo visitBlockExpression(NapileBlockExpression expression, ExpressionTypingContext context)
	{
		return basic.visitBlockExpression(expression, context, true);
	}

	@Override
	public JetTypeInfo visitParenthesizedExpression(NapileParenthesizedExpression expression, ExpressionTypingContext context)
	{
		return basic.visitParenthesizedExpression(expression, context, true);
	}

	@Override
	public JetTypeInfo visitUnaryExpression(NapileUnaryExpression expression, ExpressionTypingContext context)
	{
		return basic.visitUnaryExpression(expression, context, true);
	}
}
