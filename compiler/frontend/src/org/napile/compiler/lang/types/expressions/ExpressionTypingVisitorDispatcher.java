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

import static org.napile.compiler.lang.diagnostics.Errors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.DeferredType;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetTypeInfo;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.util.lazy.ReenteringLazyValueComputationException;
import com.intellij.lang.ASTNode;

/**
 * @author abreslav
 */
public class ExpressionTypingVisitorDispatcher extends NapileVisitor<JetTypeInfo, ExpressionTypingContext> implements ExpressionTypingInternals
{

	@Override
	public JetTypeInfo visitIdeTemplate(NapileIdeTemplate expression, ExpressionTypingContext data)
	{
		return basic.visitIdeTemplate(expression, data);
	}

	@NotNull
	public static ExpressionTypingFacade create()
	{
		return new ExpressionTypingVisitorDispatcher(null);
	}

	@NotNull
	public static ExpressionTypingInternals createForBlock(final WritableScope writableScope)
	{
		return new ExpressionTypingVisitorDispatcher(writableScope);
	}

	private final BasicExpressionTypingVisitor basic;
	private final ExpressionTypingVisitorForStatements statements;
	private final ClosureExpressionsTypingVisitor closures = new ClosureExpressionsTypingVisitor(this);
	private final ControlStructureTypingVisitor controlStructures = new ControlStructureTypingVisitor(this);
	private final PatternMatchingTypingVisitor patterns = new PatternMatchingTypingVisitor(this);

	private ExpressionTypingVisitorDispatcher(WritableScope writableScope)
	{
		this.basic = new BasicExpressionTypingVisitor(this);
		if(writableScope != null)
		{
			this.statements = new ExpressionTypingVisitorForStatements(this, writableScope, basic, controlStructures, patterns);
		}
		else
		{
			this.statements = null;
		}
	}

	@Override
	public JetTypeInfo getSelectorReturnTypeInfo(@NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull NapileExpression selectorExpression, @NotNull ExpressionTypingContext context)
	{
		return basic.getSelectorReturnTypeInfo(receiver, callOperationNode, selectorExpression, context);
	}

	@Override
	public boolean checkInExpression(NapileElement callElement, @NotNull NapileSimpleNameExpression operationSign, @Nullable NapileExpression left, @NotNull NapileExpression right, ExpressionTypingContext context)
	{
		return basic.checkInExpression(callElement, operationSign, left, right, context);
	}

	@Override
	@NotNull
	public final JetTypeInfo safeGetTypeInfo(@NotNull NapileExpression expression, ExpressionTypingContext context)
	{
		JetTypeInfo typeInfo = getTypeInfo(expression, context);
		if(typeInfo.getType() != null)
		{
			return typeInfo;
		}
		return JetTypeInfo.create(ErrorUtils.createErrorType("Type for " + expression.getText()), context.dataFlowInfo);
	}

	@Override
	@NotNull
	public final JetTypeInfo getTypeInfo(@NotNull NapileExpression expression, ExpressionTypingContext context)
	{
		return getTypeInfo(expression, context, this);
	}

	@NotNull
	public final JetTypeInfo getTypeInfo(@NotNull NapileExpression expression, ExpressionTypingContext context, boolean isStatement)
	{
		if(!isStatement)
			return getTypeInfo(expression, context);
		if(statements != null)
		{
			return getTypeInfo(expression, context, statements);
		}
		return getTypeInfo(expression, context, createStatementVisitor(context));
	}

	private ExpressionTypingVisitorForStatements createStatementVisitor(ExpressionTypingContext context)
	{
		return new ExpressionTypingVisitorForStatements(this, ExpressionTypingUtils.newWritableScopeImpl(context, "statement scope"), basic, controlStructures, patterns);
	}

	@Override
	public void checkStatementType(@NotNull NapileExpression expression, ExpressionTypingContext context)
	{
		expression.accept(createStatementVisitor(context), context);
	}

	@NotNull
	private JetTypeInfo getTypeInfo(@NotNull NapileExpression expression, ExpressionTypingContext context, NapileVisitor<JetTypeInfo, ExpressionTypingContext> visitor)
	{
		if(context.trace.get(BindingContext.PROCESSED, expression))
		{
			DataFlowInfo dataFlowInfo = context.trace.get(BindingContext.EXPRESSION_DATA_FLOW_INFO, expression);
			if(dataFlowInfo == null)
			{
				dataFlowInfo = context.dataFlowInfo;
			}
			return JetTypeInfo.create(context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression), dataFlowInfo);
		}
		JetTypeInfo result;
		try
		{
			result = expression.accept(visitor, context);
			// Some recursive definitions (object expressions) must put their types in the cache manually:
			if(context.trace.get(BindingContext.PROCESSED, expression))
			{
				return JetTypeInfo.create(context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression), result.getDataFlowInfo());
			}

			if(result.getType() instanceof DeferredType)
			{
				result = JetTypeInfo.create(((DeferredType) result.getType()).getActualType(), result.getDataFlowInfo());
			}
			if(result.getType() != null)
			{
				context.trace.record(BindingContext.EXPRESSION_TYPE, expression, result.getType());
			}
		}
		catch(ReenteringLazyValueComputationException e)
		{
			context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.on(expression));
			result = JetTypeInfo.create(null, context.dataFlowInfo);
		}

		if(!context.trace.get(BindingContext.PROCESSED, expression) && !(expression instanceof NapileReferenceExpression))
		{
			context.trace.record(BindingContext.RESOLUTION_SCOPE, expression, context.scope);
		}
		context.trace.record(BindingContext.PROCESSED, expression);
		if(result.getDataFlowInfo() != context.dataFlowInfo)
		{
			context.trace.record(BindingContext.EXPRESSION_DATA_FLOW_INFO, expression, result.getDataFlowInfo());
		}
		return result;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public JetTypeInfo visitFunctionLiteralExpression(NapileFunctionLiteralExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(closures, data);
	}

	@Override
	public JetTypeInfo visitObjectLiteralExpression(NapileObjectLiteralExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(closures, data);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public JetTypeInfo visitThrowExpression(NapileThrowExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public JetTypeInfo visitReturnExpression(NapileReturnExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public JetTypeInfo visitContinueExpression(NapileContinueExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public JetTypeInfo visitIfExpression(NapileIfExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public JetTypeInfo visitTryExpression(NapileTryExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public JetTypeInfo visitForExpression(NapileForExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public JetTypeInfo visitWhileExpression(NapileWhileExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public JetTypeInfo visitDoWhileExpression(NapileDoWhileExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public JetTypeInfo visitBreakExpression(NapileBreakExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public JetTypeInfo visitIsExpression(NapileIsExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(patterns, data);
	}

	@Override
	public JetTypeInfo visitWhenExpression(NapileWhenExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(patterns, data);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public JetTypeInfo visitJetElement(NapileElement element, ExpressionTypingContext data)
	{
		return element.accept(basic, data);
	}
}
