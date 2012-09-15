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

package org.napile.compiler.lang.cfg;

import static org.napile.compiler.lang.diagnostics.Errors.BREAK_OR_CONTINUE_OUTSIDE_A_LOOP;
import static org.napile.compiler.lang.diagnostics.Errors.ELSE_MISPLACED_IN_WHEN;
import static org.napile.compiler.lang.diagnostics.Errors.NOT_A_LOOP_LABEL;
import static org.napile.compiler.lang.diagnostics.Errors.NO_ELSE_IN_WHEN;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.NapileNodeTypes;
import org.napile.compiler.lang.cfg.pseudocode.JetControlFlowInstructionsGenerator;
import org.napile.compiler.lang.cfg.pseudocode.LocalDeclarationInstruction;
import org.napile.compiler.lang.cfg.pseudocode.Pseudocode;
import org.napile.compiler.lang.cfg.pseudocode.PseudocodeImpl;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.constants.BoolValue;
import org.napile.compiler.lang.resolve.constants.CompileTimeConstantResolver;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.expressions.OperatorConventions;
import org.napile.compiler.lexer.JetTokens;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

/**
 * @author abreslav
 * @author svtk
 */
public class JetControlFlowProcessor
{

	private final JetControlFlowBuilder builder;
	private final BindingTrace trace;

	public JetControlFlowProcessor(BindingTrace trace)
	{
		this.builder = new JetControlFlowInstructionsGenerator();
		this.trace = trace;
	}

	public Pseudocode generatePseudocode(@NotNull NapileDeclaration subroutine)
	{
		Pseudocode pseudocode = generate(subroutine);
		((PseudocodeImpl) pseudocode).postProcess();
		for(LocalDeclarationInstruction localDeclarationInstruction : pseudocode.getLocalDeclarations())
		{
			((PseudocodeImpl) localDeclarationInstruction.getBody()).postProcess();
		}
		return pseudocode;
	}

	private Pseudocode generate(@NotNull NapileDeclaration subroutine)
	{
		builder.enterSubroutine(subroutine);
		if(subroutine instanceof NapileDeclarationWithBody)
		{
			NapileDeclarationWithBody declarationWithBody = (NapileDeclarationWithBody) subroutine;
			CFPVisitor cfpVisitor = new CFPVisitor(false);
			List<NapileElement> valueParameters = declarationWithBody.getValueParameters();
			for(NapileElement valueParameter : valueParameters)
			{
				valueParameter.accept(cfpVisitor);
			}
			NapileExpression bodyExpression = declarationWithBody.getBodyExpression();
			if(bodyExpression != null)
			{
				bodyExpression.accept(cfpVisitor);
			}
		}
		else
		{
			subroutine.accept(new CFPVisitor(false));
		}
		return builder.exitSubroutine(subroutine);
	}

	private void processLocalDeclaration(@NotNull NapileDeclaration subroutine)
	{
		Label afterDeclaration = builder.createUnboundLabel();
		builder.nondeterministicJump(afterDeclaration);
		generate(subroutine);
		builder.bindLabel(afterDeclaration);
	}


	private class CFPVisitor extends NapileVisitorVoid
	{
		private final boolean inCondition;
		private final NapileVisitorVoid conditionVisitor = new NapileVisitorVoid()
		{

			@Override
			public void visitWhenConditionInRange(NapileWhenConditionInRange condition)
			{
				value(condition.getRangeExpression(), CFPVisitor.this.inCondition); // TODO : inCondition?
				value(condition.getOperationReference(), CFPVisitor.this.inCondition); // TODO : inCondition?
				// TODO : read the call to contains()...
			}

			@Override
			public void visitWhenConditionIsPattern(NapileWhenConditionIsPattern condition)
			{

			}

			@Override
			public void visitWhenConditionWithExpression(NapileWhenConditionWithExpression condition)
			{
				value(condition.getExpression(), inCondition);
			}

			@Override
			public void visitJetElement(NapileElement element)
			{
				throw new UnsupportedOperationException("[JetControlFlowProcessor] " + element.toString());
			}
		};

		private CFPVisitor(boolean inCondition)
		{
			this.inCondition = inCondition;
		}

		private void value(@Nullable NapileElement element, boolean inCondition)
		{
			if(element == null)
				return;
			CFPVisitor visitor;
			if(this.inCondition == inCondition)
			{
				visitor = this;
			}
			else
			{
				visitor = new CFPVisitor(inCondition);
			}
			element.accept(visitor);
		}

		@Override
		public void visitParenthesizedExpression(NapileParenthesizedExpression expression)
		{
			builder.read(expression);

			NapileExpression innerExpression = expression.getExpression();
			if(innerExpression != null)
			{
				value(innerExpression, inCondition);
			}
		}

		@Override
		public void visitThisExpression(NapileThisExpression expression)
		{
			builder.read(expression);
		}

		@Override
		public void visitConstantExpression(NapileConstantExpression expression)
		{
			builder.read(expression);
		}

		@Override
		public void visitReferenceParameter(NapileReferenceParameter parameter)
		{
			NapileSimpleNameExpression reference = parameter.getReferenceExpression();
			if(reference == null)
				return;

			builder.write(parameter, parameter);
		}

		@Override
		public void visitSimpleNameExpression(NapileSimpleNameExpression expression)
		{
			builder.read(expression);
			if(trace.safeGet(BindingContext.PROCESSED, expression))
			{
				JetType type = trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
				if(type != null && false)
				{
					builder.jumpToError(expression);
				}
			}
		}

		@Override
		public void visitLabelExpression(NapileLabelExpression expression)
		{
			NapileExpression body = expression.getExpression();
			if(body == null)
				return;

			builder.enterLoop(expression, null, null);

			value(body, inCondition);

			builder.exitLoop(expression);
		}

		@SuppressWarnings("SuspiciousMethodCalls")
		@Override
		public void visitBinaryExpression(NapileBinaryExpression expression)
		{
			IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
			NapileExpression right = expression.getRight();
			if(operationType == JetTokens.ANDAND)
			{
				value(expression.getLeft(), true);
				Label resultLabel = builder.createUnboundLabel();
				builder.jumpOnFalse(resultLabel);
				if(right != null)
				{
					value(right, true);
				}
				builder.bindLabel(resultLabel);
				if(!inCondition)
				{
					builder.read(expression);
				}
			}
			else if(operationType == JetTokens.OROR)
			{
				value(expression.getLeft(), true);
				Label resultLabel = builder.createUnboundLabel();
				builder.jumpOnTrue(resultLabel);
				if(right != null)
				{
					value(right, true);
				}
				builder.bindLabel(resultLabel);
				if(!inCondition)
				{
					builder.read(expression);
				}
			}
			else if(operationType == JetTokens.EQ)
			{
				NapileExpression left = NapilePsiUtil.deparenthesize(expression.getLeft());
				if(right != null)
				{
					value(right, false);
				}
				if(left instanceof NapileSimpleNameExpression)
				{
					builder.write(expression, left);
				}
				else if(left instanceof NapileArrayAccessExpression)
				{
					NapileArrayAccessExpression arrayAccessExpression = (NapileArrayAccessExpression) left;
					visitAssignToArrayAccess(expression, arrayAccessExpression);
				}
				else if(left instanceof NapileQualifiedExpression)
				{
					assert !(left instanceof NapileHashQualifiedExpression) : left; // TODO
					NapileQualifiedExpression qualifiedExpression = (NapileQualifiedExpression) left;
					value(qualifiedExpression.getReceiverExpression(), false);
					value(expression.getOperationReference(), false);
					builder.write(expression, left);
				}
				else
				{
					builder.unsupported(expression); // TODO
				}
			}
			else if(OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType))
			{
				NapileExpression left = NapilePsiUtil.deparenthesize(expression.getLeft());
				if(left != null)
				{
					value(left, false);
				}
				if(right != null)
				{
					value(right, false);
				}
				if(left instanceof NapileSimpleNameExpression || left instanceof NapileArrayAccessExpression)
				{
					value(expression.getOperationReference(), false);
					builder.write(expression, left);
				}
				else if(left != null)
				{
					builder.unsupported(expression); // TODO
				}
			}
			else if(operationType == JetTokens.ELVIS)
			{
				builder.read(expression);
				value(expression.getLeft(), false);
				value(expression.getOperationReference(), false);
				Label afterElvis = builder.createUnboundLabel();
				builder.jumpOnTrue(afterElvis);
				if(right != null)
				{
					value(right, false);
				}
				builder.bindLabel(afterElvis);
			}
			else
			{
				value(expression.getLeft(), false);
				if(right != null)
				{
					value(right, false);
				}
				value(expression.getOperationReference(), false);
				builder.read(expression);
			}
		}

		private void visitAssignToArrayAccess(NapileBinaryExpression expression, NapileArrayAccessExpression arrayAccessExpression)
		{
			for(NapileExpression index : arrayAccessExpression.getIndexExpressions())
			{
				value(index, false);
			}
			value(arrayAccessExpression.getArrayExpression(), false);
			value(expression.getOperationReference(), false);
			builder.write(expression, arrayAccessExpression); // TODO : ???
		}

		@Override
		public void visitUnaryExpression(NapileUnaryExpression expression)
		{
			NapileSimpleNameExpression operationSign = expression.getOperationReference();
			IElementType operationType = operationSign.getReferencedNameElementType();
			NapileExpression baseExpression = expression.getBaseExpression();
			if(baseExpression == null)
				return;

			value(baseExpression, false);
			value(operationSign, false);

			boolean incrementOrDecrement = isIncrementOrDecrement(operationType);
			if(incrementOrDecrement)
			{
				builder.write(expression, baseExpression);
			}

			builder.read(expression);
		}

		private boolean isIncrementOrDecrement(IElementType operationType)
		{
			return operationType == JetTokens.PLUSPLUS || operationType == JetTokens.MINUSMINUS;
		}


		@Override
		public void visitIfExpression(NapileIfExpression expression)
		{
			NapileExpression condition = expression.getCondition();
			if(condition != null)
			{
				value(condition, true);
			}
			Label elseLabel = builder.createUnboundLabel();
			builder.jumpOnFalse(elseLabel);
			NapileExpression thenBranch = expression.getThen();
			if(thenBranch != null)
			{
				value(thenBranch, inCondition);
			}
			else
			{
				builder.readUnit(expression);
			}
			Label resultLabel = builder.createUnboundLabel();
			builder.jump(resultLabel);
			builder.bindLabel(elseLabel);
			NapileExpression elseBranch = expression.getElse();
			if(elseBranch != null)
			{
				value(elseBranch, inCondition);
			}
			else
			{
				builder.readUnit(expression);
			}
			builder.bindLabel(resultLabel);
		}

		@Override
		public void visitTryExpression(NapileTryExpression expression)
		{
			builder.read(expression);
			final NapileFinallySection finallyBlock = expression.getFinallyBlock();
			if(finallyBlock != null)
			{
				builder.enterTryFinally(new GenerationTrigger()
				{
					private boolean working = false;

					@Override
					public void generate()
					{
						// This checks are needed for the case of having e.g. return inside finally: 'try {return} finally{return}'
						if(working)
							return;
						working = true;
						value(finallyBlock.getFinalExpression(), inCondition);
						working = false;
					}
				});
			}

			List<NapileCatchClause> catchClauses = expression.getCatchClauses();
			final boolean hasCatches = !catchClauses.isEmpty();
			Label onException = null;
			if(hasCatches)
			{
				onException = builder.createUnboundLabel();
				builder.nondeterministicJump(onException);
			}
			value(expression.getTryBlock(), inCondition);

			if(hasCatches)
			{
				builder.allowDead();
				Label afterCatches = builder.createUnboundLabel();
				builder.jump(afterCatches);

				builder.bindLabel(onException);
				LinkedList<Label> catchLabels = Lists.newLinkedList();
				int catchClausesSize = catchClauses.size();
				for(int i = 0; i < catchClausesSize - 1; i++)
				{
					catchLabels.add(builder.createUnboundLabel());
				}
				builder.nondeterministicJump(catchLabels);
				boolean isFirst = true;
				for(NapileCatchClause catchClause : catchClauses)
				{
					if(!isFirst)
					{
						builder.bindLabel(catchLabels.remove());
					}
					else
					{
						isFirst = false;
					}
					NapileElement catchParameter = catchClause.getCatchParameter();
					if(catchParameter instanceof NapilePropertyParameter)
					{
						builder.declare((NapilePropertyParameter) catchParameter);
						builder.write(catchParameter, catchParameter);
					}
					NapileExpression catchBody = catchClause.getCatchBody();
					if(catchBody != null)
					{
						value(catchBody, false);
					}
					builder.allowDead();
					builder.jump(afterCatches);
				}

				builder.bindLabel(afterCatches);
			}
			else
			{
				builder.allowDead();
			}

			if(finallyBlock != null)
			{
				builder.exitTryFinally();
				value(finallyBlock.getFinalExpression(), inCondition);
			}
			builder.stopAllowDead();
		}

		@Override
		public void visitWhileExpression(NapileWhileExpression expression)
		{
			builder.read(expression);
			LoopInfo loopInfo = builder.enterLoop(expression, null, null);

			builder.bindLabel(loopInfo.getConditionEntryPoint());
			NapileExpression condition = expression.getCondition();
			if(condition != null)
			{
				value(condition, true);
			}
			boolean conditionIsTrueConstant = false;
			if(condition instanceof NapileConstantExpression && condition.getNode().getElementType() == NapileNodeTypes.BOOLEAN_CONSTANT)
			{
				if(BoolValue.TRUE == new CompileTimeConstantResolver().getBooleanValue(condition.getText()))
				{
					conditionIsTrueConstant = true;
				}
			}
			if(!conditionIsTrueConstant)
			{
				builder.jumpOnFalse(loopInfo.getExitPoint());
			}

			builder.bindLabel(loopInfo.getBodyEntryPoint());
			NapileExpression body = expression.getBody();
			if(body != null)
			{
				value(body, false);
			}
			builder.jump(loopInfo.getEntryPoint());
			builder.exitLoop(expression);
			builder.readUnit(expression);
		}

		@Override
		public void visitDoWhileExpression(NapileDoWhileExpression expression)
		{
			builder.read(expression);
			LoopInfo loopInfo = builder.enterLoop(expression, null, null);

			builder.bindLabel(loopInfo.getBodyEntryPoint());
			NapileExpression body = expression.getBody();
			if(body != null)
			{
				value(body, false);
			}
			builder.bindLabel(loopInfo.getConditionEntryPoint());
			NapileExpression condition = expression.getCondition();
			if(condition != null)
			{
				value(condition, true);
			}
			builder.jumpOnTrue(loopInfo.getEntryPoint());
			builder.exitLoop(expression);
			builder.readUnit(expression);
		}

		@Override
		public void visitForExpression(NapileForExpression expression)
		{
			builder.read(expression);
			NapileExpression loopRange = expression.getLoopRange();
			if(loopRange != null)
			{
				value(loopRange, false);
			}
			NapilePropertyParameter loopParameter = expression.getLoopParameter();
			if(loopParameter != null)
			{
				builder.declare(loopParameter);
				builder.write(loopParameter, loopParameter);
			}
			// TODO : primitive cases
			Label loopExitPoint = builder.createUnboundLabel();
			Label conditionEntryPoint = builder.createUnboundLabel();

			builder.bindLabel(conditionEntryPoint);
			builder.nondeterministicJump(loopExitPoint);

			LoopInfo loopInfo = builder.enterLoop(expression, loopExitPoint, conditionEntryPoint);

			builder.bindLabel(loopInfo.getBodyEntryPoint());
			NapileExpression body = expression.getBody();
			if(body != null)
			{
				value(body, false);
			}

			builder.nondeterministicJump(loopInfo.getEntryPoint());
			builder.exitLoop(expression);
			builder.readUnit(expression);
		}

		@Override
		public void visitBreakExpression(NapileBreakExpression expression)
		{
			String labelName = expression.getLabelName();
			NapileElement loop;
			if(labelName != null)
			{
				NapileSimpleNameExpression targetLabel = expression.getTargetLabel();
				assert targetLabel != null;
				PsiElement labeledElement = BindingContextUtils.resolveToDeclarationPsiElement(trace.getBindingContext(), targetLabel);
				if(labeledElement != null)
				{
					loop = (NapileElement) labeledElement;
				}
				else
				{
					trace.report(NOT_A_LOOP_LABEL.on(expression, targetLabel.getText()));
					loop = null;
				}
			}
			else
			{
				loop = builder.getCurrentLoop();
				if(loop == null)
				{
					trace.report(BREAK_OR_CONTINUE_OUTSIDE_A_LOOP.on(expression));
				}
			}

			if(loop != null)
				builder.jump(builder.getExitPoint(loop));
		}

		@Override
		public void visitContinueExpression(NapileContinueExpression expression)
		{
			NapileElement loop = builder.getCurrentLoop();
			if(loop == null)
			{
				trace.report(BREAK_OR_CONTINUE_OUTSIDE_A_LOOP.on(expression));
			}
			else
				builder.jump(builder.getEntryPoint(loop));
		}

		@Override
		public void visitReturnExpression(NapileReturnExpression expression)
		{
			NapileExpression returnedExpression = expression.getReturnedExpression();
			if(returnedExpression != null)
			{
				value(returnedExpression, false);
			}
			NapileElement subroutine = builder.getReturnSubroutine();

			//todo cache NapileFunctionLiteral instead
			if(subroutine instanceof NapileFunctionLiteralExpression)
			{
				subroutine = ((NapileFunctionLiteralExpression) subroutine).getFunctionLiteral();
			}
			if(subroutine instanceof NapileMethod || subroutine instanceof NapilePropertyAccessor)
			{
				if(returnedExpression == null)
				{
					builder.returnNoValue(expression, subroutine);
				}
				else
				{
					builder.returnValue(expression, subroutine);
				}
			}
		}

		@Override
		public void visitPropertyParameter(NapilePropertyParameter parameter)
		{
			NapileExpression defaultValue = parameter.getDefaultValue();
			builder.declare(parameter);
			if(defaultValue != null)
			{
				value(defaultValue, inCondition);
			}
			builder.write(parameter, parameter);
		}

		@Override
		public void visitBlockExpression(NapileBlockExpression expression)
		{
			List<NapileElement> statements = expression.getStatements();
			for(NapileElement statement : statements)
			{
				value(statement, false);
			}
			if(statements.isEmpty())
			{
				builder.readUnit(expression);
			}
		}

		@Override
		public void visitConstructor(NapileConstructor constructor)
		{
			processLocalDeclaration(constructor);
		}

		@Override
		public void visitStaticConstructor(NapileStaticConstructor staticConstructor)
		{
			processLocalDeclaration(staticConstructor);
		}

		@Override
		public void visitNamedMethod(NapileNamedFunction function)
		{
			processLocalDeclaration(function);
		}

		@Override
		public void visitFunctionLiteralExpression(NapileFunctionLiteralExpression expression)
		{
			NapileFunctionLiteral functionLiteral = expression.getFunctionLiteral();
			processLocalDeclaration(functionLiteral);
			builder.read(expression);
		}

		@Override
		public void visitQualifiedExpression(NapileQualifiedExpression expression)
		{
			value(expression.getReceiverExpression(), false);
			NapileExpression selectorExpression = expression.getSelectorExpression();
			if(selectorExpression != null)
			{
				value(selectorExpression, false);
			}
			builder.read(expression);
			if(trace.safeGet(BindingContext.PROCESSED, expression))
			{
				JetType type = trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
				if(type != null && false)
				{
					builder.jumpToError(expression);
				}
			}
		}

		private void visitCall(NapileCallElement call)
		{
			for(ValueArgument argument : call.getValueArguments())
			{
				NapileExpression argumentExpression = argument.getArgumentExpression();
				if(argumentExpression != null)
				{
					value(argumentExpression, false);
				}
			}

			for(NapileExpression functionLiteral : call.getFunctionLiteralArguments())
			{
				value(functionLiteral, false);
			}
		}

		@Override
		public void visitCallExpression(NapileCallExpression expression)
		{
			//inline functions after M1
			//            ResolvedCall<? extends CallableDescriptor> resolvedCall = trace.get(BindingContext.RESOLVED_CALL, expression.getCalleeExpression());
			//            assert resolvedCall != null;
			//            CallableDescriptor resultingDescriptor = resolvedCall.getResultingDescriptor();
			//            PsiElement element = trace.get(BindingContext.DESCRIPTOR_TO_DECLARATION, resultingDescriptor);
			//            if (element instanceof NapileNamedFunction) {
			//                NapileNamedFunction namedFunction = (NapileNamedFunction) element;
			//                if (namedFunction.hasModifier(JetTokens.INLINE_KEYWORD)) {
			//                }
			//            }

			for(NapileTypeReference typeArgument : expression.getTypeArguments())
			{
				value(typeArgument, false);
			}

			visitCall(expression);

			value(expression.getCalleeExpression(), false);
			builder.read(expression);
			if(trace.safeGet(BindingContext.PROCESSED, expression))
			{
				JetType type = trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
				if(type != null && false)
				{
					builder.jumpToError(expression);
				}
			}
		}

		//        @Override
		//        public void visitNewExpression(JetNewExpression expression) {
		//            // TODO : Instantiated class is loaded
		//            // TODO : type arguments?
		//            visitCall(expression);
		//            builder.read(expression);
		//        }

		@Override
		public void visitProperty(NapileProperty property)
		{
			builder.declare(property);
			NapileExpression initializer = property.getInitializer();
			if(initializer != null)
			{
				value(initializer, false);
				builder.write(property, property);
			}
			for(NapilePropertyAccessor accessor : property.getAccessors())
			{
				value(accessor, false);
			}
		}

		@Override
		public void visitPropertyAccessor(NapilePropertyAccessor accessor)
		{
			processLocalDeclaration(accessor);
		}

		@Override
		public void visitBinaryWithTypeRHSExpression(NapileBinaryExpressionWithTypeRHS expression)
		{
			IElementType operationType = expression.getOperationSign().getReferencedNameElementType();
			if(operationType == JetTokens.COLON || operationType == JetTokens.AS_KEYWORD || operationType == JetTokens.AS_SAFE)
			{
				value(expression.getLeft(), false);
				builder.read(expression);
			}
			else
			{
				visitJetElement(expression);
			}
		}

		@Override
		public void visitThrowExpression(NapileThrowExpression expression)
		{
			NapileExpression thrownExpression = expression.getThrownExpression();
			if(thrownExpression != null)
			{
				value(thrownExpression, false);
			}
			builder.jumpToError(expression);
		}

		@Override
		public void visitArrayAccessExpression(NapileArrayAccessExpression expression)
		{
			for(NapileExpression index : expression.getIndexExpressions())
			{
				value(index, false);
			}
			value(expression.getArrayExpression(), false);
			// TODO : read 'get' or 'set' function
			builder.read(expression);
		}

		@Override
		public void visitIsExpression(final NapileIsExpression expression)
		{
			value(expression.getLeftHandSide(), inCondition);

			builder.read(expression);
		}

		@Override
		public void visitWhenExpression(NapileWhenExpression expression)
		{
			NapileExpression subjectExpression = expression.getSubjectExpression();
			if(subjectExpression != null)
			{
				value(subjectExpression, inCondition);
			}
			boolean hasElseOrIrrefutableBranch = false;

			Label doneLabel = builder.createUnboundLabel();

			Label nextLabel = null;
			for(Iterator<NapileWhenEntry> iterator = expression.getEntries().iterator(); iterator.hasNext(); )
			{
				NapileWhenEntry whenEntry = iterator.next();

				builder.read(whenEntry);

				if(whenEntry.isElse())
				{
					hasElseOrIrrefutableBranch = true;
					if(iterator.hasNext())
					{
						trace.report(ELSE_MISPLACED_IN_WHEN.on(whenEntry));
					}
				}
				boolean isIrrefutable = whenEntry.isElse();
				if(isIrrefutable)
				{
					hasElseOrIrrefutableBranch = true;
				}

				Label bodyLabel = builder.createUnboundLabel();

				NapileWhenCondition[] conditions = whenEntry.getConditions();
				for(int i = 0; i < conditions.length; i++)
				{
					NapileWhenCondition condition = conditions[i];
					condition.accept(conditionVisitor);
					if(i + 1 < conditions.length)
					{
						builder.nondeterministicJump(bodyLabel);
					}
				}

				if(!isIrrefutable)
				{
					nextLabel = builder.createUnboundLabel();
					builder.nondeterministicJump(nextLabel);
				}

				builder.bindLabel(bodyLabel);
				value(whenEntry.getExpression(), inCondition);
				builder.allowDead();
				builder.jump(doneLabel);

				if(!isIrrefutable)
				{
					builder.bindLabel(nextLabel);
				}
			}
			builder.bindLabel(doneLabel);
			boolean isWhenExhaust = WhenChecker.isWhenExhaustive(expression, trace);
			if(!hasElseOrIrrefutableBranch && !isWhenExhaust)
			{
				trace.report(NO_ELSE_IN_WHEN.on(expression));
			}
			builder.stopAllowDead();
		}

		@Override
		public void visitObjectLiteralExpression(NapileObjectLiteralExpression expression)
		{
			NapileAnonymClass declaration = expression.getObjectDeclaration();
			value(declaration, inCondition);

			List<NapileDeclaration> declarations = declaration.getDeclarations();
			List<NapileDeclaration> functions = Lists.newArrayList();
			for(NapileDeclaration localDeclaration : declarations)
			{
				if(!(localDeclaration instanceof NapileProperty) )
				{
					functions.add(localDeclaration);
				}
			}
			for(NapileDeclaration function : functions)
			{
				value(function, inCondition);
			}
			builder.read(expression);
		}

		@Override
		public void visitAnonymClass(NapileAnonymClass objectDeclaration)
		{
			visitClassOrObject(objectDeclaration);
		}

		@Override
		public void visitStringTemplateExpression(NapileStringTemplateExpression expression)
		{
			for(NapileStringTemplateEntry entry : expression.getEntries())
			{
				if(entry instanceof NapileStringTemplateEntryWithExpression)
				{
					NapileStringTemplateEntryWithExpression entryWithExpression = (NapileStringTemplateEntryWithExpression) entry;
					value(entryWithExpression.getExpression(), false);
				}
			}
			builder.read(expression);
		}

		private void visitClassOrObject(NapileClassLike classOrObject)
		{
			for(NapileDelegationSpecifier specifier : classOrObject.getDelegationSpecifiers())
			{
				value(specifier, inCondition);
			}
			List<NapileDeclaration> declarations = classOrObject.getDeclarations();
			List<NapileProperty> properties = Lists.newArrayList();
			for(NapileDeclaration declaration : declarations)
			{
				if(declaration instanceof NapileProperty)
				{
					value(declaration, inCondition);
					properties.add((NapileProperty) declaration);
				}
			}
		}

		@Override
		public void visitClass(NapileClass klass)
		{
			visitClassOrObject(klass);
		}

		@Override
		public void visitDelegationToSuperCallSpecifier(NapileDelegatorToSuperCall call)
		{
			List<? extends ValueArgument> valueArguments = call.getValueArguments();
			for(ValueArgument valueArgument : valueArguments)
			{
				value(valueArgument.getArgumentExpression(), inCondition);
			}
		}

		@Override
		public void visitJetElement(NapileElement element)
		{
			builder.unsupported(element);
		}
	}
}
