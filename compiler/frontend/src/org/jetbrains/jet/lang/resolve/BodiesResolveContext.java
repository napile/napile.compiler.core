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

/**
 * @author Nikolay Krasko
 */
public interface BodiesResolveContext
{
	Map<JetClass, MutableClassDescriptor> getClasses();

	Map<NapileConstructor, ConstructorDescriptor> getConstructors();

	Map<JetObjectDeclaration, MutableClassDescriptor> getObjects();

	Map<JetProperty, PropertyDescriptor> getProperties();

	Map<JetNamedFunction, SimpleFunctionDescriptor> getFunctions();

	Map<JetDeclaration, JetScope> getDeclaringScopes();

	void setTopDownAnalysisParameters(TopDownAnalysisParameters parameters);

	boolean completeAnalysisNeeded(@NotNull PsiElement element);
}
