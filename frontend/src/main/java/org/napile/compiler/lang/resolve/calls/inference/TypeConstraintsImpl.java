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

package org.napile.compiler.lang.resolve.calls.inference;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.types.NapileType;
import com.google.common.collect.Sets;

/**
 * @author svtk
 */
public class TypeConstraintsImpl implements TypeConstraints
{
	private final Set<NapileType> upperBounds = Sets.newLinkedHashSet();

	public TypeConstraintsImpl()
	{

	}

	public void addBound(@NotNull NapileType type)
	{
		upperBounds.add(type);
	}

	@Override
	public boolean isEmpty()
	{
		return upperBounds.isEmpty();
	}


	@NotNull
	@Override
	public Set<NapileType> getUpperBounds()
	{
		return upperBounds;
	}


	/*package*/ TypeConstraintsImpl copy()
	{
		TypeConstraintsImpl typeConstraints = new TypeConstraintsImpl();
		for(NapileType upperBound : upperBounds)
		{
			typeConstraints.upperBounds.add(upperBound);
		}
		return typeConstraints;
	}
}
