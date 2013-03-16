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

package org.napile.compiler.lang.types.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeConstructor;

/**
 * @author VISTALL
 * @since 21:48/26.12.12
 */
public abstract class AbstractTypeConstructorImpl implements TypeConstructor
{
	private final Collection<JetType> superTypes;

	protected AbstractTypeConstructorImpl(JetScope scope, FqName baseClass)
	{
		ClassDescriptor classDescriptor = scope.getClass(baseClass);
		superTypes = classDescriptor != null ? Collections.singletonList(classDescriptor.getDefaultType()) : Collections.<JetType>emptyList();
	}

	protected AbstractTypeConstructorImpl(Collection<JetType> superTypes)
	{
		this.superTypes = superTypes;
	}

	@Override
	public ClassifierDescriptor getDeclarationDescriptor()
	{
		return null;
	}

	@NotNull
	@Override
	public List<TypeParameterDescriptor> getParameters()
	{
		return Collections.emptyList();
	}

	@Override
	public boolean isSealed()
	{
		return false;
	}

	@Override
	public List<AnnotationDescriptor> getAnnotations()
	{
		return Collections.emptyList();
	}

	@Override
	@NotNull
	public final Collection<? extends JetType> getSupertypes()
	{
		return superTypes;
	}
}
