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

package org.napile.compiler.lang.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.util.CommonSuppliers;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * @author abreslav
 */
public class SubstitutionUtils
{
	@NotNull
	public static Map<TypeConstructor, NapileType> buildSubstitutionContext(@NotNull NapileType context)
	{
		return buildSubstitutionContext(context.getConstructor().getParameters(), context.getArguments());
	}

	/**
	 * Builds a context with all the supertypes' parameters substituted
	 */
	@NotNull
	public static TypeSubstitutor buildDeepSubstitutor(@NotNull NapileType type)
	{
		Map<TypeConstructor, NapileType> substitution = Maps.newHashMap();
		TypeSubstitutor typeSubstitutor = TypeSubstitutor.create(substitution);
		// we use the mutability of the map here
		fillInDeepSubstitutor(type, typeSubstitutor, substitution, null);
		return typeSubstitutor;
	}

	@NotNull
	public static Multimap<TypeConstructor, NapileType> buildDeepSubstitutionMultimap(@NotNull NapileType type)
	{
		Multimap<TypeConstructor, NapileType> fullSubstitution = CommonSuppliers.newLinkedHashSetHashSetMultimap();
		Map<TypeConstructor, NapileType> substitution = Maps.newHashMap();
		TypeSubstitutor typeSubstitutor = TypeSubstitutor.create(substitution);
		// we use the mutability of the map here
		fillInDeepSubstitutor(type, typeSubstitutor, substitution, fullSubstitution);
		return fullSubstitution;
	}

	// we use the mutability of the substitution map here
	private static void fillInDeepSubstitutor(@NotNull NapileType context, @NotNull TypeSubstitutor substitutor, @NotNull Map<TypeConstructor, NapileType> substitution, @Nullable Multimap<TypeConstructor, NapileType> fullSubstitution)
	{
		List<TypeParameterDescriptor> parameters = context.getConstructor().getParameters();
		List<NapileType> arguments = context.getArguments();

		if(parameters.size() != arguments.size())
		{
			throw new IllegalStateException();
		}

		for(int i = 0; i < arguments.size(); i++)
		{
			NapileType argument = arguments.get(i);
			TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);

			NapileType substitute = substitutor.substitute(argument, null);
			assert substitute != null;

			substitution.put(typeParameterDescriptor.getTypeConstructor(), substitute);
			if(fullSubstitution != null)
			{
				fullSubstitution.put(typeParameterDescriptor.getTypeConstructor(), substitute);
			}
		}
		if(TypeUtils.isEqualFqName(context, NapileLangPackage.NULL))
			return;
		for(NapileType supertype : context.getConstructor().getSupertypes())
		{
			fillInDeepSubstitutor(supertype, substitutor, substitution, fullSubstitution);
		}
	}

	@NotNull
	public static Map<TypeConstructor, NapileType> buildSubstitutionContext(@NotNull List<TypeParameterDescriptor> parameters, @NotNull List<NapileType> contextArguments)
	{
		Map<TypeConstructor, NapileType> parameterValues = new HashMap<TypeConstructor, NapileType>();
		fillInSubstitutionContext(parameters, contextArguments, parameterValues);
		return parameterValues;
	}

	private static void fillInSubstitutionContext(List<TypeParameterDescriptor> parameters, List<NapileType> contextArguments, Map<TypeConstructor, NapileType> parameterValues)
	{
		if(parameters.size() != contextArguments.size())
		{
			throw new IllegalArgumentException("type parameter count != context arguments");
		}
		for(int i = 0, parametersSize = parameters.size(); i < parametersSize; i++)
		{
			TypeParameterDescriptor parameter = parameters.get(i);
			NapileType value = contextArguments.get(i);
			parameterValues.put(parameter.getTypeConstructor(), value);
		}
	}

	public static boolean hasUnsubstitutedTypeParameters(NapileType type)
	{
		if(type.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor)
		{
			return true;
		}

		for(NapileType proj : type.getArguments())
		{
			if(hasUnsubstitutedTypeParameters(proj))
			{
				return true;
			}
		}

		return false;
	}

	public static void assertNotImmediatelyRecursive(Map<TypeConstructor, NapileType> context)
	{
		// Make sure we never replace a T with "Foo<T>" or something similar,
		// because the substitution will not terminate in this case
		// This check is not complete. It does not find cases like
		//    T -> Foo<T1>
		//    T -> Bar<T>

		for(Map.Entry<TypeConstructor, NapileType> entry : context.entrySet())
		{
			TypeConstructor key = entry.getKey();
			NapileType value = entry.getValue();
			if(TypeUtils.typeConstructorUsedInType(key, value))
			{
				throw new IllegalStateException("Immediately recursive substitution: " + context + "\nProblematic parameter: " + key + " -> " + value);
			}
		}
	}
}
