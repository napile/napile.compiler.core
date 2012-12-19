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

package org.napile.compiler.lang.types.expressions;

import static org.napile.compiler.lang.resolve.BindingContext.EXPRESSION_TYPE;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.AsmConstants;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.psi.NapileArrayAccessExpressionImpl;
import org.napile.compiler.lang.psi.NapileBinaryExpression;
import org.napile.compiler.lang.psi.NapileDotQualifiedExpression;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapilePsiFactory;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;
import org.napile.compiler.lang.resolve.calls.CallMaker;
import org.napile.compiler.lang.resolve.calls.OverloadResolutionResults;
import org.napile.compiler.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;

/**
 * @author VISTALL
 * @date 17:32/19.12.12
 */
public class VariableAccessorResolver
{
	public static void resolveForBinaryCall(@NotNull NapileBinaryExpression expression, @NotNull ExpressionTypingContext context)
	{
		NapileExpression left = expression.getLeft();
		Name name = null;
		ReceiverDescriptor receiverDescriptor = null;
		if(left instanceof NapileSimpleNameExpression)
		{
			name = Name.identifier(((NapileSimpleNameExpression) left).getReferencedName() + AsmConstants.ANONYM_SPLITTER + "set");
			receiverDescriptor = ReceiverDescriptor.NO_RECEIVER;
		}
		else if(left instanceof NapileDotQualifiedExpression)
		{
			NapileDotQualifiedExpression dotQualifiedExpression = ((NapileDotQualifiedExpression) left);

			if(dotQualifiedExpression.getSelectorExpression() instanceof NapileSimpleNameExpression)
			{
				name = Name.identifier(((NapileSimpleNameExpression) dotQualifiedExpression.getSelectorExpression()).getReferencedName() + AsmConstants.ANONYM_SPLITTER + "set");
				receiverDescriptor = new ExpressionReceiver(dotQualifiedExpression.getReceiverExpression(), context.trace.get(EXPRESSION_TYPE, dotQualifiedExpression.getReceiverExpression()));
			}
		}
		else if(left instanceof NapileArrayAccessExpressionImpl)
			return;

		NapileExpression argument = NapilePsiFactory.createExpression(expression.getProject(), "null");
		TemporaryBindingTrace trace = TemporaryBindingTrace.create(context.trace);

		OverloadResolutionResults<MethodDescriptor> results = context.replaceBindingTrace(trace).resolveCallWithGivenName(CallMaker.makeVariableSetCall(receiverDescriptor, expression.getOperationReference(), left, argument), expression.getOperationReference(), name);
		if(!results.isSuccess())
		{
			trace.commit();
			return;
		}

		context.trace.record(BindingContext.VARIABLE_CALL, expression, results.getResultingDescriptor());
	}
}
