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

package org.jetbrains.jet.lang.resolve;

import java.util.Collections;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.MutableClassDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.NapileConstructor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

/**
 * A storage for the part of {@see TopDownAnalysisContext} collected during headers analysis that will be used during resolution of
 * bodies
 *
 * @author Nikolay Krasko
 */
public class CachedBodiesResolveContext implements BodiesResolveContext
{
	private final Map<JetClass, MutableClassDescriptor> classes;
	private final Map<JetObjectDeclaration, MutableClassDescriptor> objects;
	private final Map<NapileConstructor, ConstructorDescriptor> constructors;
	private final Map<JetProperty, PropertyDescriptor> properties;
	private final Map<JetNamedFunction, SimpleFunctionDescriptor> functions;
	private final Map<JetDeclaration, JetScope> declaringScopes;

	private
	@NotNull
	TopDownAnalysisParameters topDownAnalysisParameters;

	public CachedBodiesResolveContext(TopDownAnalysisContext context)
	{
		classes = Collections.unmodifiableMap(context.getClasses());
		objects = Collections.unmodifiableMap(context.getObjects());
		constructors = Collections.unmodifiableMap(context.getConstructors());
		properties = Collections.unmodifiableMap(context.getProperties());
		functions = Collections.unmodifiableMap(context.getFunctions());
		declaringScopes = Collections.unmodifiableMap(context.getDeclaringScopes());

		topDownAnalysisParameters = context.getTopDownAnalysisParameters();
	}

	@Override
	public Map<JetClass, MutableClassDescriptor> getClasses()
	{
		return classes;
	}

	@Override
	public Map<JetObjectDeclaration, MutableClassDescriptor> getObjects()
	{
		return objects;
	}

	@Override
	public Map<NapileConstructor, ConstructorDescriptor> getConstructors()
	{
		return constructors;
	}

	@Override
	public Map<JetProperty, PropertyDescriptor> getProperties()
	{
		return properties;
	}

	@Override
	public Map<JetNamedFunction, SimpleFunctionDescriptor> getFunctions()
	{
		return functions;
	}

	@Override
	public Map<JetDeclaration, JetScope> getDeclaringScopes()
	{
		return declaringScopes;
	}

	@Override
	public void setTopDownAnalysisParameters(@NotNull TopDownAnalysisParameters parameters)
	{
		topDownAnalysisParameters = parameters;
	}

	@Override
	public boolean completeAnalysisNeeded(@NotNull PsiElement element)
	{
		PsiFile containingFile = element.getContainingFile();
		return containingFile != null && topDownAnalysisParameters.getAnalyzeCompletely().apply(containingFile);
	}
}
