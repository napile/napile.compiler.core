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

package org.napile.compiler.lang.resolve.processors.members;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.psi.NapileAnnotation;
import org.napile.compiler.lang.psi.NapileAnnotationOwner;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.NapileModifierListOwner;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTraceUtil;
import org.napile.compiler.lang.resolve.calls.CallMaker;
import org.napile.compiler.lang.resolve.calls.CallResolver;
import org.napile.compiler.lang.resolve.calls.OverloadResolutionResults;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeUtils;

/**
 * @author abreslav
 */
public class AnnotationResolver
{
	@NotNull
	private CallResolver callResolver;

	@Inject
	public void setCallResolver(@NotNull CallResolver callResolver)
	{
		this.callResolver = callResolver;
	}

	@NotNull
	public List<AnnotationDescriptor> bindAnnotations(@NotNull NapileScope scope, @NotNull NapileModifierListOwner modifierListOwner, BindingTrace trace)
	{
		NapileModifierList modifierList = modifierListOwner.getModifierList();
		if(modifierList == null)
			return Collections.emptyList();
		return bindAnnotations(scope, modifierList, trace);
	}

	@NotNull
	public List<AnnotationDescriptor> bindAnnotations(@NotNull NapileScope scope, @NotNull NapileAnnotationOwner annotationOwner, BindingTrace trace)
	{
		List<NapileAnnotation> annotations = annotationOwner.getAnnotations();
		if(annotations.isEmpty())
			return Collections.emptyList();

		final BindingTrace lastTrace = BindingTraceUtil.getLastParent(trace);

		List<AnnotationDescriptor> result = new ArrayList<AnnotationDescriptor>(annotations.size());
		for(NapileAnnotation annotation : annotations)
		{
			AnnotationDescriptor descriptor = new AnnotationDescriptor();

			lastTrace.record(BindingTraceKeys.ANNOTATION, annotation, descriptor);
			lastTrace.record(BindingTraceKeys.ANNOTATION_SCOPE, annotation, scope);

			result.add(descriptor);
		}
		return result;
	}

	public void resolveBindAnnotations(@NotNull final BindingTrace trace)
	{
		Collection<NapileAnnotation> annotations = trace.getKeys(BindingTraceKeys.ANNOTATION_SCOPE);
		for(NapileAnnotation annotation : annotations)
		{
			NapileScope scope = trace.safeGet(BindingTraceKeys.ANNOTATION_SCOPE, annotation);
			AnnotationDescriptor annotationDescriptor = trace.safeGet(BindingTraceKeys.ANNOTATION, annotation);

			resolveAnnotation(scope, annotation, annotationDescriptor, trace);
		}
	}

	@SuppressWarnings("unchecked")
	public void resolveAnnotation(@NotNull NapileScope scope, @NotNull NapileAnnotation annotation, @NotNull AnnotationDescriptor annotationDescriptor, BindingTrace trace)
	{
		if(annotationDescriptor.getType() != null)
		{
			return;
		}

		OverloadResolutionResults<? extends MethodDescriptor> results = callResolver.resolveFunctionCall(trace, scope, CallMaker.makeCall(ReceiverDescriptor.NO_RECEIVER, null, annotation), TypeUtils.NO_EXPECTED_TYPE, DataFlowInfo.EMPTY);
		if(results.isSuccess())
		{
			NapileType annotationType = results.getResultingDescriptor().getReturnType();
			annotationDescriptor.setAnnotationType(annotationType);
			annotationDescriptor.setResolvedCall((ResolvedCall<ConstructorDescriptor>) results.getResultingCall());
		}
		else
			annotationDescriptor.setAnnotationType(ErrorUtils.createErrorType("Unresolved annotation type"));
	}
}
