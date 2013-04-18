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
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.types.impl.NapileTypeImpl;

/**
 * @author abreslav
 */
public class CommonSupertypes
{
	@NotNull
	public static NapileType commonSupertype(@NotNull Collection<NapileType> types)
	{
		Collection<NapileType> typeSet = new HashSet<NapileType>(types);
		assert !typeSet.isEmpty();

		final NapileScope napileScope = TypeUtils.getChainedScope(types);

		// If any of the types is nullable, the result must be nullable
		// This also removed Nothing and Nothing? because they are subtypes of everything else
		boolean nullable = false;
		for(Iterator<NapileType> iterator = typeSet.iterator(); iterator.hasNext(); )
		{
			NapileType type = iterator.next();
			assert type != null;
			if(TypeUtils.isEqualFqName(type, NapileLangPackage.NULL))
			{
				iterator.remove();
			}
			nullable |= type.isNullable();
		}

		// Everything deleted => it's Nothing or Nothing?
		if(typeSet.isEmpty())
			return TypeUtils.getTypeOfClassOrErrorType(napileScope, NapileLangPackage.NULL, nullable);

		if(typeSet.size() == 1)
		{
			return TypeUtils.makeNullableIfNeeded(typeSet.iterator().next(), nullable);
		}

		// constructor of the supertype -> all of its instantiations occurring as supertypes
		Map<TypeConstructor, Set<NapileType>> commonSupertypes = computeCommonRawSupertypes(typeSet);
		if(commonSupertypes.isEmpty())
			return TypeUtils.getTypeOfClassOrErrorType(napileScope, NapileLangPackage.NULL, nullable);
		while(commonSupertypes.size() > 1)
		{
			Set<NapileType> merge = new HashSet<NapileType>();
			for(Set<NapileType> supertypes : commonSupertypes.values())
			{
				merge.addAll(supertypes);
			}
			commonSupertypes = computeCommonRawSupertypes(merge);
		}
		assert !commonSupertypes.isEmpty() : commonSupertypes + " <- " + types;

		// constructor of the supertype -> all of its instantiations occurring as supertypes
		Map.Entry<TypeConstructor, Set<NapileType>> entry = commonSupertypes.entrySet().iterator().next();

		// Reconstructing type arguments if possible
		NapileType result = computeSupertypeProjections(entry.getKey(), entry.getValue());
		return TypeUtils.makeNullableIfNeeded(result, nullable);
	}

	// Raw supertypes are superclasses w/o type arguments
	// @return TypeConstructor -> all instantiations of this constructor occurring as supertypes
	@NotNull
	private static Map<TypeConstructor, Set<NapileType>> computeCommonRawSupertypes(@NotNull Collection<NapileType> types)
	{
		assert !types.isEmpty();

		final Map<TypeConstructor, Set<NapileType>> constructorToAllInstances = new HashMap<TypeConstructor, Set<NapileType>>();
		Set<TypeConstructor> commonSuperclasses = null;

		List<TypeConstructor> order = null;
		for(NapileType type : types)
		{
			Set<TypeConstructor> visited = new HashSet<TypeConstructor>();

			order = dfs(type, visited, new DfsNodeHandler<List<TypeConstructor>>()
			{
				public LinkedList<TypeConstructor> list = new LinkedList<TypeConstructor>();

				@Override
				public void beforeChildren(NapileType current)
				{
					TypeConstructor constructor = current.getConstructor();

					Set<NapileType> instances = constructorToAllInstances.get(constructor);
					if(instances == null)
					{
						instances = new HashSet<NapileType>();
						constructorToAllInstances.put(constructor, instances);
					}
					instances.add(current);
				}

				@Override
				public void afterChildren(NapileType current)
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
		Map<TypeConstructor, Set<NapileType>> result = new HashMap<TypeConstructor, Set<NapileType>>();
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
	private static NapileType computeSupertypeProjections(@NotNull TypeConstructor constructor, @NotNull Set<NapileType> types)
	{
		// we assume that all the given types are applications of the same type constructor

		assert !types.isEmpty();

		if(types.size() == 1)
		{
			return types.iterator().next();
		}

		List<TypeParameterDescriptor> parameters = constructor.getParameters();
		List<NapileType> newProjections = new ArrayList<NapileType>();
		for(int i = 0, parametersSize = parameters.size(); i < parametersSize; i++)
		{
			TypeParameterDescriptor parameterDescriptor = parameters.get(i);
			Set<NapileType> typeProjections = new HashSet<NapileType>();
			for(NapileType type : types)
				typeProjections.add(type.getArguments().get(i));
			newProjections.add(computeSupertypeProjection(parameterDescriptor, typeProjections));
		}

		boolean nullable = false;
		for(NapileType type : types)
		{
			nullable |= type.isNullable();
		}

		// TODO : attributes?
		NapileScope newScope = NapileScope.EMPTY;
		DeclarationDescriptor declarationDescriptor = constructor.getDeclarationDescriptor();
		if(declarationDescriptor instanceof ClassDescriptor)
		{
			newScope = ((ClassDescriptor) declarationDescriptor).getMemberScope(newProjections);
		}
		return new NapileTypeImpl(Collections.<AnnotationDescriptor>emptyList(), constructor, nullable, newProjections, newScope);
	}

	@NotNull
	private static NapileType computeSupertypeProjection(@NotNull TypeParameterDescriptor parameterDescriptor, @NotNull Set<NapileType> typeProjections)
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
		for(NapileType type : typeConstructor.getSupertypes())
		{
			markAll(type.getConstructor(), markerSet);
		}
	}

	private static <R> R dfs(@NotNull NapileType current, @NotNull Set<TypeConstructor> visited, @NotNull DfsNodeHandler<R> handler)
	{
		doDfs(current, visited, handler);
		return handler.result();
	}

	private static void doDfs(@NotNull NapileType current, @NotNull Set<TypeConstructor> visited, @NotNull DfsNodeHandler<?> handler)
	{
		if(!visited.add(current.getConstructor()))
		{
			return;
		}
		handler.beforeChildren(current);
		//        Map<TypeConstructor, TypeProjection> substitutionContext = TypeUtils.buildSubstitutionContext(current);
		TypeSubstitutor substitutor = TypeSubstitutor.create(current);
		for(NapileType supertype : current.getConstructor().getSupertypes())
		{
			TypeConstructor supertypeConstructor = supertype.getConstructor();
			if(visited.contains(supertypeConstructor))
			{
				continue;
			}
			NapileType substitutedSupertype = substitutor.safeSubstitute(supertype);
			dfs(substitutedSupertype, visited, handler);
		}
		handler.afterChildren(current);
	}

	private static class DfsNodeHandler<R>
	{

		public void beforeChildren(NapileType current)
		{

		}

		public void afterChildren(NapileType current)
		{

		}

		public R result()
		{
			return null;
		}
	}
}
