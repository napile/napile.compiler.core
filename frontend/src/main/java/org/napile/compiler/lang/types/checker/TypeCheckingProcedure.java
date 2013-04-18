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
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.NapileType;
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
	public static NapileType findCorrespondingSupertype(@NotNull NapileType subtype, @NotNull NapileType supertype)
	{
		TypeConstructor constructor = subtype.getConstructor();
		if(constructor.equals(supertype.getConstructor()))
		{
			return subtype;
		}
		for(NapileType immediateSupertype : constructor.getSupertypes())
		{
			NapileType correspondingSupertype = findCorrespondingSupertype(immediateSupertype, supertype);
			if(correspondingSupertype != null)
			{
				return TypeSubstitutor.create(subtype).safeSubstitute(correspondingSupertype);
			}
		}
		return null;
	}

	private final SuperCheckTypeConstructorVisitor superCheckTypeConstructorVisitor;
	private final TypingConstraints constraints;

	public TypeCheckingProcedure(TypingConstraints constraints)
	{
		this.constraints = constraints;
		superCheckTypeConstructorVisitor = new SuperCheckTypeConstructorVisitor(this, constraints);
	}

	public boolean equalTypes(@NotNull NapileType type1, @NotNull NapileType type2)
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

		List<NapileType> type1Arguments = type1.getArguments();
		List<NapileType> type2Arguments = type2.getArguments();
		if(type1Arguments.size() != type2Arguments.size())
		{
			return false;
		}

		for(int i = 0; i < type1Arguments.size(); i++)
		{
			NapileType typeProjection1 = type1Arguments.get(i);
			NapileType typeProjection2 = type2Arguments.get(i);

			if(!constraints.assertEqualTypes(typeProjection1, typeProjection2, this))
			{
				return false;
			}
		}
		return true;
	}


	public boolean isSubtypeOf(@NotNull NapileType subTypeOriginal, @NotNull NapileType superTypeOriginal)
	{
		if(ErrorUtils.isErrorType(subTypeOriginal) || ErrorUtils.isErrorType(superTypeOriginal))
			return true;

		if(!superTypeOriginal.isNullable() && subTypeOriginal.isNullable())
			return false;

		if(TypeUtils.isEqualFqName(subTypeOriginal, NapileLangPackage.NULL))
			return true;

		//System.out.println(subTypeOriginal + " " + superTypeOriginal);
		return TypeUtils.makeNotNullable(subTypeOriginal).accept(superCheckTypeConstructorVisitor, TypeUtils.makeNotNullable(superTypeOriginal));
	}
}
