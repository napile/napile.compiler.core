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

package org.napile.compiler.lang.resolve.calls.inference;

import static org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo.BOUNDS_FOR_KNOWNS;
import static org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo.BOUNDS_FOR_UNKNOWNS;
import static org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo.ERRORS;
import static org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo.LOG;
import static org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo.SOLUTION;
import static org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo.UNKNOWNS;

import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.resolve.calls.ResolutionDebugInfo;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.types.JetType;
import com.google.common.collect.Maps;

/**
 * @author abreslav
 */
public class DebugConstraintResolutionListener implements ConstraintResolutionListener
{

	private final ResolutionDebugInfo.Data debugInfo;
	private final ResolvedCall<? extends CallableDescriptor> candidateCall;

	public DebugConstraintResolutionListener(@NotNull ResolvedCall<? extends CallableDescriptor> candidateCall, @NotNull ResolutionDebugInfo.Data debugInfo)
	{
		this.debugInfo = debugInfo;
		this.candidateCall = candidateCall;
	}

	@Override
	public void constraintsForUnknown(TypeParameterDescriptor typeParameterDescriptor, BoundsOwner typeValue)
	{
		if(!ResolutionDebugInfo.isResolutionDebugEnabled())
			return;
		Map<TypeParameterDescriptor, BoundsOwner> map = debugInfo.getByKey(BOUNDS_FOR_UNKNOWNS, candidateCall);
		if(map == null)
		{
			map = Maps.newLinkedHashMap();
			debugInfo.putByKey(BOUNDS_FOR_UNKNOWNS, candidateCall, map);
		}
		map.put(typeParameterDescriptor, typeValue);
	}

	@Override
	public void constraintsForKnownType(JetType type, BoundsOwner typeValue)
	{
		if(!ResolutionDebugInfo.isResolutionDebugEnabled())
			return;
		Map<JetType, BoundsOwner> map = debugInfo.getByKey(BOUNDS_FOR_KNOWNS, candidateCall);
		if(map == null)
		{
			map = Maps.newLinkedHashMap();
			debugInfo.putByKey(BOUNDS_FOR_KNOWNS, candidateCall, map);
		}
		map.put(type, typeValue);
	}

	@Override
	public void done(ConstraintSystemSolution solution, Set<TypeParameterDescriptor> typeParameterDescriptors)
	{
		if(!ResolutionDebugInfo.isResolutionDebugEnabled())
			return;
		debugInfo.putByKey(SOLUTION, candidateCall, solution);
		debugInfo.putByKey(UNKNOWNS, candidateCall, typeParameterDescriptors);
	}

	@Override
	public void log(Object... messageFragments)
	{
		if(!ResolutionDebugInfo.isResolutionDebugEnabled())
			return;
		StringBuilder stringBuilder = debugInfo.getByKey(LOG, candidateCall);
		if(stringBuilder == null)
		{
			stringBuilder = new StringBuilder();
			debugInfo.putByKey(LOG, candidateCall, stringBuilder);
		}
		for(Object m : messageFragments)
		{
			stringBuilder.append(m);
		}
		stringBuilder.append("\n");
	}

	@Override
	public void error(Object... messageFragments)
	{
		if(!ResolutionDebugInfo.isResolutionDebugEnabled())
			return;
		StringBuilder stringBuilder = debugInfo.getByKey(ERRORS, candidateCall);
		if(stringBuilder == null)
		{
			stringBuilder = new StringBuilder();
			debugInfo.putByKey(ERRORS, candidateCall, stringBuilder);
		}
		for(Object m : messageFragments)
		{
			stringBuilder.append(m);
		}
		stringBuilder.append("\n");
	}
}
