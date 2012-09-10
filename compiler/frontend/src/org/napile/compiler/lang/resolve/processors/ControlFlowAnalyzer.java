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

package org.napile.compiler.lang.resolve.processors;

import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.cfg.JetFlowInformationProvider;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.PropertyAccessorDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.lang.resolve.TopDownAnalysisParameters;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeUtils;

/**
 * @author svtk
 */
public class ControlFlowAnalyzer
{
	private TopDownAnalysisParameters topDownAnalysisParameters;
	private BindingTrace trace;

	@Inject
	public void setTopDownAnalysisParameters(TopDownAnalysisParameters topDownAnalysisParameters)
	{
		this.topDownAnalysisParameters = topDownAnalysisParameters;
	}

	@Inject
	public void setTrace(BindingTrace trace)
	{
		this.trace = trace;
	}

	public void process(@NotNull BodiesResolveContext bodiesResolveContext)
	{
		for(NapileClass aClass : bodiesResolveContext.getClasses().keySet())
		{
			if(!bodiesResolveContext.completeAnalysisNeeded(aClass))
				continue;
			checkClassOrObject(aClass);
		}

		for(NapileAnonymClass objectDeclaration : bodiesResolveContext.getAnonymous().keySet())
		{
			if(!bodiesResolveContext.completeAnalysisNeeded(objectDeclaration))
				continue;
			checkClassOrObject(objectDeclaration);
		}

		for(Map.Entry<NapileNamedFunction, SimpleMethodDescriptor> entry : bodiesResolveContext.getMethods().entrySet())
		{
			NapileNamedFunction function = entry.getKey();
			SimpleMethodDescriptor functionDescriptor = entry.getValue();
			if(!bodiesResolveContext.completeAnalysisNeeded(function))
				continue;
			final JetType expectedReturnType = !function.hasBlockBody() && !function.hasDeclaredReturnType() ? TypeUtils.NO_EXPECTED_TYPE : functionDescriptor.getReturnType();
			checkFunction(function, expectedReturnType);
		}

		for(Map.Entry<NapileConstructor, ConstructorDescriptor> entry : bodiesResolveContext.getConstructors().entrySet())
		{
			NapileConstructor constructor = entry.getKey();
			if(!bodiesResolveContext.completeAnalysisNeeded(constructor))
				continue;

			checkFunction(constructor, TypeUtils.NO_EXPECTED_TYPE);
		}

		for(Map.Entry<NapileProperty, PropertyDescriptor> entry : bodiesResolveContext.getProperties().entrySet())
		{
			NapileProperty property = entry.getKey();
			if(!bodiesResolveContext.completeAnalysisNeeded(property))
				continue;
			PropertyDescriptor propertyDescriptor = entry.getValue();
			checkProperty(property, propertyDescriptor);
		}
	}

	private void checkClassOrObject(NapileLikeClass klass)
	{
		// A pseudocode of class initialization corresponds to a class
		JetFlowInformationProvider flowInformationProvider = new JetFlowInformationProvider((NapileDeclaration) klass, trace);
		flowInformationProvider.markUninitializedVariables(topDownAnalysisParameters.isDeclaredLocally());
	}

	private void checkProperty(NapileProperty property, PropertyDescriptor propertyDescriptor)
	{
		for(NapilePropertyAccessor accessor : property.getAccessors())
		{
			PropertyAccessorDescriptor accessorDescriptor = accessor.isGetter() ? propertyDescriptor.getGetter() : propertyDescriptor.getSetter();
			assert accessorDescriptor != null;
			checkFunction(accessor, accessorDescriptor.getReturnType());
		}
	}

	private void checkFunction(NapileDeclarationWithBody function, final @NotNull JetType expectedReturnType)
	{
		NapileExpression bodyExpression = function.getBodyExpression();
		if(bodyExpression == null)
			return;
		JetFlowInformationProvider flowInformationProvider = new JetFlowInformationProvider((NapileDeclaration) function, trace);

		flowInformationProvider.checkDefiniteReturn(expectedReturnType);

		// Property accessor is checked through initialization of a class check (at 'checkClassOrObject')
		boolean isPropertyAccessor = function instanceof NapilePropertyAccessor;
		flowInformationProvider.markUninitializedVariables(topDownAnalysisParameters.isDeclaredLocally() || isPropertyAccessor);

		flowInformationProvider.markUnusedVariables();

		flowInformationProvider.checkMethodReferenceParameters();

		flowInformationProvider.markUnusedLiteralsInBlock();
	}
}
