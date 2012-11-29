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

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.Call;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;

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
				return super.createCallContexts(candidate, task, candidateTrace);

			assert candidate.getDescriptor() instanceof VariableDescriptor;

			CallableDescriptor callableDescriptor = candidate.getDescriptor().getCallableDescriptor();
			if(callableDescriptor == null)
				return Collections.emptyList();
			ResolvedCallImpl<CallableDescriptor> call = ResolvedCallImpl.create(ResolutionCandidate.create(callableDescriptor, false), candidateTrace);
			call.setVariableDescriptor((VariableDescriptor) candidate.getDescriptor());
			CallResolutionContext<CallableDescriptor, MethodDescriptor> context = CallResolutionContext.create(call, task, candidateTrace, task.tracing, task.call);
			return Collections.singleton(context);
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
