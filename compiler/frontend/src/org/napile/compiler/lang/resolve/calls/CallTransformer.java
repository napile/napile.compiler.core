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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.FunctionDescriptorUtil;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.Call;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileTypeArgumentList;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileValueArgumentList;
import org.napile.compiler.lang.psi.ValueArgument;
import org.napile.compiler.lang.resolve.ChainedTemporaryBindingTrace;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.MethodTypeConstructor;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * CallTransformer treats specially 'variable as function' call case, other cases keeps unchanged (base realization).
 * <p/>
 * For the call 'b.foo(1)' where foo is a variable that has method 'invoke' (for example of function type)
 * CallTransformer creates two contexts, two calls in each, and performs second ('invoke') call resolution:
 * <p/>
 * context#1. calls: 'b.foo' 'invoke(1)'
 * context#2. calls: 'foo'   'b.invoke(1)'
 * <p/>
 * If success VariableAsFunctionResolvedCall is created.
 *
 * @author svtk
 */
public class CallTransformer<D extends CallableDescriptor, F extends D>
{
	private CallTransformer()
	{
	}

	/**
	 * Returns two contexts for 'variable as function' case (in FUNCTION_CALL_TRANSFORMER), one context otherwise
	 */
	@NotNull
	public Collection<CallResolutionContext<D, F>> createCallContexts(@NotNull ResolutionCandidate<D> candidate, @NotNull ResolutionTask<D, F> task, @NotNull TemporaryBindingTrace candidateTrace)
	{

		ResolvedCallImpl<D> candidateCall = ResolvedCallImpl.create(candidate, candidateTrace);
		return Collections.singleton(CallResolutionContext.create(candidateCall, task, candidateTrace, task.tracing));
	}

	/**
	 * Returns collection of resolved calls for 'invoke' for 'variable as function' case (in FUNCTION_CALL_TRANSFORMER),
	 * the resolved call from callResolutionContext otherwise
	 */
	@NotNull
	public Collection<ResolvedCallWithTrace<F>> transformCall(@NotNull CallResolutionContext<D, F> callResolutionContext, @NotNull CallResolver callResolver, @NotNull ResolutionTask<D, F> task)
	{

		return Collections.singleton((ResolvedCallWithTrace<F>) callResolutionContext.candidateCall);
	}


	public static CallTransformer<VariableDescriptor, VariableDescriptor> PROPERTY_CALL_TRANSFORMER = new CallTransformer<VariableDescriptor, VariableDescriptor>();

	public static CallTransformer<CallableDescriptor, MethodDescriptor> FUNCTION_CALL_TRANSFORMER = new CallTransformer<CallableDescriptor, MethodDescriptor>()
	{
		@NotNull
		@Override
		public Collection<CallResolutionContext<CallableDescriptor, MethodDescriptor>> createCallContexts(@NotNull ResolutionCandidate<CallableDescriptor> candidate, @NotNull ResolutionTask<CallableDescriptor, MethodDescriptor> task, @NotNull TemporaryBindingTrace candidateTrace)
		{

			if(candidate.getDescriptor() instanceof MethodDescriptor)
			{
				return super.createCallContexts(candidate, task, candidateTrace);
			}

			assert candidate.getDescriptor() instanceof VariableDescriptor;

			boolean hasReceiver = candidate.getReceiverArgument().exists();
			Call variableCall = stripCallArguments(task);
			if(!hasReceiver)
			{
				CallResolutionContext<CallableDescriptor, MethodDescriptor> context = CallResolutionContext.create(ResolvedCallImpl.create(candidate, candidateTrace), task, candidateTrace, task.tracing, variableCall);
				return Collections.singleton(context);
			}
			Call variableCallWithoutReceiver = stripReceiver(variableCall);
			CallResolutionContext<CallableDescriptor, MethodDescriptor> contextWithReceiver = createContextWithChainedTrace(candidate, variableCall, candidateTrace, task);

			ResolutionCandidate<CallableDescriptor> candidateWithoutReceiver = ResolutionCandidate.create(candidate.getDescriptor(), candidate.getThisObject(), ReceiverDescriptor.NO_RECEIVER, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, false);

			CallResolutionContext<CallableDescriptor, MethodDescriptor> contextWithoutReceiver = createContextWithChainedTrace(candidateWithoutReceiver, variableCallWithoutReceiver, candidateTrace, task);

			contextWithoutReceiver.receiverForVariableAsFunctionSecondCall = variableCall.getExplicitReceiver();

			return Lists.newArrayList(contextWithReceiver, contextWithoutReceiver);
		}

		private CallResolutionContext<CallableDescriptor, MethodDescriptor> createContextWithChainedTrace(ResolutionCandidate<CallableDescriptor> candidate, Call call, TemporaryBindingTrace temporaryTrace, ResolutionTask<CallableDescriptor, MethodDescriptor> task)
		{

			ChainedTemporaryBindingTrace chainedTrace = ChainedTemporaryBindingTrace.create(temporaryTrace);
			ResolvedCallImpl<CallableDescriptor> resolvedCall = ResolvedCallImpl.create(candidate, chainedTrace);
			return CallResolutionContext.create(resolvedCall, task, chainedTrace, task.tracing, call);
		}

		private Call stripCallArguments(@NotNull ResolutionTask<CallableDescriptor, MethodDescriptor> task)
		{
			return new DelegatingCall(task.call)
			{
				@Override
				public NapileValueArgumentList getValueArgumentList()
				{
					return null;
				}

				@NotNull
				@Override
				public List<? extends ValueArgument> getValueArguments()
				{
					return Collections.emptyList();
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
			};
		}

		private Call stripReceiver(@NotNull Call variableCall)
		{
			return new DelegatingCall(variableCall)
			{
				@NotNull
				@Override
				public ReceiverDescriptor getExplicitReceiver()
				{
					return ReceiverDescriptor.NO_RECEIVER;
				}
			};
		}

		@NotNull
		@Override
		public Collection<ResolvedCallWithTrace<MethodDescriptor>> transformCall(@NotNull final CallResolutionContext<CallableDescriptor, MethodDescriptor> context, @NotNull CallResolver callResolver, @NotNull final ResolutionTask<CallableDescriptor, MethodDescriptor> task)
		{

			final CallableDescriptor descriptor = context.candidateCall.getCandidateDescriptor();
			if(descriptor instanceof MethodDescriptor)
			{
				return super.transformCall(context, callResolver, task);
			}

			assert descriptor instanceof VariableDescriptor;
			JetType returnType = descriptor.getReturnType();
			if(returnType == null || !(returnType.getConstructor() instanceof MethodTypeConstructor))
				return Collections.emptyList();

			final ResolvedCallWithTrace<VariableDescriptor> variableResolvedCall = (ResolvedCallWithTrace) context.candidateCall;

			MethodDescriptor methodDescriptor = FunctionDescriptorUtil.createDescriptorFromType(descriptor.getName(), returnType, descriptor.getContainingDeclaration());

			final TemporaryBindingTrace variableCallTrace = context.candidateCall.getTrace();

			ResolutionCandidate<MethodDescriptor> resolutionCandidate = ResolutionCandidate.create(methodDescriptor, false);

			OverloadResolutionResults<MethodDescriptor> results = OverloadResolutionResultsImpl.success(ResolvedCallImpl.create(resolutionCandidate, variableCallTrace));
			Collection<ResolvedCallWithTrace<MethodDescriptor>> calls = ((OverloadResolutionResultsImpl<MethodDescriptor>) results).getResultingCalls();

			return Collections2.transform(calls, new Function<ResolvedCallWithTrace<MethodDescriptor>, ResolvedCallWithTrace<MethodDescriptor>>()
			{
				@Override
				public ResolvedCallWithTrace<MethodDescriptor> apply(ResolvedCallWithTrace<MethodDescriptor> functionResolvedCall)
				{
					return new VariableAsFunctionResolvedCall(functionResolvedCall, variableResolvedCall);
				}
			});
		}
	};

	public static class CallForImplicitInvoke extends DelegatingCall
	{
		public CallForImplicitInvoke(@NotNull Call delegate)
		{
			super(delegate);
		}
	}
}
