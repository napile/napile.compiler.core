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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeSubstitution;
import org.napile.compiler.lang.types.TypeSubstitutor;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.checker.TypeCheckingProcedure;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author svtk
 */
public class ConstraintSystemImpl implements ConstraintSystem
{

	public static final NapileType DONT_CARE = ErrorUtils.createErrorTypeWithCustomDebugName("DONT_CARE");

	private final Map<TypeParameterDescriptor, TypeConstraintsImpl> typeParameterConstraints = Maps.newLinkedHashMap();
	private final Set<ConstraintPosition> errorConstraintPositions = Sets.newHashSet();
	private final TypeSubstitutor resultingSubstitutor;
	private final TypeSubstitutor currentSubstitutor;
	private boolean hasErrorInConstrainingTypes;

	public ConstraintSystemImpl()
	{
		this.resultingSubstitutor = createTypeSubstitutorWithDefaultForUnknownTypeParameter(null);
		this.currentSubstitutor = createTypeSubstitutorWithDefaultForUnknownTypeParameter(DONT_CARE);
	}

	private TypeSubstitutor createTypeSubstitutorWithDefaultForUnknownTypeParameter(@Nullable final NapileType defaultTypeProjection)
	{
		return TypeSubstitutor.create(new TypeSubstitution()
		{
			@Override
			public NapileType get(TypeConstructor key)
			{
				DeclarationDescriptor declarationDescriptor = key.getDeclarationDescriptor();
				if(declarationDescriptor instanceof TypeParameterDescriptor)
				{
					TypeParameterDescriptor descriptor = (TypeParameterDescriptor) declarationDescriptor;

					NapileType value = ConstraintsUtil.getValue(getTypeConstraints(descriptor));
					if(value != null && !TypeUtils.dependsOnTypeParameterConstructors(value, Collections.singleton(DONT_CARE.getConstructor())))
					{
						return value;
					}
					if(typeParameterConstraints.containsKey(descriptor))
					{
						return defaultTypeProjection;
					}
				}
				return null;
			}

			@Override
			public boolean isEmpty()
			{
				return false;
			}

			@Override
			public String toString()
			{
				return typeParameterConstraints.toString();
			}
		});
	}

	@Override
	public boolean hasTypeConstructorMismatch()
	{
		return !errorConstraintPositions.isEmpty();
	}

	@Override
	public boolean hasTypeConstructorMismatchAt(@NotNull ConstraintPosition constraintPosition)
	{
		return errorConstraintPositions.contains(constraintPosition);
	}

	@Override
	public boolean hasExpectedTypeMismatch()
	{
		return errorConstraintPositions.size() == 1 && errorConstraintPositions.contains(ConstraintPosition.EXPECTED_TYPE_POSITION);
	}

	@Override
	public boolean hasErrorInConstrainingTypes()
	{
		return hasErrorInConstrainingTypes;
	}

	@Override
	public void registerTypeVariable(@NotNull TypeParameterDescriptor typeVariable)
	{
		typeParameterConstraints.put(typeVariable, new TypeConstraintsImpl());
	}

	@Override
	@NotNull
	public ConstraintSystem copy()
	{
		ConstraintSystemImpl newConstraintSystem = new ConstraintSystemImpl();
		for(Map.Entry<TypeParameterDescriptor, TypeConstraintsImpl> entry : typeParameterConstraints.entrySet())
		{
			TypeParameterDescriptor typeParameter = entry.getKey();
			TypeConstraintsImpl typeConstraints = entry.getValue();
			newConstraintSystem.typeParameterConstraints.put(typeParameter, typeConstraints.copy());
		}
		newConstraintSystem.errorConstraintPositions.addAll(errorConstraintPositions);
		newConstraintSystem.hasErrorInConstrainingTypes = hasErrorInConstrainingTypes;
		return newConstraintSystem;
	}

	@NotNull
	public ConstraintSystem replaceTypeVariables(@NotNull Function<TypeParameterDescriptor, TypeParameterDescriptor> typeVariablesMap)
	{
		ConstraintSystemImpl newConstraintSystem = new ConstraintSystemImpl();
		for(Map.Entry<TypeParameterDescriptor, TypeConstraintsImpl> entry : typeParameterConstraints.entrySet())
		{
			TypeParameterDescriptor typeParameter = entry.getKey();
			TypeConstraintsImpl typeConstraints = entry.getValue();

			TypeParameterDescriptor newTypeParameter = typeVariablesMap.apply(typeParameter);
			assert newTypeParameter != null;
			newConstraintSystem.typeParameterConstraints.put(newTypeParameter, typeConstraints);
		}
		newConstraintSystem.errorConstraintPositions.addAll(errorConstraintPositions);
		newConstraintSystem.hasErrorInConstrainingTypes = hasErrorInConstrainingTypes;
		return newConstraintSystem;
	}

	@Override
	public void addSubtypingConstraint(@NotNull NapileType subjectType, @Nullable NapileType constrainingType, @NotNull ConstraintPosition constraintPosition)
	{
		addConstraint(subjectType, constrainingType, constraintPosition);
	}

	@Override
	public void addSupertypeConstraint(@NotNull NapileType subjectType, @Nullable NapileType constrainingType, @NotNull ConstraintPosition constraintPosition)
	{
		addConstraint(subjectType, constrainingType, constraintPosition);
	}

	private void addConstraint(@NotNull NapileType subjectType, @Nullable NapileType constrainingType, @NotNull ConstraintPosition constraintPosition)
	{
		if(constrainingType == null || (ErrorUtils.isErrorType(constrainingType) && constrainingType != DONT_CARE))
		{
			hasErrorInConstrainingTypes = true;
			return;
		}

		assert subjectType != TypeUtils.NO_EXPECTED_TYPE : "Subject type shouldn't be NO_EXPECTED_TYPE (in position " + constraintPosition + " )";

		if(constrainingType == DONT_CARE || ErrorUtils.isErrorType(subjectType) || constrainingType == TypeUtils.NO_EXPECTED_TYPE)
			return;

		DeclarationDescriptor subjectTypeDescriptor = subjectType.getConstructor().getDeclarationDescriptor();

		if(subjectTypeDescriptor instanceof TypeParameterDescriptor)
		{
			TypeParameterDescriptor typeParameter = (TypeParameterDescriptor) subjectTypeDescriptor;
			TypeConstraintsImpl typeConstraints = typeParameterConstraints.get(typeParameter);
			if(typeConstraints != null)
			{
				if(TypeUtils.dependsOnTypeParameterConstructors(constrainingType, Collections.singleton(DONT_CARE.getConstructor())))
					return;

				if(subjectType.isNullable() && constrainingType.isNullable())
					constrainingType = TypeUtils.makeNotNullable(constrainingType);
				typeConstraints.addBound(constrainingType);
				return;
			}
		}

		NapileType correspondingSupertype = TypeCheckingProcedure.findCorrespondingSupertype(subjectType, constrainingType);
		if(correspondingSupertype != null)
			subjectType = correspondingSupertype;

		List<NapileType> subjectArguments = subjectType.getArguments();
		List<NapileType> constrainingArguments = constrainingType.getArguments();
		if(subjectArguments.size() != constrainingArguments.size())
			return;

		for(int i = 0; i < subjectArguments.size(); i++)
			addConstraint(subjectArguments.get(i), constrainingArguments.get(i), constraintPosition);
	}

	@NotNull
	@Override
	public Set<TypeParameterDescriptor> getTypeVariables()
	{
		return typeParameterConstraints.keySet();
	}

	@Override
	@Nullable
	public TypeConstraints getTypeConstraints(@NotNull TypeParameterDescriptor typeVariable)
	{
		return typeParameterConstraints.get(typeVariable);
	}

	@Override
	public boolean isSuccessful()
	{
		return !hasTypeConstructorMismatch() && !hasUnknownParameters() && !hasConflictingConstraints();
	}

	@Override
	public boolean hasContradiction()
	{
		return hasTypeConstructorMismatch() || hasConflictingConstraints();
	}

	@Override
	public boolean hasConflictingConstraints()
	{
		for(TypeParameterDescriptor typeParameter : typeParameterConstraints.keySet())
		{
			TypeConstraints typeConstraints = getTypeConstraints(typeParameter);
			if(typeConstraints != null && ConstraintsUtil.getValues(typeConstraints).size() > 1)
				return true;
		}
		return false;
	}

	@Override
	public boolean hasUnknownParameters()
	{
		for(TypeConstraintsImpl constraints : typeParameterConstraints.values())
		{
			if(constraints.isEmpty())
			{
				return true;
			}
		}
		return false;
	}

	@NotNull
	@Override
	public TypeSubstitutor getResultingSubstitutor()
	{
		return resultingSubstitutor;
	}

	@NotNull
	@Override
	public TypeSubstitutor getCurrentSubstitutor()
	{
		return currentSubstitutor;
	}
}
