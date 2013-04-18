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

package org.napile.compiler.lang.resolve.calls.autocasts;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.types.NapileType;

/**
 * @author abreslav
 */
public class DataFlowValue
{
	private final boolean stableIdentifier;
	private final NapileType type;
	private final Object id;
	private final Nullability immanentNullability;

	// Use DataFlowValueFactory
	public DataFlowValue(Object id, NapileType type, boolean stableIdentifier, Nullability immanentNullability)
	{
		this.stableIdentifier = stableIdentifier;
		this.type = type;
		this.id = id;
		this.immanentNullability = immanentNullability;
	}

	@NotNull
	public Nullability getImmanentNullability()
	{
		return immanentNullability;
	}

	/**
	 * Stable identifier is a non-literal value that is statically known to be immutable
	 *
	 * @return
	 */
	public boolean isStableIdentifier()
	{
		return stableIdentifier;
	}

	@NotNull
	public NapileType getType()
	{
		return type;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;
		if(o == null || getClass() != o.getClass())
			return false;

		DataFlowValue that = (DataFlowValue) o;

		if(stableIdentifier != that.stableIdentifier)
			return false;
		if(id != null ? !id.equals(that.id) : that.id != null)
			return false;
		if(type != null ? !type.equals(that.type) : that.type != null)
			return false;

		return true;
	}

	@Override
	public String toString()
	{
		return (stableIdentifier ? "stable " : "unstable ") + (id == null ? null : id.toString()) + " " + immanentNullability;
	}

	@Override
	public int hashCode()
	{
		int result = (stableIdentifier ? 1 : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (id != null ? id.hashCode() : 0);
		return result;
	}
}
