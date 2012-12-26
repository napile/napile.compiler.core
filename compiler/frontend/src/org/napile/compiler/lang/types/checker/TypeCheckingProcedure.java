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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeSubstitutor;
import org.napile.compiler.lang.types.TypeUtils;

/**
 * @author abreslav
 */
public class TypeCheckingProcedure
{

	// This method returns the supertype of the first parameter that has the same constructor
	// as the second parameter, applying the substitution of type arguments to it
	@Nullable
	public static JetType findCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype)
	{
		TypeConstructor constructor = subtype.getConstructor();
		if(constructor.equals(supertype.getConstructor()))
		{
			return subtype;
		}
		for(JetType immediateSupertype : constructor.getSupertypes())
		{
			JetType correspondingSupertype = findCorrespondingSupertype(immediateSupertype, supertype);
			if(correspondingSupertype != null)
			{
				return TypeSubstitutor.create(subtype).safeSubstitute(correspondingSupertype);
			}
		}
		return null;
	}

	private final TypingConstraints constraints;

	public TypeCheckingProcedure(TypingConstraints constraints)
	{
		this.constraints = constraints;
	}

	public boolean equalTypes(@NotNull JetType type1, @NotNull JetType type2)
	{
		if(type1.isNullable() != type2.isNullable())
		{
			return false;
		}

		if(type1.isNullable())
		{
			// Then type2 is nullable, too (see the previous condition
			return constraints.assertEqualTypes(TypeUtils.makeNotNullable(type1), TypeUtils.makeNotNullable(type2), this);
		}

		TypeConstructor constructor1 = type1.getConstructor();
		TypeConstructor constructor2 = type2.getConstructor();

		if(!constraints.assertEqualTypeConstructors(constructor1, constructor2))
		{
			return false;
		}

		List<JetType> type1Arguments = type1.getArguments();
		List<JetType> type2Arguments = type2.getArguments();
		if(type1Arguments.size() != type2Arguments.size())
		{
			return false;
		}

		for(int i = 0; i < type1Arguments.size(); i++)
		{
			JetType typeProjection1 = type1Arguments.get(i);
			JetType typeProjection2 = type2Arguments.get(i);

			if(!constraints.assertEqualTypes(typeProjection1, typeProjection2, this))
			{
				return false;
			}
		}
		return true;
	}


	public boolean isSubtypeOf(@NotNull JetType subtype, @NotNull JetType supertype)
	{
		if(ErrorUtils.isErrorType(subtype) || ErrorUtils.isErrorType(supertype))
			return true;

		if(!supertype.isNullable() && subtype.isNullable())
			return false;

		subtype = TypeUtils.makeNotNullable(subtype);
		supertype = TypeUtils.makeNotNullable(supertype);
		if(TypeUtils.isEqualFqName(subtype, NapileLangPackage.NULL))
			return true;

		@Nullable JetType closestSupertype = findCorrespondingSupertype(subtype, supertype);
		if(closestSupertype == null)
			return constraints.noCorrespondingSupertype(subtype, supertype); // if this returns true, there still isn't any supertype to continue with

		return checkSubtypeForTheSameConstructor(closestSupertype, supertype);
	}

	private boolean checkSubtypeForTheSameConstructor(@NotNull JetType subtype, @NotNull JetType supertype)
	{
		TypeConstructor constructor = subtype.getConstructor();
		assert constructor.equals(supertype.getConstructor()) : constructor + " is not " + supertype.getConstructor();

		List<JetType> subArguments = subtype.getArguments();
		List<JetType> superArguments = supertype.getArguments();
		List<TypeParameterDescriptor> parameters = constructor.getParameters();
		for(int i = 0; i < parameters.size(); i++)
		{
			JetType subArgument = subArguments.get(i);

			JetType superArgument = superArguments.get(i);

			if(superArgument.isNullable() && !subArgument.isNullable())
				return false;
			if(!isSubtypeOf(subArgument, superArgument))
				return false;
		}
		return true;
	}
}
