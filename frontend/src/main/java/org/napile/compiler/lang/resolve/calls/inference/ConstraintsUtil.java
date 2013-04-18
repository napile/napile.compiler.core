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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeSubstitutor;
import org.napile.compiler.lang.types.checker.NapileTypeChecker;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author svtk
 */
public class ConstraintsUtil
{

	@NotNull
	public static Set<NapileType> getValues(@Nullable TypeConstraints typeConstraints)
	{
		Set<NapileType> values = Sets.newLinkedHashSet();
		if(typeConstraints != null && !typeConstraints.isEmpty())
		{
			if(!typeConstraints.getUpperBounds().isEmpty())
			{
				//todo subTypeOfUpperBounds
				NapileType subTypeOfUpperBounds = typeConstraints.getUpperBounds().iterator().next(); //todo
				if(values.isEmpty())
				{
					values.add(subTypeOfUpperBounds);
				}
				for(NapileType value : values)
				{
					if(!NapileTypeChecker.INSTANCE.isSubtypeOf(value, subTypeOfUpperBounds))
					{
						values.add(subTypeOfUpperBounds);
						break;
					}
				}
			}
		}
		return values;
	}

	@Nullable
	public static NapileType getValue(@Nullable TypeConstraints typeConstraints)
	{
		//todo all checks
		//todo variance dependance
		if(typeConstraints == null)
		{
			//todo assert typeConstraints != null;
			return null;
		}
		Set<NapileType> values = getValues(typeConstraints);
		if(values.size() == 1)
		{
			return values.iterator().next();
		}
		return null;
	}


	@Nullable
	public static TypeParameterDescriptor getFirstConflictingParameter(@NotNull ConstraintSystem constraintSystem)
	{
		for(TypeParameterDescriptor typeParameter : constraintSystem.getTypeVariables())
		{
			TypeConstraints constraints = constraintSystem.getTypeConstraints(typeParameter);
			if(getValues(constraints).size() > 1)
			{
				return typeParameter;
			}
		}
		return null;
	}

	@NotNull
	public static Collection<TypeSubstitutor> getSubstitutorsForConflictingParameters(@NotNull ConstraintSystem constraintSystem)
	{
		TypeParameterDescriptor firstConflictingParameter = getFirstConflictingParameter(constraintSystem);
		if(firstConflictingParameter == null)
			return Collections.emptyList();

		Collection<NapileType> conflictingTypes = getValues(constraintSystem.getTypeConstraints(firstConflictingParameter));

		ArrayList<Map<TypeConstructor, NapileType>> substitutionContexts = Lists.newArrayList();
		for(NapileType type : conflictingTypes)
		{
			Map<TypeConstructor, NapileType> context = Maps.newLinkedHashMap();
			context.put(firstConflictingParameter.getTypeConstructor(), type);
			substitutionContexts.add(context);
		}

		for(TypeParameterDescriptor typeParameter : constraintSystem.getTypeVariables())
		{
			if(typeParameter == firstConflictingParameter)
				continue;

			NapileType safeType = getSafeValue(constraintSystem, typeParameter);
			for(Map<TypeConstructor, NapileType> context : substitutionContexts)
			{
				context.put(typeParameter.getTypeConstructor(), safeType);
			}
		}
		Collection<TypeSubstitutor> typeSubstitutors = Lists.newArrayList();
		for(Map<TypeConstructor, NapileType> context : substitutionContexts)
		{
			typeSubstitutors.add(TypeSubstitutor.create(context));
		}
		return typeSubstitutors;
	}

	@NotNull
	public static NapileType getSafeValue(@NotNull ConstraintSystem constraintSystem, @NotNull TypeParameterDescriptor typeParameter)
	{
		TypeConstraints constraints = constraintSystem.getTypeConstraints(typeParameter);
		NapileType type = getValue(constraints);
		if(type != null)
		{
			return type;
		}
		//todo may be error type
		return typeParameter.getUpperBoundsAsType();
	}

	public static boolean checkUpperBoundIsSatisfied(@NotNull ConstraintSystem constraintSystem, @NotNull TypeParameterDescriptor typeParameter)
	{
		TypeConstraints typeConstraints = constraintSystem.getTypeConstraints(typeParameter);
		assert typeConstraints != null;
		NapileType type = getValue(typeConstraints);
		NapileType upperBound = typeParameter.getUpperBoundsAsType();
		NapileType substitute = constraintSystem.getResultingSubstitutor().substitute(upperBound, null);

		if(type != null)
		{
			if(substitute == null || !NapileTypeChecker.INSTANCE.isSubtypeOf(type, substitute))
			{
				return false;
			}
		}
		return true;
	}

	public static boolean checkBoundsAreSatisfied(@NotNull ConstraintSystem constraintSystem)
	{
		for(TypeParameterDescriptor typeVariable : constraintSystem.getTypeVariables())
		{
			NapileType type = getValue(constraintSystem.getTypeConstraints(typeVariable));
			NapileType upperBound = typeVariable.getUpperBoundsAsType();
			NapileType substitutedType = constraintSystem.getResultingSubstitutor().substitute(upperBound, null);

			if(type != null)
			{
				if(substitutedType == null || !NapileTypeChecker.INSTANCE.isSubtypeOf(type, substitutedType))
				{
					return false;
				}
			}
		}
		return true;
	}
}
