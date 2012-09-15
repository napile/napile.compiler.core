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

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.rt.NapileLangPackage;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.impl.JetTypeImpl;
import org.napile.compiler.lang.types.lang.JetStandardClasses;

/**
 * @author abreslav
 */
public class CommonSupertypes
{
	@NotNull
	public static JetType commonSupertype(@NotNull Collection<JetType> types)
	{
		Collection<JetType> typeSet = new HashSet<JetType>(types);
		assert !typeSet.isEmpty();

		final JetScope jetScope = TypeUtils.getChainedScope(types);

		// If any of the types is nullable, the result must be nullable
		// This also removed Nothing and Nothing? because they are subtypes of everything else
		boolean nullable = false;
		for(Iterator<JetType> iterator = typeSet.iterator(); iterator.hasNext(); )
		{
			JetType type = iterator.next();
			assert type != null;
			if(TypeUtils.isEqualFqName(type, NapileLangPackage.NULL))
			{
				iterator.remove();
			}
			nullable |= type.isNullable();
		}

		// Everything deleted => it's Nothing or Nothing?
		if(typeSet.isEmpty())
			return TypeUtils.getTypeOfClassOrErrorType(jetScope, NapileLangPackage.NULL, nullable);

		if(typeSet.size() == 1)
		{
			return TypeUtils.makeNullableIfNeeded(typeSet.iterator().next(), nullable);
		}

		// constructor of the supertype -> all of its instantiations occurring as supertypes
		Map<TypeConstructor, Set<JetType>> commonSupertypes = computeCommonRawSupertypes(typeSet);
		while(commonSupertypes.size() > 1)
		{
			Set<JetType> merge = new HashSet<JetType>();
			for(Set<JetType> supertypes : commonSupertypes.values())
			{
				merge.addAll(supertypes);
			}
			commonSupertypes = computeCommonRawSupertypes(merge);
		}
		assert !commonSupertypes.isEmpty() : commonSupertypes + " <- " + types;

		// constructor of the supertype -> all of its instantiations occurring as supertypes
		Map.Entry<TypeConstructor, Set<JetType>> entry = commonSupertypes.entrySet().iterator().next();

		// Reconstructing type arguments if possible
		JetType result = computeSupertypeProjections(entry.getKey(), entry.getValue());
		return TypeUtils.makeNullableIfNeeded(result, nullable);
	}

	// Raw supertypes are superclasses w/o type arguments
	// @return TypeConstructor -> all instantiations of this constructor occurring as supertypes
	@NotNull
	private static Map<TypeConstructor, Set<JetType>> computeCommonRawSupertypes(@NotNull Collection<JetType> types)
	{
		assert !types.isEmpty();

		final Map<TypeConstructor, Set<JetType>> constructorToAllInstances = new HashMap<TypeConstructor, Set<JetType>>();
		Set<TypeConstructor> commonSuperclasses = null;

		List<TypeConstructor> order = null;
		for(JetType type : types)
		{
			Set<TypeConstructor> visited = new HashSet<TypeConstructor>();

			order = dfs(type, visited, new DfsNodeHandler<List<TypeConstructor>>()
			{
				public LinkedList<TypeConstructor> list = new LinkedList<TypeConstructor>();

				@Override
				public void beforeChildren(JetType current)
				{
					TypeConstructor constructor = current.getConstructor();

					Set<JetType> instances = constructorToAllInstances.get(constructor);
					if(instances == null)
					{
						instances = new HashSet<JetType>();
						constructorToAllInstances.put(constructor, instances);
					}
					instances.add(current);
				}

				@Override
				public void afterChildren(JetType current)
				{
					list.addFirst(current.getConstructor());
				}

				@Override
				public List<TypeConstructor> result()
				{
					return list;
				}
			});

			if(commonSuperclasses == null)
			{
				commonSuperclasses = visited;
			}
			else
			{
				commonSuperclasses.retainAll(visited);
			}
		}
		assert order != null;

		Set<TypeConstructor> notSource = new HashSet<TypeConstructor>();
		Map<TypeConstructor, Set<JetType>> result = new HashMap<TypeConstructor, Set<JetType>>();
		for(TypeConstructor superConstructor : order)
		{
			if(!commonSuperclasses.contains(superConstructor))
			{
				continue;
			}

			if(!notSource.contains(superConstructor))
			{
				result.put(superConstructor, constructorToAllInstances.get(superConstructor));
				markAll(superConstructor, notSource);
			}
		}

		return result;
	}

	// constructor - type constructor of a supertype to be instantiated
	// types - instantiations of constructor occurring as supertypes of classes we are trying to intersect
	@NotNull
	private static JetType computeSupertypeProjections(@NotNull TypeConstructor constructor, @NotNull Set<JetType> types)
	{
		// we assume that all the given types are applications of the same type constructor

		assert !types.isEmpty();

		if(types.size() == 1)
		{
			return types.iterator().next();
		}

		List<TypeParameterDescriptor> parameters = constructor.getParameters();
		List<JetType> newProjections = new ArrayList<JetType>();
		for(int i = 0, parametersSize = parameters.size(); i < parametersSize; i++)
		{
			TypeParameterDescriptor parameterDescriptor = parameters.get(i);
			Set<JetType> typeProjections = new HashSet<JetType>();
			for(JetType type : types)
				typeProjections.add(type.getArguments().get(i));
			newProjections.add(computeSupertypeProjection(parameterDescriptor, typeProjections));
		}

		boolean nullable = false;
		for(JetType type : types)
		{
			nullable |= type.isNullable();
		}

		// TODO : attributes?
		JetScope newScope = JetStandardClasses.STUB;
		DeclarationDescriptor declarationDescriptor = constructor.getDeclarationDescriptor();
		if(declarationDescriptor instanceof ClassDescriptor)
		{
			newScope = ((ClassDescriptor) declarationDescriptor).getMemberScope(newProjections);
		}
		return new JetTypeImpl(Collections.<AnnotationDescriptor>emptyList(), constructor, nullable, newProjections, newScope);
	}

	@NotNull
	private static JetType computeSupertypeProjection(@NotNull TypeParameterDescriptor parameterDescriptor, @NotNull Set<JetType> typeProjections)
	{
		if(typeProjections.size() == 1)
		{
			return typeProjections.iterator().next();
		}

		return commonSupertype(parameterDescriptor.getUpperBounds());
	}

	private static void markAll(@NotNull TypeConstructor typeConstructor, @NotNull Set<TypeConstructor> markerSet)
	{
		markerSet.add(typeConstructor);
		for(JetType type : typeConstructor.getSupertypes())
		{
			markAll(type.getConstructor(), markerSet);
		}
	}

	private static <R> R dfs(@NotNull JetType current, @NotNull Set<TypeConstructor> visited, @NotNull DfsNodeHandler<R> handler)
	{
		doDfs(current, visited, handler);
		return handler.result();
	}

	private static void doDfs(@NotNull JetType current, @NotNull Set<TypeConstructor> visited, @NotNull DfsNodeHandler<?> handler)
	{
		if(!visited.add(current.getConstructor()))
		{
			return;
		}
		handler.beforeChildren(current);
		//        Map<TypeConstructor, TypeProjection> substitutionContext = TypeUtils.buildSubstitutionContext(current);
		TypeSubstitutor substitutor = TypeSubstitutor.create(current);
		for(JetType supertype : current.getConstructor().getSupertypes())
		{
			TypeConstructor supertypeConstructor = supertype.getConstructor();
			if(visited.contains(supertypeConstructor))
			{
				continue;
			}
			JetType substitutedSupertype = substitutor.safeSubstitute(supertype);
			dfs(substitutedSupertype, visited, handler);
		}
		handler.afterChildren(current);
	}

	private static class DfsNodeHandler<R>
	{

		public void beforeChildren(JetType current)
		{

		}

		public void afterChildren(JetType current)
		{

		}

		public R result()
		{
			return null;
		}
	}
}
