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

import java.util.Collections;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileAnonymClass;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileEnumValue;
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.resolve.scopes.JetScope;

/**
 * @author Nikolay Krasko
 */
public interface BodiesResolveContext
{
	BodiesResolveContext EMPTY = new BodiesResolveContext()
	{
		@Override
		public Map<NapileClass, MutableClassDescriptor> getClasses()
		{
			return Collections.emptyMap();
		}

		@Override
		public Map<NapileAnonymClass, MutableClassDescriptor> getAnonymous()
		{
			return Collections.emptyMap();
		}

		@Override
		public Map<NapileConstructor, ConstructorDescriptor> getConstructors()
		{
			return Collections.emptyMap();
		}

		@Override
		public Map<NapileVariable, VariableDescriptor> getVariables()
		{
			return Collections.emptyMap();
		}

		@Override
		public Map<NapileEnumValue, MutableClassDescriptor> getEnumValues()
		{
			return Collections.emptyMap();
		}

		@Override
		public Map<NapileNamedMethodOrMacro, SimpleMethodDescriptor> getMethods()
		{
			return Collections.emptyMap();
		}

		@Override
		public Map<NapileDeclaration, JetScope> getDeclaringScopes()
		{
			return Collections.emptyMap();
		}

		@Override
		public void setTopDownAnalysisParameters(TopDownAnalysisParameters parameters)
		{
		}

		@Override
		public boolean completeAnalysisNeeded(@NotNull NapileElement element)
		{
			return false;
		}

	};

	Map<NapileClass, MutableClassDescriptor> getClasses();

	Map<NapileAnonymClass, MutableClassDescriptor> getAnonymous();

	Map<NapileConstructor, ConstructorDescriptor> getConstructors();

	Map<NapileVariable, VariableDescriptor> getVariables();

	Map<NapileEnumValue, MutableClassDescriptor> getEnumValues();

	Map<NapileNamedMethodOrMacro, SimpleMethodDescriptor> getMethods();

	Map<NapileDeclaration, JetScope> getDeclaringScopes();

	void setTopDownAnalysisParameters(TopDownAnalysisParameters parameters);

	boolean completeAnalysisNeeded(@NotNull NapileElement element);
}
