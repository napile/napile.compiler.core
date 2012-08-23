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

package org.napile.compiler.lang.resolve;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.SimpleFunctionDescriptor;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileNamedFunction;
import org.napile.compiler.lang.psi.NapileObjectDeclaration;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.resolve.scopes.JetScope;

/**
 * @author Nikolay Krasko
 */
public interface BodiesResolveContext
{
	Map<NapileClass, MutableClassDescriptor> getClasses();

	Map<NapileConstructor, ConstructorDescriptor> getConstructors();

	Map<NapileObjectDeclaration, MutableClassDescriptor> getObjects();

	Map<NapileProperty, PropertyDescriptor> getProperties();

	Map<NapileNamedFunction, SimpleFunctionDescriptor> getFunctions();

	Map<NapileDeclaration, JetScope> getDeclaringScopes();

	void setTopDownAnalysisParameters(TopDownAnalysisParameters parameters);

	boolean completeAnalysisNeeded(@NotNull NapileElement element);
}
