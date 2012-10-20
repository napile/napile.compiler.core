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

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.lib.NapileAnnotationPackage;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.resolve.AnnotationUtils;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BodiesResolveContext;

/**
 * @author VISTALL
 * @date 15:27/26.08.12
 */
public class AnnotationChecker
{
	@NotNull
	private BindingTrace trace;

	@Inject
	public void setTrace(@NotNull BindingTrace trace)
	{
		this.trace = trace;
	}

	public void processFirst(@NotNull BodiesResolveContext bodiesResolveContext)
	{
		for(MutableClassDescriptor descriptor : bodiesResolveContext.getClasses().values())
			checkForDeprecation(descriptor);

		for(PropertyDescriptor descriptor : bodiesResolveContext.getProperties().values())
			checkForDeprecation(descriptor);

		for(SimpleMethodDescriptor descriptor : bodiesResolveContext.getMethods().values())
			checkForDeprecation(descriptor);
	}

	private void checkForDeprecation(@NotNull DeclarationDescriptor declarationDescriptor)
	{
		boolean deprecated = AnnotationUtils.hasAnnotation(declarationDescriptor, NapileAnnotationPackage.DEPRECATED);
		if(deprecated)
			trace.record(BindingContext.DEPRECATED, declarationDescriptor, Boolean.valueOf(deprecated));
	}

	public void processLater(@NotNull BodiesResolveContext bodiesResolveContext)
	{
		/*Collection<NapileReferenceExpression> refs = trace.getKeys(BindingContext.REFERENCE_TARGET);
		for(NapileReferenceExpression ref : refs)
		{
			DeclarationDescriptor declarationDescriptor = trace.safeGet(BindingContext.REFERENCE_TARGET, ref);

			if(trace.safeGet(BindingContext.DEPRECATED, declarationDescriptor))
				trace.report(Errors.TARGET_IS_DEPRECATED.on(ref, "Not Done")); //TODO [VISTALL] message
		} */
	}
}
