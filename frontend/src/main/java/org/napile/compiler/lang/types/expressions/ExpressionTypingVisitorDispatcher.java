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
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.DeferredType;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.NapileTypeInfo;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.util.lazy.ReenteringLazyValueComputationException;
import com.intellij.lang.ASTNode;

/**
 * @author abreslav
 */
public class ExpressionTypingVisitorDispatcher extends NapileVisitor<NapileTypeInfo, ExpressionTypingContext> implements ExpressionTypingInternals
{

	@Override
	public NapileTypeInfo visitIdeTemplate(NapileIdeTemplate expression, ExpressionTypingContext data)
	{
		return basic.visitIdeTemplate(expression, data);
	}

	@NotNull
	public static ExpressionTypingFacade create(ExpressionTypingServices services)
	{
		return new ExpressionTypingVisitorDispatcher(null, services);
	}

	@NotNull
	public static ExpressionTypingInternals createForBlock(final WritableScope writableScope, final ExpressionTypingServices services)
	{
		return new ExpressionTypingVisitorDispatcher(writableScope, services);
	}

	private final BasicExpressionTypingVisitor basic;
	private final ExpressionTypingVisitorForStatements statements;
	private final ClosureExpressionsTypingVisitor closures;
	private final ControlStructureTypingVisitor controlStructures = new ControlStructureTypingVisitor(this);
	private final PatternMatchingTypingVisitor patterns = new PatternMatchingTypingVisitor(this);

	private ExpressionTypingVisitorDispatcher(WritableScope writableScope, @NotNull ExpressionTypingServices services)
	{
		this.basic = new BasicExpressionTypingVisitor(this);
		this.closures = new ClosureExpressionsTypingVisitor(this, services);

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
	public NapileTypeInfo getSelectorReturnTypeInfo(@NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull NapileExpression selectorExpression, @NotNull ExpressionTypingContext context)
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
	public final NapileTypeInfo safeGetTypeInfo(@NotNull NapileExpression expression, ExpressionTypingContext context)
	{
		NapileTypeInfo typeInfo = getTypeInfo(expression, context);
		if(typeInfo.getType() != null)
		{
			return typeInfo;
		}
		return NapileTypeInfo.create(ErrorUtils.createErrorType("Type for " + expression.getText()), context.dataFlowInfo);
	}

	@Override
	@NotNull
	public final NapileTypeInfo getTypeInfo(@NotNull NapileExpression expression, ExpressionTypingContext context)
	{
		return getTypeInfo(expression, context, this);
	}

	@NotNull
	@Override
	public final NapileTypeInfo getTypeInfo(@NotNull NapileExpression expression, ExpressionTypingContext context, boolean isStatement)
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
	private NapileTypeInfo getTypeInfo(@NotNull NapileExpression expression, ExpressionTypingContext context, NapileVisitor<NapileTypeInfo, ExpressionTypingContext> visitor)
	{
		if(context.trace.safeGet(BindingTraceKeys.PROCESSED, expression))
		{
			DataFlowInfo dataFlowInfo = context.trace.get(BindingTraceKeys.EXPRESSION_DATA_FLOW_INFO, expression);
			if(dataFlowInfo == null)
			{
				dataFlowInfo = context.dataFlowInfo;
			}
			return NapileTypeInfo.create(context.trace.get(BindingTraceKeys.EXPRESSION_TYPE, expression), dataFlowInfo);
		}
		NapileTypeInfo result;
		try
		{
			result = expression.accept(visitor, context);
			// Some recursive definitions (object expressions) must put their types in the cache manually:
			if(context.trace.safeGet(BindingTraceKeys.PROCESSED, expression))
			{
				return NapileTypeInfo.create(context.trace.get(BindingTraceKeys.EXPRESSION_TYPE, expression), result.getDataFlowInfo());
			}

			if(result.getType() instanceof DeferredType)
			{
				result = NapileTypeInfo.create(((DeferredType) result.getType()).getActualType(), result.getDataFlowInfo());
			}
			if(result.getType() != null)
			{
				context.trace.record(BindingTraceKeys.EXPRESSION_TYPE, expression, result.getType());
			}
		}
		catch(ReenteringLazyValueComputationException e)
		{
			context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.on(expression));
			result = NapileTypeInfo.create(null, context.dataFlowInfo);
		}

		if(!context.trace.get(BindingTraceKeys.PROCESSED, expression) && !(expression instanceof NapileReferenceExpression))
		{
			context.trace.record(BindingTraceKeys.RESOLUTION_SCOPE, expression, context.scope);
		}
		context.trace.record(BindingTraceKeys.PROCESSED, expression);
		if(result.getDataFlowInfo() != context.dataFlowInfo)
		{
			context.trace.record(BindingTraceKeys.EXPRESSION_DATA_FLOW_INFO, expression, result.getDataFlowInfo());
		}
		return result;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public NapileTypeInfo visitAnonymMethodExpression(NapileAnonymMethodExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(closures, data);
	}

	@Override
	public NapileTypeInfo visitAnonymClassExpression(NapileAnonymClassExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(closures, data);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public NapileTypeInfo visitThrowExpression(NapileThrowExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public NapileTypeInfo visitReturnExpression(NapileReturnExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public NapileTypeInfo visitContinueExpression(NapileContinueExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public NapileTypeInfo visitIfExpression(NapileIfExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public NapileTypeInfo visitTryExpression(NapileTryExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public NapileTypeInfo visitForExpression(NapileForExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public NapileTypeInfo visitWhileExpression(NapileWhileExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public NapileTypeInfo visitDoWhileExpression(NapileDoWhileExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	@Override
	public NapileTypeInfo visitBreakExpression(NapileBreakExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(controlStructures, data);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public NapileTypeInfo visitIsExpression(NapileIsExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(patterns, data);
	}

	@Override
	public NapileTypeInfo visitWhenExpression(NapileWhenExpression expression, ExpressionTypingContext data)
	{
		return expression.accept(patterns, data);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public NapileTypeInfo visitJetElement(NapileElement element, ExpressionTypingContext data)
	{
		return element.accept(basic, data);
	}
}
