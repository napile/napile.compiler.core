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

package org.jetbrains.jet.lang.types.lang.rt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

/**
 * @author VISTALL
 * @date 13:33/12.08.12
 */
public class TypedFqName extends FqName
{
	public TypedFqName(@NotNull String fqName)
	{
		super(fqName);
	}

	private TypedFqName(@NotNull FqNameUnsafe fqName, FqName parent)
	{
		super(fqName, parent);
	}

	@Override
	@NotNull
	public TypedFqName child(@NotNull Name name)
	{
		return new TypedFqName(fqName.child(name), this);
	}

	@NotNull
	public JetType getTypeSafe(@NotNull JetScope jetScope, boolean nullable)
	{
		return TypeUtils.getTypeOfClassOrErrorType(jetScope, this, nullable);
	}
}
