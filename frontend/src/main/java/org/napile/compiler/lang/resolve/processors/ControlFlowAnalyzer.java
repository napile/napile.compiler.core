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
import org.napile.compiler.lang.cfg.NapileFlowInformationProvider;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableAccessorDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.lang.resolve.TopDownAnalysisParameters;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.expressions.VariableAccessorResolver;

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
			checkClassLike(aClass);
		}

		for(NapileAnonymClass objectDeclaration : bodiesResolveContext.getAnonymous().keySet())
		{
			if(!bodiesResolveContext.completeAnalysisNeeded(objectDeclaration))
				continue;
			checkClassLike(objectDeclaration);
		}

		for(Map.Entry<NapileNamedMethodOrMacro, SimpleMethodDescriptor> entry : bodiesResolveContext.getMethods().entrySet())
		{
			NapileNamedMethodOrMacro function = entry.getKey();
			SimpleMethodDescriptor functionDescriptor = entry.getValue();
			if(!bodiesResolveContext.completeAnalysisNeeded(function))
				continue;
			final NapileType expectedReturnType = !function.hasBlockBody() && !function.hasDeclaredReturnType() ? TypeUtils.NO_EXPECTED_TYPE : functionDescriptor.getReturnType();
			checkMethod(function, expectedReturnType);
		}

		for(Map.Entry<NapileConstructor, ConstructorDescriptor> entry : bodiesResolveContext.getConstructors().entrySet())
		{
			NapileConstructor constructor = entry.getKey();
			if(!bodiesResolveContext.completeAnalysisNeeded(constructor))
				continue;

			checkMethod(constructor, TypeUtils.NO_EXPECTED_TYPE);
		}

		for(Map.Entry<NapileVariable, VariableDescriptor> entry : bodiesResolveContext.getVariables().entrySet())
		{
			NapileVariable variable = entry.getKey();
			if(!bodiesResolveContext.completeAnalysisNeeded(variable))
				continue;

			//VariableDescriptor variableDescriptor = entry.getValue();

			for(NapileVariableAccessor accessor : variable.getAccessors())
			{
				final VariableAccessorDescriptor descriptor = trace.get(VariableAccessorResolver.getSliceForAccessor(accessor), accessor);
				if(descriptor == null || accessor.getBodyExpression() == null)
				{
					continue;
				}

				checkMethod(accessor, descriptor.getReturnType());
			}
		}
	}

	private void checkClassLike(NapileClassLike klass)
	{
		// A pseudocode of class initialization corresponds to a class
		NapileFlowInformationProvider flowInformationProvider = new NapileFlowInformationProvider((NapileDeclaration) klass, trace);
		flowInformationProvider.markUninitializedVariables(topDownAnalysisParameters.isDeclaredLocally());
	}

	private void checkMethod(NapileDeclarationWithBody function, final @NotNull NapileType expectedReturnType)
	{
		NapileFlowInformationProvider flowInformationProvider = new NapileFlowInformationProvider(function, trace);

		flowInformationProvider.checkMethodReferenceParameters();

		NapileExpression bodyExpression = function.getBodyExpression();
		if(bodyExpression == null)
			return;

		flowInformationProvider.checkDefiniteReturn(expectedReturnType);

		// Property accessor is checked through initialization of a class check (at 'checkClassOrObject')
		flowInformationProvider.markUninitializedVariables(topDownAnalysisParameters.isDeclaredLocally());

		flowInformationProvider.markUnusedVariables();

		flowInformationProvider.markUnusedLiteralsInBlock();
	}
}
