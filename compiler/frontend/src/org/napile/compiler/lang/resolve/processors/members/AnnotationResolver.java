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
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.NapileAnnotation;
import org.napile.compiler.lang.resolve.AnnotationUtils;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.calls.CallMaker;
import org.napile.compiler.lang.resolve.calls.CallResolver;
import org.napile.compiler.lang.resolve.calls.OverloadResolutionResults;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.psi.NapileModifierList;

/**
 * @author abreslav
 */
public class AnnotationResolver
{
	private CallResolver callResolver;

	@Inject
	public void setCallResolver(CallResolver callResolver)
	{
		this.callResolver = callResolver;
	}

	@NotNull
	public List<AnnotationDescriptor> resolveAnnotations(@NotNull JetScope scope, @Nullable NapileModifierList modifierList, BindingTrace trace)
	{
		if(modifierList == null)
			return Collections.emptyList();

		return resolveAnnotations(scope, modifierList.getAnnotations(), trace);
	}

	@NotNull
	public List<AnnotationDescriptor> resolveAnnotations(@NotNull JetScope scope, @NotNull List<NapileAnnotation> annotationEntryElements, BindingTrace trace)
	{
		if(annotationEntryElements.isEmpty())
			return Collections.emptyList();
		List<AnnotationDescriptor> result = new ArrayList<AnnotationDescriptor>(annotationEntryElements.size());
		for(NapileAnnotation entryElement : annotationEntryElements)
		{
			AnnotationDescriptor descriptor = new AnnotationDescriptor();
			resolveAnnotation(scope, entryElement, descriptor, trace);
			trace.record(BindingContext.ANNOTATION, entryElement, descriptor);
			result.add(descriptor);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public void resolveAnnotation(@NotNull JetScope scope, @NotNull NapileAnnotation entryElement, @NotNull AnnotationDescriptor annotationDescriptor, BindingTrace trace)
	{
		OverloadResolutionResults<? extends MethodDescriptor> results = callResolver.resolveFunctionCall(trace, scope, CallMaker.makeCall(ReceiverDescriptor.NO_RECEIVER, null, entryElement), TypeUtils.NO_EXPECTED_TYPE, DataFlowInfo.EMPTY);
		if(results.isSuccess())
		{
			MethodDescriptor descriptor = results.getResultingDescriptor();
			if(!ErrorUtils.isError(descriptor))
			{
				if(descriptor instanceof ConstructorDescriptor)
				{
					ConstructorDescriptor constructor = (ConstructorDescriptor) descriptor;
					ClassifierDescriptor classDescriptor = constructor.getContainingDeclaration();
					if(!AnnotationUtils.isAnnotation(classDescriptor))
						trace.report(Errors.NOT_AN_ANNOTATION_CLASS.on(entryElement, classDescriptor.getName().getName()));
				}
				else
					trace.report(Errors.NOT_AN_ANNOTATION_CLASS.on(entryElement, descriptor.getName().getName()));
			}
		}

		if(results.isSuccess())
		{
			JetType annotationType = results.getResultingDescriptor().getReturnType();
			annotationDescriptor.setAnnotationType(annotationType);
			annotationDescriptor.setResolvedCall((ResolvedCall<ConstructorDescriptor>) results.getResultingCall());
		}
		else
			annotationDescriptor.setAnnotationType(ErrorUtils.createErrorType("Unresolved annotation type"));
	}
}
