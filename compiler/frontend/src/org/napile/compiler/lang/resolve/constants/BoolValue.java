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
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeUtils;

/**
 * @author abreslav
 */
public class BoolValue implements CompileTimeConstant<Boolean>
{
	public static final BoolValue FALSE = new BoolValue(false);
	public static final BoolValue TRUE = new BoolValue(true);

	private final boolean value;

	private BoolValue(boolean value)
	{
		this.value = value;
	}

	@Override
	public Boolean getValue()
	{
		return value;
	}

	@Nullable
	@Override
	public JetType getType(@NotNull JetScope jetScope)
	{
		return TypeUtils.getTypeOfClassOrErrorType(jetScope, NapileLangPackage.BOOL, false);
	}

	@Override
	public String toString()
	{
		return String.valueOf(value);
	}
}
