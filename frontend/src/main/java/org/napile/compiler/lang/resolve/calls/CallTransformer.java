/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.napile.compiler.lang.resolve.calls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.lib.NapileAnnotationPackage;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptorUtil;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.Call;
import org.napile.compiler.lang.psi.ValueArgument;
import org.napile.compiler.lang.resolve.AnnotationUtils;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;
import org.napile.compiler.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.SubstitutionUtils;
import org.napile.compiler.lang.types.TypeSubstitutor;
import org.napile.compiler.lang.types.checker.NapileTypeChecker;
import com.intellij.openapi.util.Pair;

/**
 * Call transformer. It change resolving target if variable type is method type & expression is call
 *
 * @author svtk
 * @author VISTALL
 */
public class CallTransformer<D extends CallableDescriptor, F extends D>
{
	public static CallTransformer<VariableDescriptor, VariableDescriptor> VARIABLE_CALL_TRANSFORMER = new CallTransformer<VariableDescriptor, VariableDescriptor>();

	public static CallTransformer<CallableDescriptor, MethodDescriptor> ANONYM_METHOD_CALL_TRANSFORMER = new CallTransformer<CallableDescriptor, MethodDescriptor>()
	{
		@NotNull
		@Override
		public Collection<CallResolutionContext<CallableDescriptor, MethodDescriptor>> createCallContexts(@NotNull ResolutionCandidate<CallableDescriptor> candidate, @NotNull ResolutionTask<CallableDescriptor, MethodDescriptor> task, @NotNull TemporaryBindingTrace candidateTrace)
		{
			if(candidate.getDescriptor() instanceof MethodDescriptor)
				return super.createCallContexts(candidate, task, candidateTrace);

			assert candidate.getDescriptor() instanceof VariableDescriptor;

			VariableDescriptor variableDescriptor = (VariableDescriptor) candidate.getDescriptor();

			CallableDescriptor callableDescriptor = MethodDescriptorUtil.createDescriptorFromType(variableDescriptor.getName(), variableDescriptor.getType(), variableDescriptor.getContainingDeclaration());
			if(callableDescriptor == null)
				return Collections.emptyList();

			ResolvedCallImpl<CallableDescriptor> call = ResolvedCallImpl.create(ResolutionCandidate.create(callableDescriptor, false), candidateTrace);
			call.setVariableCallInfo(new Pair<VariableDescriptor, ReceiverDescriptor>(variableDescriptor, candidate.getThisObject()));

			CallResolutionContext<CallableDescriptor, MethodDescriptor> context = CallResolutionContext.create(call, task, candidateTrace, task.tracing, task.call);
			return Collections.singleton(context);
		}
	};

	private CallTransformer()
	{
	}

	/**
	 * Returns one context otherwise
	 */
	@NotNull
	public Collection<CallResolutionContext<D, F>> createCallContexts(@NotNull ResolutionCandidate<D> candidate, @NotNull ResolutionTask<D, F> task, @NotNull TemporaryBindingTrace candidateTrace)
	{
		ResolvedCallImpl<D> candidateCall = ResolvedCallImpl.create(candidate, candidateTrace);
		CallableDescriptor callableDescriptor = candidate.getDescriptor();

		extensionLabel:
		{
			if(AnnotationUtils.hasAnnotation(candidate.getDescriptor(), NapileAnnotationPackage.EXTENSION))
			{
				final ReceiverDescriptor receiverDescriptor = task.call.getExplicitReceiver();
				if(receiverDescriptor instanceof ExpressionReceiver)
				{
					CallParameterDescriptor parameterDescriptor = callableDescriptor.getValueParameters().isEmpty() ? null : callableDescriptor.getValueParameters().get(0);
					if(parameterDescriptor == null)
					{
						break extensionLabel;
					}

					NapileType parameterType = parameterDescriptor.getType();
					if(SubstitutionUtils.hasUnsubstitutedTypeParameters(parameterType))
					{
						parameterType = TypeSubstitutor.DEFAULT_TYPE_FOR_TYPE_PARAMETERS.substitute(parameterType, null);
					}

					if(NapileTypeChecker.INSTANCE.isSubtypeOf(receiverDescriptor.getType(), parameterType))
					{
						Call call = new DelegatingCall(task.call)
						{
							@Override
							@NotNull
							public List<? extends ValueArgument> getValueArguments()
							{
								List<? extends ValueArgument> list = super.getValueArguments();
								List<ValueArgument> arguments = new ArrayList<ValueArgument>(list.size() + 1);
								arguments.add(CallMaker.makeValueArgument(((ExpressionReceiver) receiverDescriptor).getExpression()));
								arguments.addAll(list);
								return arguments;
							}
						};
						return Collections.singleton(CallResolutionContext.create(candidateCall, task, candidateTrace, task.tracing, call));
					}
				}
			}
		}
		return Collections.singleton(CallResolutionContext.create(candidateCall, task, candidateTrace, task.tracing));
	}
}
