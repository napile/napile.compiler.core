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

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.annotations.AnnotatedImpl;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeConstructorVisitor;
import org.napile.compiler.lang.types.checker.NapileTypeChecker;
import org.napile.compiler.render.DescriptorRenderer;

/**
 * @author abreslav
 */
public final class NapileTypeImpl extends AnnotatedImpl implements NapileType
{
	private final TypeConstructor constructor;
	private final List<NapileType> arguments;
	private final boolean nullable;
	private final NapileScope memberScope;

	public NapileTypeImpl(List<AnnotationDescriptor> annotations, TypeConstructor constructor, boolean nullable, @NotNull List<NapileType> arguments, NapileScope memberScope)
	{
		super(annotations);

		if(memberScope instanceof ErrorUtils.ErrorScope)
		{
			throw new IllegalStateException();
		}

		this.constructor = constructor;
		this.nullable = nullable;
		this.arguments = arguments;
		this.memberScope = memberScope;
	}

	public NapileTypeImpl(TypeConstructor constructor, NapileScope memberScope)
	{
		this(Collections.<AnnotationDescriptor>emptyList(), constructor, false, Collections.<NapileType>emptyList(), memberScope);
	}

	public NapileTypeImpl(@NotNull ClassDescriptor classDescriptor)
	{
		this(Collections.<AnnotationDescriptor>emptyList(), classDescriptor.getTypeConstructor(), false, Collections.<NapileType>emptyList(), classDescriptor.getMemberScope(Collections.<NapileType>emptyList()));
	}

	@NotNull
	@Override
	public TypeConstructor getConstructor()
	{
		return constructor;
	}

	@NotNull
	@Override
	public List<NapileType> getArguments()
	{
		return arguments;
	}

	@Override
	public boolean isNullable()
	{
		return nullable;
	}

	@NotNull
	@Override
	public NapileScope getMemberScope()
	{
		if(memberScope == null)
		{
			// TODO : this was supposed to mean something...
			throw new IllegalStateException(this.toString());
		}
		return memberScope;
	}

	@Override
	public String toString()
	{
		return DescriptorRenderer.TEXT.renderType(this);
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;
		if(o == null || getClass() != o.getClass())
			return false;

		NapileTypeImpl type = (NapileTypeImpl) o;

		return NapileTypeChecker.INSTANCE.equalTypes(this, type);
	}

	@Override
	public <A, R> R accept(@NotNull TypeConstructorVisitor<A, R> visitor, A arg)
	{
		return getConstructor().accept(this, visitor, arg);
	}

	@Override
	public int hashCode()
	{
		int result = constructor != null ? constructor.hashCode() : 0;
		result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
		result = 31 * result + (nullable ? 1 : 0);
		return result;
	}
}
