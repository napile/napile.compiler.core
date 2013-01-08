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

import org.jetbrains.annotations.NotNull;
import org.napile.asm.lib.NapileAnnotationPackage;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.annotations.Annotated;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.NapileAnnotation;
import org.napile.compiler.lang.resolve.AnnotationUtils;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.types.JetType;

/**
 * @author VISTALL
 * @date 15:27/26.08.12
 */
public class AnnotationChecker
{
	public void process(@NotNull Collection<NapileAnnotation> annotations, @NotNull BindingTrace trace)
	{
		for(NapileAnnotation annotation : annotations)
		{
			AnnotationDescriptor annotationDescriptor = trace.safeGet(BindingContext.ANNOTATION, annotation);

			checkRepeatable(annotation, annotationDescriptor, trace);
		}
	}

	private void checkRepeatable(@NotNull NapileAnnotation annotation, @NotNull AnnotationDescriptor annotationDescriptor, @NotNull BindingTrace trace)
	{
		JetType type = annotationDescriptor.getType();

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
				trace.report(Errors.DUPLICATE_ANNOTATION.on(annotation));
				break;
			}
		}
	}
}
