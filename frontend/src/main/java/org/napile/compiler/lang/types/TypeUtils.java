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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.calls.inference.ConstraintResolutionListener;
import org.napile.compiler.lang.resolve.calls.inference.ConstraintSystemSolution;
import org.napile.compiler.lang.resolve.calls.inference.ConstraintSystemWithPriorities;
import org.napile.compiler.lang.resolve.calls.inference.ConstraintType;
import org.napile.compiler.lang.resolve.scopes.ChainedScope;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.types.checker.NapileTypeChecker;
import org.napile.compiler.lang.types.impl.NapileTypeImpl;
import org.napile.compiler.plugin.NodeVisitorAdapter;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import com.intellij.util.Processor;

/**
 * @author abreslav
 */
public class TypeUtils
{
	public static final NapileType NO_EXPECTED_TYPE = new NapileType()
	{
		@NotNull
		@Override
		public TypeConstructor getConstructor()
		{
			throw new UnsupportedOperationException(); // TODO
		}

		@NotNull
		@Override
		public List<NapileType> getArguments()
		{
			throw new UnsupportedOperationException(); // TODO
		}

		@Override
		public boolean isNullable()
		{
			throw new UnsupportedOperationException(); // TODO
		}

		@NotNull
		@Override
		public NapileScope getMemberScope()
		{
			throw new UnsupportedOperationException(); // TODO
		}

		@Override
		public <A, R> R accept(@NotNull TypeConstructorVisitor<A, R> visitor, A arg)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public List<AnnotationDescriptor> getAnnotations()
		{
			throw new UnsupportedOperationException(); // TODO
		}

		@Override
		public String toString()
		{
			return "NO_EXPECTED_TYPE";
		}
	};

	@NotNull
	public static NapileType makeNullable(@NotNull NapileType type)
	{
		return makeNullableAsSpecified(type, true);
	}

	@NotNull
	public static NapileType makeNotNullable(@NotNull NapileType type)
	{
		return makeNullableAsSpecified(type, false);
	}

	@NotNull
	public static NapileType makeNullableAsSpecified(@NotNull NapileType type, boolean nullable)
	{
		if(type.isNullable() == nullable)
		{
			return type;
		}
		if(ErrorUtils.isErrorType(type))
		{
			return type;
		}
		return new NapileTypeImpl(type.getAnnotations(), type.getConstructor(), nullable, type.getArguments(), type.getMemberScope());
	}

	public static boolean isIntersectionEmpty(@NotNull NapileType typeA, @NotNull NapileType typeB)
	{
		return intersect(NapileTypeChecker.INSTANCE, Sets.newLinkedHashSet(Lists.newArrayList(typeA, typeB)), new ChainedScope(null, typeA.getMemberScope(), typeB.getMemberScope())) == null;
	}

	@Nullable
	public static NapileType intersect(@NotNull NapileTypeChecker typeChecker, @NotNull Set<NapileType> types, @NotNull NapileScope napileScope)
	{
		if(types.isEmpty())
		{
			return getTypeOfClassOrErrorType(napileScope, NapileLangPackage.ANY, true);
		}

		if(types.size() == 1)
		{
			return types.iterator().next();
		}

		// Intersection of T1..Tn is an intersection of their non-null versions,
		//   made nullable is they all were nullable
		boolean allNullable = true;
		boolean nothingTypePresent = false;
		List<NapileType> nullabilityStripped = Lists.newArrayList();
		for(NapileType type : types)
		{
			nothingTypePresent |= isEqualFqName(type, NapileLangPackage.NULL);
			allNullable &= type.isNullable();
			nullabilityStripped.add(makeNotNullable(type));
		}

		if(nothingTypePresent)
		{
			return getTypeOfClassOrErrorType(napileScope, NapileLangPackage.NULL, allNullable);
		}

		// Now we remove types that have subtypes in the list
		List<NapileType> resultingTypes = Lists.newArrayList();
		outer:
		for(NapileType type : nullabilityStripped)
		{
			if(!canHaveSubtypes(typeChecker, type))
			{
				for(NapileType other : nullabilityStripped)
				{
					// It makes sense to check for subtyping (other <: type), despite that
					// type is not supposed to be open, for there're enums
					if(!TypeUnifier.mayBeEqual(type, other, napileScope) &&
							!typeChecker.isSubtypeOf(type, other) &&
							!typeChecker.isSubtypeOf(other, type))
					{
						return null;
					}
				}
				return makeNullableAsSpecified(type, allNullable);
			}
			else
			{
				for(NapileType other : nullabilityStripped)
				{
					if(!type.equals(other) && typeChecker.isSubtypeOf(other, type))
					{
						continue outer;
					}
				}
			}

			resultingTypes.add(type);
		}

		if(resultingTypes.size() == 1)
		{
			return makeNullableAsSpecified(resultingTypes.get(0), allNullable);
		}


		List<AnnotationDescriptor> noAnnotations = Collections.<AnnotationDescriptor>emptyList();
		TypeConstructor constructor = new IntersectionTypeConstructor(noAnnotations, resultingTypes);

		NapileScope[] scopes = new NapileScope[resultingTypes.size()];
		int i = 0;
		for(NapileType type : resultingTypes)
		{
			scopes[i] = type.getMemberScope();
			i++;
		}

		return new NapileTypeImpl(noAnnotations, constructor, allNullable, Collections.<NapileType>emptyList(), new ChainedScope(null, scopes)); // TODO : check intersectibility, don't use a chanied scope
	}

	private static class TypeUnifier
	{
		private static class TypeParameterUsage
		{
			private final TypeParameterDescriptor typeParameterDescriptor;

			public TypeParameterUsage(TypeParameterDescriptor typeParameterDescriptor)
			{
				this.typeParameterDescriptor = typeParameterDescriptor;
			}
		}

		public static boolean mayBeEqual(@NotNull NapileType type, @NotNull NapileType other, @NotNull NapileScope napileScope)
		{
			return unify(type, other, napileScope);
		}

		private static boolean unify(NapileType withParameters, NapileType expected, @NotNull NapileScope napileScope)
		{
			ConstraintSystemWithPriorities constraintSystem = new ConstraintSystemWithPriorities(ConstraintResolutionListener.DO_NOTHING);
			// T -> how T is used
			final List<TypeParameterDescriptor> parameters = Lists.newArrayList();
			Processor<TypeParameterUsage> processor = new Processor<TypeParameterUsage>()
			{
				@Override
				public boolean process(TypeParameterUsage parameterUsage)
				{
					parameters.add(parameterUsage.typeParameterDescriptor);
					return true;
				}
			};
			processAllTypeParameters(withParameters, processor);
			processAllTypeParameters(expected, processor);
			for(TypeParameterDescriptor entry : parameters)
			{
				constraintSystem.registerTypeVariable(entry);
			}
			constraintSystem.addSubtypingConstraint(ConstraintType.VALUE_ARGUMENT.assertSubtyping(withParameters, expected));

			ConstraintSystemSolution solution = constraintSystem.solve(napileScope);
			return solution.getStatus().isSuccessful();
		}

		private static void processAllTypeParameters(NapileType type, Processor<TypeParameterUsage> result)
		{
			ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
			if(descriptor instanceof TypeParameterDescriptor)
			{
				result.process(new TypeParameterUsage((TypeParameterDescriptor) descriptor));
			}
			for(NapileType projection : type.getArguments())
			{
				processAllTypeParameters(projection, result);
			}
		}
	}

	public static boolean canHaveSubtypes(NapileTypeChecker typeChecker, NapileType type)
	{
		if(type.isNullable())
		{
			return true;
		}
		if(!type.getConstructor().isSealed())
		{
			return true;
		}

		List<TypeParameterDescriptor> parameters = type.getConstructor().getParameters();
		List<NapileType> arguments = type.getArguments();
		for(int i = 0, parametersSize = parameters.size(); i < parametersSize; i++)
		{
			TypeParameterDescriptor parameterDescriptor = parameters.get(i);
			NapileType typeProjection = arguments.get(i);

			NapileType argument = typeProjection;

		/*	switch(parameterDescriptor.getVariance())
			{
				case INVARIANT:
					switch(projectionKind)
					{
						case INVARIANT:
							if(lowerThanBound(typeChecker, argument, parameterDescriptor) || canHaveSubtypes(typeChecker, argument))
							{
								return true;
							}
							break;
						case IN_VARIANCE:
							if(lowerThanBound(typeChecker, argument, parameterDescriptor))
							{
								return true;
							}
							break;
						case OUT_VARIANCE:
							if(canHaveSubtypes(typeChecker, argument))
							{
								return true;
							}
							break;
					}
					break;
				case IN_VARIANCE:
					if(projectionKind != Variance.OUT_VARIANCE)
					{
						if(lowerThanBound(typeChecker, argument, parameterDescriptor))
						{
							return true;
						}
					}
					else
					{
						if(canHaveSubtypes(typeChecker, argument))
						{
							return true;
						}
					}
					break;
				case OUT_VARIANCE:
					if(projectionKind != Variance.IN_VARIANCE)
					{
						if(canHaveSubtypes(typeChecker, argument))
						{
							return true;
						}
					}
					else
					{
						if(lowerThanBound(typeChecker, argument, parameterDescriptor))
						{
							return true;
						}
					}
					break;
			}  */
		}
		return false;
	}

	private static boolean lowerThanBound(NapileTypeChecker typeChecker, NapileType argument, TypeParameterDescriptor parameterDescriptor)
	{
		for(NapileType bound : parameterDescriptor.getUpperBounds())
		{
			if(typeChecker.isSubtypeOf(argument, bound))
			{
				if(!argument.getConstructor().equals(bound.getConstructor()))
				{
					return true;
				}
			}
		}
		return false;
	}

	public static NapileType makeNullableIfNeeded(NapileType type, boolean nullable)
	{
		if(nullable)
		{
			return makeNullable(type);
		}
		return type;
	}

	@NotNull
	public static NapileType makeUnsubstitutedType(ClassDescriptor classDescriptor, NapileScope unsubstitutedMemberScope)
	{
		if(ErrorUtils.isError(classDescriptor))
		{
			return ErrorUtils.createErrorType("Unsubstituted type for " + classDescriptor);
		}
		List<NapileType> arguments = getDefaultTypeProjections(classDescriptor.getTypeConstructor().getParameters());
		return new NapileTypeImpl(Collections.<AnnotationDescriptor>emptyList(), classDescriptor.getTypeConstructor(), false, arguments, unsubstitutedMemberScope);
	}

	@NotNull
	public static List<NapileType> getDefaultTypeProjections(List<TypeParameterDescriptor> parameters)
	{
		List<NapileType> result = new ArrayList<NapileType>(parameters.size());
		for(TypeParameterDescriptor parameterDescriptor : parameters)
		{
			result.add(parameterDescriptor.getDefaultType());
		}
		return result;
	}

	@NotNull
	public static List<NapileType> getDefaultTypes(List<TypeParameterDescriptor> parameters)
	{
		List<NapileType> result = Lists.newArrayList();
		for(TypeParameterDescriptor parameterDescriptor : parameters)
		{
			result.add(parameterDescriptor.getDefaultType());
		}
		return result;
	}

	private static void collectImmediateSupertypes(@NotNull NapileType type, @NotNull Collection<NapileType> result)
	{
		TypeSubstitutor substitutor = TypeSubstitutor.create(type);
		for(NapileType supertype : type.getConstructor().getSupertypes())
		{
			result.add(substitutor.substitute(supertype, null));
		}
	}

	@NotNull
	public static List<NapileType> getImmediateSupertypes(@NotNull NapileType type)
	{
		List<NapileType> result = Lists.newArrayList();
		collectImmediateSupertypes(type, result);
		return result;
	}

	private static void collectAllSupertypes(@NotNull NapileType type, @NotNull Set<NapileType> result)
	{
		List<NapileType> immediateSupertypes = getImmediateSupertypes(type);
		result.addAll(immediateSupertypes);
		for(NapileType supertype : immediateSupertypes)
		{
			collectAllSupertypes(supertype, result);
		}
	}


	@NotNull
	public static Set<NapileType> getAllSupertypes(@NotNull NapileType type)
	{
		Set<NapileType> result = Sets.newLinkedHashSet();
		collectAllSupertypes(type, result);
		return result;
	}

	public static boolean equalClasses(@NotNull NapileType type1, @NotNull NapileType type2)
	{
		DeclarationDescriptor declarationDescriptor1 = type1.getConstructor().getDeclarationDescriptor();
		if(declarationDescriptor1 == null)
			return false; // No class, classes are not equal
		DeclarationDescriptor declarationDescriptor2 = type2.getConstructor().getDeclarationDescriptor();
		if(declarationDescriptor2 == null)
			return false; // Class of type1 is not null
		return declarationDescriptor1.getOriginal().equals(declarationDescriptor2.getOriginal());
	}

	@Nullable
	public static ClassDescriptor getClassDescriptor(@NotNull NapileType type)
	{
		DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
		if(declarationDescriptor instanceof ClassDescriptor)
		{
			return (ClassDescriptor) declarationDescriptor;
		}
		return null;
	}

	@NotNull
	public static NapileType substituteParameters(@NotNull ClassDescriptor clazz, @NotNull List<NapileType> actualTypeParameters)
	{
		List<TypeParameterDescriptor> clazzTypeParameters = clazz.getTypeConstructor().getParameters();

		if(clazzTypeParameters.size() != actualTypeParameters.size())
		{
			throw new IllegalArgumentException("type parameter counts do not match: " + clazz + ", " + actualTypeParameters);
		}

		Map<TypeConstructor, NapileType> substitutions = Maps.newHashMap();

		for(int i = 0; i < clazzTypeParameters.size(); ++i)
		{
			TypeConstructor typeConstructor = clazzTypeParameters.get(i).getTypeConstructor();
			substitutions.put(typeConstructor, actualTypeParameters.get(i));
		}

		return TypeSubstitutor.create(substitutions).substitute(clazz.getDefaultType(), null);
	}

	private static void addAllClassDescriptors(@NotNull NapileType type, @NotNull Set<ClassDescriptor> set)
	{
		ClassDescriptor cd = getClassDescriptor(type);
		if(cd != null)
		{
			set.add(cd);
		}
		for(NapileType projection : type.getArguments())
		{
			addAllClassDescriptors(projection, set);
		}
	}

	@NotNull
	public static List<ClassDescriptor> getAllClassDescriptors(@NotNull NapileType type)
	{
		Set<ClassDescriptor> classDescriptors = new HashSet<ClassDescriptor>();
		addAllClassDescriptors(type, classDescriptors);
		return new ArrayList<ClassDescriptor>(classDescriptors);
	}

	public static boolean equalTypes(@NotNull NapileType a, @NotNull NapileType b)
	{
		return NapileTypeChecker.INSTANCE.isSubtypeOf(a, b) && NapileTypeChecker.INSTANCE.isSubtypeOf(b, a);
	}

	public static boolean typeConstructorUsedInType(@NotNull TypeConstructor key, @NotNull NapileType value)
	{
		if(value.getConstructor() == key)
			return true;
		for(NapileType projection : value.getArguments())
		{
			if(typeConstructorUsedInType(key, projection))
			{
				return true;
			}
		}
		return false;
	}

	public static boolean dependsOnTypeParameters(@NotNull NapileType type, @NotNull Collection<TypeParameterDescriptor> typeParameters)
	{
		return dependsOnTypeParameterConstructors(type, Collections2.transform(typeParameters, new Function<TypeParameterDescriptor, TypeConstructor>()
		{
			@Override
			public TypeConstructor apply(@Nullable TypeParameterDescriptor typeParameterDescriptor)
			{
				assert typeParameterDescriptor != null;
				return typeParameterDescriptor.getTypeConstructor();
			}
		}));
	}

	public static boolean dependsOnTypeParameterConstructors(@NotNull NapileType type, @NotNull Collection<TypeConstructor> typeParameterConstructors)
	{
		if(typeParameterConstructors.contains(type.getConstructor()))
			return true;
		for(NapileType typeProjection : type.getArguments())
		{
			if(dependsOnTypeParameterConstructors(typeProjection, typeParameterConstructors))
			{
				return true;
			}
		}
		return false;
	}

	@NotNull
	public static NapileType getTypeOfClassOrErrorType(@NotNull NapileScope napileScope, @NotNull FqName name)
	{
		return getTypeOfClassOrErrorType(napileScope, name, false);
	}

	@NotNull
	public static NapileType getTypeOfClassOrErrorType(@NotNull NapileScope napileScope, @NotNull FqName name, boolean nullable)
	{
		ClassDescriptor classifierDescriptor = napileScope.getClass(name);
		if(classifierDescriptor == null || napileScope instanceof ErrorUtils.ErrorScope)
			return ErrorUtils.createErrorType(name.getFqName());
		else
		{
			napileScope = classifierDescriptor.getMemberScope(Collections.<NapileType>emptyList());
			if(napileScope instanceof ErrorUtils.ErrorScope)
				return ErrorUtils.createErrorType(name.getFqName());

			return new NapileTypeImpl(Collections.<AnnotationDescriptor>emptyList(), classifierDescriptor.getTypeConstructor(), nullable, Collections.<NapileType>emptyList(), napileScope);
		}
	}

	public static boolean isEqualFqName(@NotNull NapileType napileType, @NotNull FqName name)
	{
		if(napileType == NO_EXPECTED_TYPE)
		{
			return false;
		}
		return isEqualFqName(napileType.getConstructor(), name);
	}

	public static boolean isEqualFqName(@NotNull TypeConstructor constructor, @NotNull FqName name)
	{
		ClassifierDescriptor classifierDescriptor = constructor.getDeclarationDescriptor();
		if(classifierDescriptor == null)
			return false;
		else
			return DescriptorUtils.getFQName(classifierDescriptor).equals(name);
	}

	public static NapileScope getChainedScope(Collection<NapileType> set)
	{
		List<NapileScope> list = new ArrayList<NapileScope>(set.size());
		for(NapileType bound : set)
			list.add(bound.getMemberScope());
		return new ChainedScope(null, list.toArray(new NapileScope[list.size()]));
	}

	@Nullable
	public static FqName getFqName(@NotNull NapileType napileType)
	{
		ClassifierDescriptor classifierDescriptor = napileType.getConstructor().getDeclarationDescriptor();
		if(classifierDescriptor == null)
			return null;
		else
			return DescriptorUtils.getFQName(classifierDescriptor).toSafe();
	}

	public static NapileType toCompilerType(@NotNull final NapileScope scope, @NotNull TypeNode typeNode)
	{
		final Pair<TypeConstructor, NapileScope> accept = typeNode.typeConstructorNode.accept(new NodeVisitorAdapter<Void, Pair<TypeConstructor, NapileScope>>()
		{
			@Override
			public Pair<TypeConstructor, NapileScope> visitClassTypeNode(ClassTypeNode classTypeNode, Void a2)
			{
				final ClassDescriptor aClass = scope.getClass(classTypeNode.className);
				return aClass == null ? null : new Pair<TypeConstructor, NapileScope>(aClass.getTypeConstructor(), aClass.getMemberScope(Collections.<NapileType>emptyList()));
			}
		}, null);

		if(accept == null)
		{
			return ErrorUtils.createErrorType("'" + typeNode + "' is not resolved");
		}

		List<NapileType> parameters = typeNode.arguments.isEmpty() ? Collections.<NapileType>emptyList() : new ArrayList<NapileType>(typeNode.arguments.size());
		for(TypeNode temp : typeNode.arguments)
		{
			parameters.add(toCompilerType(scope, temp));
		}

		return new NapileTypeImpl(Collections.<AnnotationDescriptor>emptyList(), accept.getFirst(), typeNode.nullable, parameters, accept.getSecond());
	}
}
