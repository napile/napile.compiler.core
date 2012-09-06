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

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;

/**
 * @author svtk
 */
public class VariableAsFunctionResolvedCall implements ResolvedCallWithTrace<MethodDescriptor>
{
	private final ResolvedCallWithTrace<MethodDescriptor> functionCall;
	private final ResolvedCallWithTrace<VariableDescriptor> variableCall;

	public VariableAsFunctionResolvedCall(@NotNull ResolvedCallWithTrace<MethodDescriptor> functionCall, @NotNull ResolvedCallWithTrace<VariableDescriptor> variableCall)
	{
		this.functionCall = functionCall;
		this.variableCall = variableCall;
	}

	public ResolvedCallWithTrace<MethodDescriptor> getFunctionCall()
	{
		return functionCall;
	}

	public ResolvedCallWithTrace<VariableDescriptor> getVariableCall()
	{
		return variableCall;
	}

	@NotNull
	@Override
	public MethodDescriptor getCandidateDescriptor()
	{
		return functionCall.getResultingDescriptor();
	}

	@NotNull
	@Override
	public MethodDescriptor getResultingDescriptor()
	{
		return functionCall.getResultingDescriptor();
	}

	@NotNull
	@Override
	public ReceiverDescriptor getReceiverArgument()
	{
		return variableCall.getReceiverArgument();
	}

	@NotNull
	@Override
	public ReceiverDescriptor getThisObject()
	{
		return variableCall.getThisObject();
	}

	@NotNull
	@Override
	public ExplicitReceiverKind getExplicitReceiverKind()
	{
		return variableCall.getExplicitReceiverKind();
	}

	@NotNull
	@Override
	public Map<ParameterDescriptor, ResolvedValueArgument> getValueArguments()
	{
		return functionCall.getValueArguments();
	}

	@NotNull
	@Override
	public List<ResolvedValueArgument> getValueArgumentsByIndex()
	{
		return functionCall.getValueArgumentsByIndex();
	}

	@NotNull
	@Override
	public Map<TypeParameterDescriptor, JetType> getTypeArguments()
	{
		return functionCall.getTypeArguments();
	}

	@NotNull
	@Override
	public ResolutionStatus getStatus()
	{
		if(variableCall.getStatus() == ResolutionStatus.SUCCESS)
		{
			return functionCall.getStatus();
		}
		return variableCall.getStatus();
	}

	@Override
	public boolean isDirty()
	{
		return functionCall.isDirty();
	}

	@Override
	public TemporaryBindingTrace getTrace()
	{
		//functionCall.trace is temporary trace above variableCall.trace and is committed already
		return variableCall.getTrace();
	}

	@Override
	public boolean isSafeCall()
	{
		return variableCall.isSafeCall();
	}

	@Override
	public boolean hasUnknownTypeParameters()
	{
		return variableCall.hasUnknownTypeParameters();
	}
}
