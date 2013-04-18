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

package org.napile.compiler.lang.types.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.annotations.AnnotatedImpl;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeConstructorVisitor;

/**
 * @author abreslav
 */
public class TypeConstructorImpl extends AnnotatedImpl implements TypeConstructor
{
	private final List<TypeParameterDescriptor> parameters;
	private final Collection<NapileType> supertypes;
	private final String debugName;
	private final boolean sealed;

	@Nullable
	private final ClassifierDescriptor classifierDescriptor;

	public TypeConstructorImpl(@Nullable ClassifierDescriptor classifierDescriptor, @NotNull List<AnnotationDescriptor> annotations, boolean sealed, @NotNull String debugName, @NotNull List<? extends TypeParameterDescriptor> parameters, @NotNull Collection<NapileType> supertypes)
	{
		super(annotations);
		this.classifierDescriptor = classifierDescriptor;
		this.sealed = sealed;
		this.debugName = debugName;
		this.parameters = Collections.unmodifiableList(new ArrayList<TypeParameterDescriptor>(parameters));
		this.supertypes = Collections.unmodifiableCollection(supertypes);
	}

	@Override
	@NotNull
	public List<TypeParameterDescriptor> getParameters()
	{
		return parameters;
	}

	@Override
	@NotNull
	public Collection<NapileType> getSupertypes()
	{
		return supertypes;
	}

	@Override
	public String toString()
	{
		return debugName;
	}

	@Override
	public boolean isSealed()
	{
		return sealed;
	}

	@Override
	@Nullable
	public ClassifierDescriptor getDeclarationDescriptor()
	{
		return classifierDescriptor;
	}

	@Override
	public <A, R> R accept(NapileType type, TypeConstructorVisitor<A, R> visitor, A arg)
	{
		return visitor.visitType(type, this, arg);
	}
}
