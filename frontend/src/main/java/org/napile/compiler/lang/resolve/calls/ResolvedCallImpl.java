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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;
import org.napile.compiler.lang.resolve.calls.inference.ConstraintSystem;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeSubstitutor;
import com.google.common.collect.Maps;
import com.intellij.openapi.util.Pair;
import com.intellij.util.Function;

/**
 * @author abreslav
 */
public class ResolvedCallImpl<D extends CallableDescriptor> implements ResolvedCallWithTrace<D>
{

	public static final Function<ResolvedCallWithTrace<? extends CallableDescriptor>, CallableDescriptor> MAP_TO_CANDIDATE = new Function<ResolvedCallWithTrace<? extends CallableDescriptor>, CallableDescriptor>()
	{
		@Override
		public CallableDescriptor fun(ResolvedCallWithTrace<? extends CallableDescriptor> resolvedCall)
		{
			return resolvedCall.getCandidateDescriptor();
		}
	};

	public static final Function<ResolvedCallWithTrace<? extends CallableDescriptor>, CallableDescriptor> MAP_TO_RESULT = new Function<ResolvedCallWithTrace<? extends CallableDescriptor>, CallableDescriptor>()
	{
		@Override
		public CallableDescriptor fun(ResolvedCallWithTrace<? extends CallableDescriptor> resolvedCall)
		{
			return resolvedCall.getResultingDescriptor();
		}
	};

	@NotNull
	public static <D extends CallableDescriptor> ResolvedCallImpl<D> create(@NotNull ResolutionCandidate<D> candidate, @NotNull TemporaryBindingTrace trace)
	{
		return new ResolvedCallImpl<D>(candidate, trace);
	}

	private final D candidateDescriptor;
	private D resultingDescriptor; // Probably substituted
	private Pair<VariableDescriptor, ReceiverDescriptor> variableCallInfo;
	private final ReceiverDescriptor thisObject; // receiver object of a method
	private final ExplicitReceiverKind explicitReceiverKind;
	private final boolean isSafeCall;

	private final Map<TypeParameterDescriptor, NapileType> typeArguments = Maps.newLinkedHashMap();
	private final Map<CallParameterDescriptor, NapileType> autoCasts = Maps.newHashMap();
	private final Map<CallParameterDescriptor, ResolvedValueArgument> valueArguments = Maps.newLinkedHashMap();
	private boolean someArgumentHasNoType = false;
	private TemporaryBindingTrace trace;
	private ResolutionStatus status = ResolutionStatus.UNKNOWN_STATUS;
	private boolean hasUnknownTypeParameters = false;
	private ConstraintSystem constraintSystem = null;

	private ResolvedCallImpl(@NotNull ResolutionCandidate<D> candidate, @NotNull TemporaryBindingTrace trace)
	{
		this.candidateDescriptor = candidate.getDescriptor();
		this.thisObject = candidate.getThisObject();
		this.explicitReceiverKind = candidate.getExplicitReceiverKind();
		this.isSafeCall = candidate.isSafeCall();
		this.trace = trace;
	}

	@Override
	@NotNull
	public ResolutionStatus getStatus()
	{
		return status;
	}

	public void addStatus(@NotNull ResolutionStatus status)
	{
		this.status = this.status.combine(status);
	}

	@Override
	public boolean hasUnknownTypeParameters()
	{
		return hasUnknownTypeParameters;
	}

	public void setHasUnknownTypeParameters(boolean hasUnknownTypeParameters)
	{
		this.hasUnknownTypeParameters = hasUnknownTypeParameters;
	}

	@Override
	@NotNull
	public TemporaryBindingTrace getTrace()
	{
		return trace;
	}

	@Override
	@NotNull
	public D getCandidateDescriptor()
	{
		return candidateDescriptor;
	}

	@Nullable
	@Override
	public Pair<VariableDescriptor, ReceiverDescriptor> getVariableCallInfo()
	{
		return variableCallInfo;
	}

	@Override
	@NotNull
	public D getResultingDescriptor()
	{
		return resultingDescriptor == null ? candidateDescriptor : resultingDescriptor;
	}

	public void setResultingSubstitutor(@NotNull TypeSubstitutor substitutor)
	{
		resultingDescriptor = (D) candidateDescriptor.substitute(substitutor);
		assert resultingDescriptor != null : candidateDescriptor;

		Map<CallParameterDescriptor, CallParameterDescriptor> parameterMap = Maps.newHashMap();
		for(CallParameterDescriptor parameterDescriptor : resultingDescriptor.getValueParameters())
		{
			parameterMap.put(parameterDescriptor.getOriginal(), parameterDescriptor);
		}

		Map<CallParameterDescriptor, ResolvedValueArgument> originalValueArguments = Maps.newHashMap(valueArguments);
		valueArguments.clear();
		for(Map.Entry<CallParameterDescriptor, ResolvedValueArgument> entry : originalValueArguments.entrySet())
		{
			CallParameterDescriptor substitutedVersion = parameterMap.get(entry.getKey().getOriginal());
			assert substitutedVersion != null : entry.getKey();
			valueArguments.put(substitutedVersion, entry.getValue());
		}
	}

	public void recordTypeArgument(@NotNull TypeParameterDescriptor typeParameter, @NotNull NapileType typeArgument)
	{
		assert !typeArguments.containsKey(typeParameter) : typeParameter + " -> " + typeArgument;
		typeArguments.put(typeParameter, typeArgument);
	}

	public void setConstraintSystem(@NotNull ConstraintSystem constraintSystem)
	{
		this.constraintSystem = constraintSystem;
	}

	@Nullable
	@Override
	public ConstraintSystem getConstraintSystem()
	{
		return constraintSystem;
	}

	public void recordValueArgument(@NotNull CallParameterDescriptor valueParameter, @NotNull ResolvedValueArgument valueArgument)
	{
		assert !valueArguments.containsKey(valueParameter) : valueParameter + " -> " + valueArgument;
		valueArguments.put(valueParameter, valueArgument);
	}

	public void autoCastValueArgument(@NotNull CallParameterDescriptor parameter, @NotNull NapileType target)
	{
		assert !autoCasts.containsKey(parameter);
		autoCasts.put(parameter, target);
	}

	@Override
	@NotNull
	public ReceiverDescriptor getThisObject()
	{
		return thisObject;
	}

	@Override
	@NotNull
	public ExplicitReceiverKind getExplicitReceiverKind()
	{
		return explicitReceiverKind;
	}

	@Override
	@NotNull
	public Map<CallParameterDescriptor, ResolvedValueArgument> getValueArguments()
	{
		return valueArguments;
	}

	@NotNull
	@Override
	public List<ResolvedValueArgument> getValueArgumentsByIndex()
	{
		List<ResolvedValueArgument> arguments = new ArrayList<ResolvedValueArgument>(candidateDescriptor.getValueParameters().size());
		for(int i = 0; i < candidateDescriptor.getValueParameters().size(); ++i)
		{
			arguments.add(null);
		}

		for(Map.Entry<CallParameterDescriptor, ResolvedValueArgument> entry : valueArguments.entrySet())
		{
			if(arguments.set(entry.getKey().getIndex(), entry.getValue()) != null)
			{
				throw new IllegalStateException();
			}
		}

		for(Object o : arguments)
		{
			if(o == null)
			{
				throw new IllegalStateException();
			}
		}

		return arguments;
	}

	public void argumentHasNoType()
	{
		this.someArgumentHasNoType = true;
	}

	@Override
	public boolean isDirty()
	{
		return someArgumentHasNoType;
	}

	@NotNull
	@Override
	public Map<TypeParameterDescriptor, NapileType> getTypeArguments()
	{
		return typeArguments;
	}

	@Override
	public boolean isSafeCall()
	{
		return isSafeCall;
	}

	public void setVariableCallInfo(Pair<VariableDescriptor, ReceiverDescriptor> variableCallInfo)
	{
		this.variableCallInfo = variableCallInfo;
	}
}
