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

package org.napile.compiler.lang.resolve.processors.checkers;

import java.util.Collection;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.lib.NapileAnnotationPackage;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.annotations.Annotated;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.NapileAnnotation;
import org.napile.compiler.lang.resolve.AnnotationUtils;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.NapileType;

/**
 * @author VISTALL
 * @since 15:27/26.08.12
 */
public class AnnotationChecker
{
	private BindingTrace bindingTrace;

	@Inject
	public void setBindingTrace(@NotNull BindingTrace bindingTrace)
	{
		this.bindingTrace = bindingTrace;
	}

	public void process()
	{
		Collection<NapileAnnotation> annotations = bindingTrace.getKeys(BindingTraceKeys.ANNOTATION_SCOPE);
		for(NapileAnnotation annotation : annotations)
		{
			AnnotationDescriptor annotationDescriptor = bindingTrace.safeGet(BindingTraceKeys.ANNOTATION, annotation);

			checkIsAnnotationClass(annotation, annotationDescriptor);

			checkRepeatable(annotation, annotationDescriptor);

			checkExtension(annotation, annotationDescriptor);
		}
	}

	private void checkIsAnnotationClass(@NotNull NapileAnnotation annotation, @NotNull AnnotationDescriptor annotationDescriptor)
	{
		ResolvedCall<ConstructorDescriptor> resolvedCall = annotationDescriptor.getResolvedCall();
		if(resolvedCall == null)
			return;

		MethodDescriptor descriptor = resolvedCall.getResultingDescriptor();
		if(!ErrorUtils.isError(descriptor))
		{
			ConstructorDescriptor constructor = (ConstructorDescriptor) descriptor;
			ClassifierDescriptor classDescriptor = constructor.getContainingDeclaration();
			if(!AnnotationUtils.isAnnotation(classDescriptor))
				bindingTrace.report(Errors.NOT_AN_ANNOTATION_CLASS.on(annotation, classDescriptor.getName().getName()));
		}
	}

	private void checkRepeatable(@NotNull NapileAnnotation annotation, @NotNull AnnotationDescriptor annotationDescriptor)
	{
		NapileType type = annotationDescriptor.getType();

		ClassDescriptor classDescriptor = (ClassDescriptor) type.getConstructor().getDeclarationDescriptor();

		if(AnnotationUtils.hasAnnotation(classDescriptor, NapileAnnotationPackage.REPEATABLE))
			return;

		Annotated owner = annotationDescriptor.getOwner();
		if(owner == null)
			return;
		for(AnnotationDescriptor otherAnnotation : owner.getAnnotations())
		{
			if(otherAnnotation == annotationDescriptor)
				continue;

			if(otherAnnotation.getType().equals(annotationDescriptor.getType()))
			{
				bindingTrace.report(Errors.DUPLICATE_ANNOTATION.on(annotation));
				break;
			}
		}
	}

	private void checkExtension(NapileAnnotation annotation, AnnotationDescriptor annotationDescriptor)
	{
		Annotated owner = annotationDescriptor.getOwner();
		if(owner == null)
			return;

		if(!AnnotationUtils.hasAnnotation(owner, NapileAnnotationPackage.EXTENSION))
			return;

		if(owner instanceof MethodDescriptor && ((MethodDescriptor) owner).isStatic() && !((MethodDescriptor) owner).getValueParameters().isEmpty())
			return;

		bindingTrace.report(Errors.NONE_APPLICABLE_ANNOTATION.on(annotation));
	}
}
