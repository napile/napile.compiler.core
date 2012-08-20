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

package org.napile.compiler.lang.resolve.calls.inference;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.types.JetType;
import com.google.common.collect.Sets;

/**
 * @author abreslav
 */
public class TypeValue implements BoundsOwner
{
	private final Set<TypeValue> upperBounds = Sets.newLinkedHashSet();
	private final Set<TypeValue> lowerBounds = Sets.newLinkedHashSet();

	private final TypeParameterDescriptor typeParameterDescriptor; // Null for known types
	private final JetType originalType;
	private JetType value; // For an unknown - the value found by constraint resolution, for a known - just it's value

	// Unknown type
	public TypeValue(@NotNull TypeParameterDescriptor typeParameterDescriptor)
	{
		this.typeParameterDescriptor = typeParameterDescriptor;
		this.originalType = typeParameterDescriptor.getDefaultType();
	}

	// Known type
	public TypeValue(@NotNull JetType knownType)
	{
		this.typeParameterDescriptor = null;
		this.originalType = knownType;
		this.value = knownType;
	}

	public boolean isKnown()
	{
		return typeParameterDescriptor == null;
	}

	public TypeParameterDescriptor getTypeParameterDescriptor()
	{
		return typeParameterDescriptor;
	}

	@Override
	@NotNull
	public Set<TypeValue> getUpperBounds()
	{
		return upperBounds;
	}

	@Override
	@NotNull
	public Set<TypeValue> getLowerBounds()
	{
		return lowerBounds;
	}

	@NotNull
	public JetType getType()
	{
		return value;
	}

	@NotNull
	public JetType getOriginalType()
	{
		return originalType;
	}

	public void addUpperBound(@NotNull TypeValue bound)
	{
		upperBounds.add(bound);
	}

	public void addLowerBound(@NotNull TypeValue bound)
	{
		lowerBounds.add(bound);
	}

	public void setValue(@NotNull JetType value)
	{
		this.value = value;
	}

	public boolean hasValue()
	{
		return value != null;
	}

	@Override
	public String toString()
	{
		return isKnown() ? getType().toString() : (getTypeParameterDescriptor() + (hasValue() ? " |-> " + getType() : ""));
	}
}
