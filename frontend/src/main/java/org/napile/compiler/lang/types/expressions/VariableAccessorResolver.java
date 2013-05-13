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

import static org.napile.compiler.lang.resolve.BindingTraceKeys.EXPRESSION_TYPE;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.AsmConstants;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.AbstractCallParameterDescriptorImpl;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.LocalVariableDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableAccessorDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileArrayAccessExpressionImpl;
import org.napile.compiler.lang.psi.NapileBinaryExpression;
import org.napile.compiler.lang.psi.NapileDotQualifiedExpressionImpl;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapilePsiFactory;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileUnaryExpression;
import org.napile.compiler.lang.psi.NapileVariableAccessor;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;
import org.napile.compiler.lang.resolve.calls.CallMaker;
import org.napile.compiler.lang.resolve.calls.OverloadResolutionResults;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.MultiTypeConstructor;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.util.slicedmap.WritableSlice;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 17:32/19.12.12
 */
public class VariableAccessorResolver
{
	@Nullable
	private static Object[] getReceiverAndName(@NotNull NapileExpression exp, @NotNull ExpressionTypingContext context)
	{
		Name name = null;
		NapileSimpleNameExpression nameExpression = null;
		ReceiverDescriptor receiverDescriptor = null;
		if(exp instanceof NapileSimpleNameExpression)
		{
			name = Name.identifier(((NapileSimpleNameExpression) exp).getReferencedName() + AsmConstants.ANONYM_SPLITTER + "set");
			receiverDescriptor = ReceiverDescriptor.NO_RECEIVER;
			nameExpression = (NapileSimpleNameExpression) exp;
		}
		else if(exp instanceof NapileDotQualifiedExpressionImpl)
		{
			NapileDotQualifiedExpressionImpl dotQualifiedExpression = ((NapileDotQualifiedExpressionImpl) exp);

			final NapileExpression receiverExpression = dotQualifiedExpression.getReceiverExpression();
			if(receiverExpression == null)
			{
				return null;
			}
			NapileType receiverType = context.trace.get(BindingTraceKeys.EXPRESSION_TYPE, receiverExpression);
			if(receiverType != null && receiverType.getConstructor() instanceof MultiTypeConstructor)
				return null;

			if(dotQualifiedExpression.getSelectorExpression() instanceof NapileSimpleNameExpression)
			{
				nameExpression = (NapileSimpleNameExpression) dotQualifiedExpression.getSelectorExpression();
				name = Name.identifier(nameExpression.getReferencedName() + AsmConstants.ANONYM_SPLITTER + "set");
				NapileType type = context.trace.get(EXPRESSION_TYPE, receiverExpression);
				if(type == null)
					return null;
				receiverDescriptor = new ExpressionReceiver(receiverExpression, type);
			}
		}
		else if(exp instanceof NapileArrayAccessExpressionImpl)
			return null;

		DeclarationDescriptor declarationDescriptor = context.trace.get(BindingTraceKeys.REFERENCE_TARGET, nameExpression);
		// local variable and call parameter cant have setter and getter
		if(declarationDescriptor instanceof LocalVariableDescriptor || declarationDescriptor instanceof AbstractCallParameterDescriptorImpl)
			return null;
		if(receiverDescriptor == null)
			return null;
		return new Object[] {name, receiverDescriptor, nameExpression};
	}

	public static VariableDescriptor resolveSetterForReferenceParameter(@NotNull NapileSimpleNameExpression expression, @NotNull ExpressionTypingServices expressionTypingServices, BindingTrace originTrace, NapileScope scope)
	{
		ExpressionTypingContext context = ExpressionTypingContext.newContext(expressionTypingServices, originTrace, scope, DataFlowInfo.EMPTY, TypeUtils.NO_EXPECTED_TYPE, false);

		Object[] pair = getReceiverAndName(expression, context);
		if(pair == null)
			return null;

		Name name = (Name) pair[0];
		ReceiverDescriptor receiverDescriptor = (ReceiverDescriptor) pair[1];
		//NapileSimpleNameExpression nameExpression = (NapileSimpleNameExpression) pair[2];

		NapileExpression argument = NapilePsiFactory.createExpression(expression.getProject(), "null");
		TemporaryBindingTrace trace = TemporaryBindingTrace.create(context.trace);

		OverloadResolutionResults<MethodDescriptor> results = context.replaceBindingTrace(trace).resolveCallWithGivenName(CallMaker.makeVariableSetCall(receiverDescriptor, expression, expression, argument), expression, name, false);
		if(results.isSuccess())
			context.trace.record(BindingTraceKeys.VARIABLE_CALL, expression, results.getResultingDescriptor());

		copyResolvingErrors(context, trace);

		OverloadResolutionResults<VariableDescriptor> varResolve = context.resolveSimpleProperty(receiverDescriptor, null, expression);
		return varResolve.isSingleResult() ? varResolve.getResultingDescriptor() : null;
	}

	public static void resolveSetterForBinaryCall(@NotNull NapileBinaryExpression expression, @NotNull ExpressionTypingContext context)
	{
		NapileExpression left = expression.getLeft();

		Object[] pair = getReceiverAndName(left, context);
		if(pair == null)
			return;

		Name name = (Name) pair[0];
		ReceiverDescriptor receiverDescriptor = (ReceiverDescriptor) pair[1];
		//NapileSimpleNameExpression nameExpression = (NapileSimpleNameExpression) pair[2];

		NapileExpression argument = NapilePsiFactory.createExpression(expression.getProject(), "null");
		TemporaryBindingTrace trace = TemporaryBindingTrace.create(context.trace);

		OverloadResolutionResults<MethodDescriptor> results = context.replaceBindingTrace(trace).resolveCallWithGivenName(CallMaker.makeVariableSetCall(receiverDescriptor, expression.getOperationReference(), left, argument), expression.getOperationReference(), name, false);
		if(results.isSingleResult())
			context.trace.record(BindingTraceKeys.VARIABLE_CALL, expression, results.getResultingDescriptor());

		copyResolvingErrors(context, trace);
	}

	public static void resolveSetterForUnaryCalL(@NotNull NapileUnaryExpression expression, @NotNull ExpressionTypingContext context)
	{
		NapileExpression left = expression.getBaseExpression();
		Object[] pair = getReceiverAndName(left, context);
		if(pair == null)
			return;

		Name name = (Name) pair[0];
		ReceiverDescriptor receiverDescriptor = (ReceiverDescriptor) pair[1];
		NapileSimpleNameExpression nameExpression = (NapileSimpleNameExpression) pair[2];

		NapileExpression argument = NapilePsiFactory.createExpression(expression.getProject(), "null");
		TemporaryBindingTrace trace = TemporaryBindingTrace.create(context.trace);

		OverloadResolutionResults<MethodDescriptor> results = context.replaceBindingTrace(trace).resolveCallWithGivenName(CallMaker.makeVariableSetCall(receiverDescriptor, expression.getBaseExpression(), left, argument), nameExpression, name, false);
		if(results.isSingleResult())
			context.trace.record(BindingTraceKeys.VARIABLE_CALL, expression, results.getResultingDescriptor());
		else
			copyResolvingErrors(context, trace);
	}

	public static void resolveGetter(@NotNull NapileSimpleNameExpression expression, @NotNull ReceiverDescriptor receiverDescriptor, @NotNull ExpressionTypingContext context)
	{
		DeclarationDescriptor declarationDescriptor = context.trace.get(BindingTraceKeys.REFERENCE_TARGET, expression);
		// local variable and call parameter cant have setter and getter
		if(declarationDescriptor instanceof LocalVariableDescriptor || declarationDescriptor instanceof AbstractCallParameterDescriptorImpl)
			return;

		Name name = Name.identifier(expression.getReferencedName() + AsmConstants.ANONYM_SPLITTER + "get");

		TemporaryBindingTrace trace = TemporaryBindingTrace.create(context.trace);

		NapileSimpleNameExpression argument = expression;//(NapileSimpleNameExpression) NapilePsiFactory.createExpression(expression.getProject(), expression.getText());

		OverloadResolutionResults<MethodDescriptor> results = context.replaceBindingTrace(trace).resolveCallWithGivenName(CallMaker.makeVariableGetCall(receiverDescriptor, expression, argument), argument, name, false);

		if(results.isSingleResult())
			context.trace.record(BindingTraceKeys.VARIABLE_CALL, expression, results.getResultingDescriptor());
		else
			copyResolvingErrors(context, trace);
	}

	private static void copyResolvingErrors(@NotNull ExpressionTypingContext context, @NotNull BindingTrace trace)
	{
		for(Diagnostic d : trace.getDiagnostics())
			if(d.getFactory() == Errors.INVISIBLE_MEMBER)
				context.trace.report(d);
	}

	@NotNull
	public static WritableSlice<PsiElement, VariableAccessorDescriptor> getSliceForAccessor(@NotNull NapileVariableAccessor accessor)
	{
		return accessor.getAccessorElementType() == NapileTokens.SET_KEYWORD ? BindingTraceKeys.VARIABLE_SET_ACCESSOR : BindingTraceKeys.VARIABLE_GET_ACCESSOR;
	}
}
