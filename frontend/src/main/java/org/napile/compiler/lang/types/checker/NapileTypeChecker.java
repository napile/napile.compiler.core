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

package org.napile.compiler.lang.types.checker;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeConstructor;
import com.google.common.collect.BiMap;

/**
 * @author abreslav
 */
public class NapileTypeChecker
{
	public static final NapileTypeChecker INSTANCE = new NapileTypeChecker();

	private NapileTypeChecker()
	{
	}

	public boolean isSubtypeOf(@NotNull NapileType subtype, @NotNull NapileType supertype)
	{
		return TYPE_CHECKER.isSubtypeOf(subtype, supertype);
	}

	public boolean equalTypes(@NotNull NapileType a, @NotNull NapileType b)
	{
		return TYPE_CHECKER.equalTypes(a, b);
	}

	public boolean equalTypes(@NotNull NapileType a, @NotNull NapileType b, @NotNull final BiMap<TypeConstructor, TypeConstructor> equalityAxioms)
	{
		return new TypeCheckingProcedure(new TypeCheckerTypingConstraints()
		{
			@Override
			public boolean assertEqualTypeConstructors(@NotNull TypeConstructor constructor1, @NotNull TypeConstructor constructor2)
			{
				if(!constructor1.equals(constructor2))
				{
					TypeConstructor img1 = equalityAxioms.get(constructor1);
					TypeConstructor img2 = equalityAxioms.get(constructor2);
					if(!(img1 != null && img1.equals(constructor2)) && !(img2 != null && img2.equals(constructor1)))
					{
						return false;
					}
				}
				return true;
			}
		}).equalTypes(a, b);
	}

	private static final TypeCheckingProcedure TYPE_CHECKER = new TypeCheckingProcedure(new TypeCheckerTypingConstraints());

	private static class TypeCheckerTypingConstraints implements TypingConstraints
	{
		@Override
		public boolean assertEqualTypes(@NotNull NapileType a, @NotNull NapileType b, @NotNull TypeCheckingProcedure typeCheckingProcedure)
		{
			return typeCheckingProcedure.equalTypes(a, b);
			//            return TypeUtils.equalTypes(a, b);
		}

		@Override
		public boolean assertEqualTypeConstructors(@NotNull TypeConstructor a, @NotNull TypeConstructor b)
		{
			return a.equals(b);
		}

		@Override
		public boolean assertSubtype(@NotNull NapileType subtype, @NotNull NapileType supertype, @NotNull TypeCheckingProcedure typeCheckingProcedure)
		{
			return typeCheckingProcedure.isSubtypeOf(subtype, supertype);
		}

		@Override
		public boolean noCorrespondingSupertype(@NotNull NapileType subtype, @NotNull NapileType supertype)
		{
			return false; // type checking fails
		}
	}
}
