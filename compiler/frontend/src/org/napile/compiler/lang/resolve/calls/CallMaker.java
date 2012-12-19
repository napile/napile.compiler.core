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

package org.napile.compiler.lang.resolve.calls;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.psi.Call.CallType;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileTypeReference;
import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;

/**
 * @author abreslav
 */
public class CallMaker
{

	private static class ExpressionValueArgument implements ValueArgument
	{

		private final NapileExpression expression;

		private final PsiElement reportErrorsOn;

		private ExpressionValueArgument(@Nullable NapileExpression expression, @NotNull PsiElement reportErrorsOn)
		{
			this.expression = expression;
			this.reportErrorsOn = expression == null ? reportErrorsOn : expression;
		}

		@Override
		public NapileExpression getArgumentExpression()
		{
			return expression;
		}

		@Override
		public NapileValueArgumentName getArgumentName()
		{
			return null;
		}

		@Override
		public boolean isNamed()
		{
			return false;
		}

		@NotNull
		@Override
		public PsiElement asElement()
		{
			return reportErrorsOn;
		}

		@Override
		public LeafPsiElement getSpreadElement()
		{
			return null;
		}
	}

	private static class CallImpl implements Call
	{

		private final PsiElement callElement;
		private final ReceiverDescriptor explicitReceiver;
		private ASTNode callOperationNode;
		private final NapileExpression calleeExpression;
		private final List<? extends ValueArgument> valueArguments;
		private final Call.CallType callType;

		protected CallImpl(@Nullable PsiElement callElement, @NotNull ReceiverDescriptor explicitReceiver, @Nullable ASTNode callOperationNode, @NotNull NapileExpression calleeExpression, @NotNull List<? extends ValueArgument> valueArguments)
		{
			this(callElement, explicitReceiver, callOperationNode, calleeExpression, valueArguments, CallType.DEFAULT);
		}

		protected CallImpl(@Nullable PsiElement callElement, @NotNull ReceiverDescriptor explicitReceiver, @Nullable ASTNode callOperationNode, @NotNull NapileExpression calleeExpression, @NotNull List<? extends ValueArgument> valueArguments, @NotNull CallType callType)
		{
			this.callElement = callElement;
			this.explicitReceiver = explicitReceiver;
			this.callOperationNode = callOperationNode;
			this.calleeExpression = calleeExpression;
			this.valueArguments = valueArguments;
			this.callType = callType;
		}

		@Override
		public ASTNode getCallOperationNode()
		{
			return callOperationNode;
		}

		@NotNull
		@Override
		public ReceiverDescriptor getExplicitReceiver()
		{
			return explicitReceiver;
		}

		@NotNull
		@Override
		public ReceiverDescriptor getThisObject()
		{
			return ReceiverDescriptor.NO_RECEIVER;
		}

		@Override
		public NapileExpression getCalleeExpression()
		{
			return calleeExpression;
		}

		@NotNull
		@Override
		public List<? extends ValueArgument> getValueArguments()
		{
			return valueArguments;
		}

		@NotNull
		@Override
		public PsiElement getCallElement()
		{
			return callElement;
		}

		@Override
		public NapileValueArgumentList getValueArgumentList()
		{
			return null;
		}

		@NotNull
		@Override
		public List<NapileExpression> getFunctionLiteralArguments()
		{
			return Collections.emptyList();
		}

		@NotNull
		@Override
		public List<NapileTypeReference> getTypeArguments()
		{
			return Collections.emptyList();
		}

		@Override
		public NapileTypeArgumentList getTypeArgumentList()
		{
			return null;
		}

		@Override
		public String toString()
		{
			return getCallElement().getText();
		}

		@NotNull
		@Override
		public CallType getCallType()
		{
			return callType;
		}
	}

	public static Call makeCallWithExpressions(@NotNull NapileElement callElement, @NotNull ReceiverDescriptor explicitReceiver, @Nullable ASTNode callOperationNode, @NotNull NapileExpression calleeExpression, @NotNull List<NapileExpression> argumentExpressions)
	{
		return makeCallWithExpressions(callElement, explicitReceiver, callOperationNode, calleeExpression, argumentExpressions, CallType.DEFAULT);
	}

	public static Call makeCallWithExpressions(@NotNull NapileElement callElement, @NotNull ReceiverDescriptor explicitReceiver, @Nullable ASTNode callOperationNode, @NotNull NapileExpression calleeExpression, @NotNull List<NapileExpression> argumentExpressions, @NotNull CallType callType)
	{
		List<ValueArgument> arguments = Lists.newArrayList();
		for(NapileExpression argumentExpression : argumentExpressions)
		{
			arguments.add(makeValueArgument(argumentExpression, calleeExpression));
		}
		return makeCall(callElement, explicitReceiver, callOperationNode, calleeExpression, arguments, callType);
	}

	public static Call makeCall(NapileElement callElement, ReceiverDescriptor explicitReceiver, @Nullable ASTNode callOperationNode, NapileExpression calleeExpression, List<? extends ValueArgument> arguments)
	{
		return makeCall(callElement, explicitReceiver, callOperationNode, calleeExpression, arguments, CallType.DEFAULT);
	}

	public static Call makeCall(NapileElement callElement, ReceiverDescriptor explicitReceiver, @Nullable ASTNode callOperationNode, NapileExpression calleeExpression, List<? extends ValueArgument> arguments, CallType callType)
	{
		return new CallImpl(callElement, explicitReceiver, callOperationNode, calleeExpression, arguments, callType);
	}

	public static Call makeCall(@NotNull ReceiverDescriptor leftAsReceiver, NapileBinaryExpression expression)
	{
		return makeCallWithExpressions(expression, leftAsReceiver, null, expression.getOperationReference(), Collections.singletonList(expression.getRight()));
	}

	public static Call makeCall(@NotNull ReceiverDescriptor baseAsReceiver, NapileUnaryExpression expression)
	{
		return makeCall(expression, baseAsReceiver, null, expression.getOperationReference(), Collections.<ValueArgument>emptyList());
	}

	public static Call makeVariableSetCall(@NotNull ReceiverDescriptor receiverDescriptor, @NotNull NapileExpression callExp, @NotNull NapileExpression leftExpression, @NotNull NapileExpression rightExpression)
	{
		return makeCallWithExpressions(callExp, receiverDescriptor, null, leftExpression, Collections.singletonList(rightExpression), CallType.DEFAULT);
	}

	public static Call makeArraySetCall(@NotNull ReceiverDescriptor arrayAsReceiver, @NotNull NapileArrayAccessExpressionImpl arrayAccessExpression, @NotNull NapileExpression rightHandSide, @NotNull CallType callType)
	{
		List<NapileExpression> arguments = Lists.newArrayList(arrayAccessExpression.getIndexExpressions());
		arguments.add(rightHandSide);
		return makeCallWithExpressions(arrayAccessExpression, arrayAsReceiver, null, arrayAccessExpression, arguments, callType);
	}

	public static Call makeArrayGetCall(@NotNull ReceiverDescriptor arrayAsReceiver, @NotNull NapileArrayAccessExpressionImpl arrayAccessExpression, @NotNull CallType callType)
	{
		return makeCallWithExpressions(arrayAccessExpression, arrayAsReceiver, null, arrayAccessExpression, arrayAccessExpression.getIndexExpressions(), callType);
	}

	public static ValueArgument makeValueArgument(@NotNull NapileExpression expression)
	{
		return makeValueArgument(expression, expression);
	}

	public static ValueArgument makeValueArgument(@Nullable NapileExpression expression, @NotNull PsiElement reportErrorsOn)
	{
		return new ExpressionValueArgument(expression, reportErrorsOn);
	}

	public static Call makeVariableCall(@NotNull ReceiverDescriptor explicitReceiver, @Nullable ASTNode callOperationNode, @NotNull NapileSimpleNameExpression nameExpression)
	{
		return makeCallWithExpressions(nameExpression, explicitReceiver, callOperationNode, nameExpression, Collections.<NapileExpression>emptyList());
	}

	public static Call makeCall(@NotNull final ReceiverDescriptor explicitReceiver, @Nullable final ASTNode callOperationNode, @NotNull final NapileCallElement callElement)
	{
		return new Call()
		{
			@Override
			public ASTNode getCallOperationNode()
			{
				return callOperationNode;
			}

			@NotNull
			@Override
			public ReceiverDescriptor getExplicitReceiver()
			{
				return explicitReceiver;
			}

			@NotNull
			@Override
			public ReceiverDescriptor getThisObject()
			{
				return ReceiverDescriptor.NO_RECEIVER;
			}

			@Nullable
			@Override
			public NapileExpression getCalleeExpression()
			{
				return callElement.getCalleeExpression();
			}

			@Override
			@Nullable
			public NapileValueArgumentList getValueArgumentList()
			{
				return callElement.getValueArgumentList();
			}

			@Override
			@NotNull
			public List<? extends ValueArgument> getValueArguments()
			{
				return callElement.getValueArguments();
			}

			@NotNull
			@Override
			public List<NapileExpression> getFunctionLiteralArguments()
			{
				return callElement.getFunctionLiteralArguments();
			}

			@NotNull
			@Override
			public List<NapileTypeReference> getTypeArguments()
			{
				return callElement.getTypeArguments();
			}

			@Nullable
			@Override
			public NapileTypeArgumentList getTypeArgumentList()
			{
				return callElement.getTypeArgumentList();
			}

			@NotNull
			@Override
			public PsiElement getCallElement()
			{
				return callElement;
			}

			@Override
			public String toString()
			{
				return callElement.getText();
			}

			@NotNull
			@Override
			public CallType getCallType()
			{
				return CallType.DEFAULT;
			}
		};
	}
}
