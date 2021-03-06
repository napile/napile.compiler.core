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

package org.napile.compiler.lang.resolve.constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeUtils;

/**
 * @author abreslav
 */
public class StringValue implements CompileTimeConstant<String>
{

	private final String value;

	public StringValue(String value)
	{
		this.value = value;
	}

	@Override
	public String getValue()
	{
		return value;
	}

	@Nullable
	@Override
	public NapileType getType(@NotNull NapileScope napileScope)
	{
		return TypeUtils.getTypeOfClassOrErrorType(napileScope, NapileLangPackage.STRING, false);
	}

	@Override
	public String toString()
	{
		return "\'" + value + "\'";
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;
		if(o == null || getClass() != o.getClass())
			return false;

		StringValue that = (StringValue) o;

		if(value != null ? !value.equals(that.value) : that.value != null)
			return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		return value != null ? value.hashCode() : 0;
	}
}
