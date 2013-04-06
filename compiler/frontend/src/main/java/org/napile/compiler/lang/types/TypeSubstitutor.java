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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.SubstitutingScope;
import org.napile.compiler.lang.types.impl.JetTypeImpl;
import org.napile.compiler.lang.types.impl.MethodTypeConstructorImpl;
import org.napile.compiler.lang.types.impl.MultiTypeConstructorImpl;
import com.intellij.openapi.progress.ProcessCanceledException;

/**
 * @author abreslav
 */
public class TypeSubstitutor
{

	private static final int MAX_RECURSION_DEPTH = 100;

	public static class MapToTypeSubstitutionAdapter implements TypeSubstitution
	{
		@NotNull
		private final Map<TypeConstructor, JetType> substitutionContext;

		public MapToTypeSubstitutionAdapter(@NotNull Map<TypeConstructor, JetType> substitutionContext)
		{
			this.substitutionContext = substitutionContext;
		}

		@Override
		public JetType get(TypeConstructor key)
		{
			return substitutionContext.get(key);
		}

		@Override
		public boolean isEmpty()
		{
			return substitutionContext.isEmpty();
		}

		@Override
		public String toString()
		{
			return substitutionContext.toString();
		}
	}

	public static final TypeSubstitutor EMPTY = create(TypeSubstitution.EMPTY);
	public static final TypeSubstitutor DEFAULT_TYPE_FOR_TYPE_PARAMETERS = create(TypeSubstitution.DEFAULT_TYPE_FOR_TYPE_PARAMETERS);

	public static TypeSubstitutor create(@NotNull TypeSubstitution substitution)
	{
		return new TypeSubstitutor(substitution);
	}

	public static TypeSubstitutor create(@NotNull TypeSubstitution... substitutions)
	{
		return create(new CompositeTypeSubstitution(substitutions));
	}

	/**
	 * No assertion for immediate recursion
	 */
	public static TypeSubstitutor createUnsafe(@NotNull Map<TypeConstructor, JetType> substitutionContext)
	{
		Map<TypeConstructor, JetType> cleanContext = substitutionContext;
		return create(new MapToTypeSubstitutionAdapter(cleanContext));
	}

	public static TypeSubstitutor create(@NotNull Map<TypeConstructor, JetType> substitutionContext)
	{
		Map<TypeConstructor, JetType> cleanContext = substitutionContext;
		//SubstitutionUtils.assertNotImmediatelyRecursive(cleanContext);
		return createUnsafe(cleanContext);
	}

	public static TypeSubstitutor create(@NotNull JetType context)
	{
		return create(SubstitutionUtils.buildSubstitutionContext(context));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final
	@NotNull
	TypeSubstitution substitution;

	protected TypeSubstitutor(@NotNull TypeSubstitution substitution)
	{
		this.substitution = substitution;
	}

	public boolean inRange(@NotNull TypeConstructor typeConstructor)
	{
		return substitution.get(typeConstructor) != null;
	}

	public boolean isEmpty()
	{
		return substitution.isEmpty();
	}

	@NotNull
	public TypeSubstitution getSubstitution()
	{
		return substitution;
	}

	@NotNull
	public JetType safeSubstitute(@NotNull JetType type)
	{
		return unsafeSubstitute(type, null, 0);
	}

	@Nullable
	public JetType substitute(@NotNull JetType type, @Nullable DeclarationDescriptor ownerDescriptor)
	{
		return unsafeSubstitute(type, ownerDescriptor, 0);
	}

	@NotNull
	private JetType unsafeSubstitute(@NotNull JetType type, @Nullable final DeclarationDescriptor ownerDescriptor, final int recursionDepth)// throws SubstitutionException
	{
		assertRecursionDepth(recursionDepth, type, substitution);
		// The type is within the substitution range, i.e. T or T?

		if(ErrorUtils.isErrorType(type))
			return type;

		return type.accept(new TypeConstructorVisitor<Object, JetType>()
		{
			@Override
			public JetType visitType(JetType type, TypeConstructor t, Object arg)
			{
				JetType replacement = substitution.get(t);

				if(replacement != null)
				{
					boolean resultingIsNullable = type.isNullable() || replacement.isNullable();

					return TypeUtils.makeNullableAsSpecified(replacement, resultingIsNullable);
				}
				else
				{
					// The type is not within the substitution range, i.e. Foo, Bar<T> etc.
					List<JetType> substitutedArguments = substituteTypeArguments(type.getConstructor().getParameters(), ownerDescriptor, type.getArguments(), recursionDepth);

					return new JetTypeImpl(type.getAnnotations(), t, type.isNullable(), substitutedArguments, new SubstitutingScope(type.getMemberScope(), TypeSubstitutor.this));
				}
			}

			@Override
			public JetType visitSelfType(JetType type, SelfTypeConstructor t, Object arg)
			{
				ClassDescriptor classDescriptor = t.getDeclarationDescriptor();

				JetType defaultType = classDescriptor.getDefaultType();

				return new JetTypeImpl(type.getAnnotations(), classDescriptor.getTypeConstructor(), false, defaultType.getArguments(), new SubstitutingScope(type.getMemberScope(), TypeSubstitutor.this));
			}

			@Override
			public JetType visitMethodType(JetType type, MethodTypeConstructor t, Object arg)
			{
				JetType subReturnType = unsafeSubstitute(t.getReturnType(), ownerDescriptor, recursionDepth);
				Map<Name, JetType> parameters = new LinkedHashMap<Name, JetType>(t.getParameterTypes().size());
				for(Map.Entry<Name, JetType> entry : t.getParameterTypes().entrySet())
					parameters.put(entry.getKey(), unsafeSubstitute(entry.getValue(), ownerDescriptor, recursionDepth));
				JetScope scope = new SubstitutingScope(type.getMemberScope(), TypeSubstitutor.this);
				return new JetTypeImpl(type.getAnnotations(), new MethodTypeConstructorImpl(t.getExpectedName(), subReturnType, parameters, scope), type.isNullable(), type.getArguments(), scope);
			}

			@Override
			public JetType visitMultiType(JetType type, MultiTypeConstructor t, Object arg)
			{
				List<MultiTypeEntry> list = new ArrayList<MultiTypeEntry>(t.getEntries().size());
				for(MultiTypeEntry entry : t.getEntries())
					list.add(new MultiTypeEntry(entry.index, entry.mutable,entry.name, unsafeSubstitute(entry.type, ownerDescriptor, recursionDepth)));

				JetScope scope = new SubstitutingScope(type.getMemberScope(), TypeSubstitutor.this);
				return new JetTypeImpl(type.getAnnotations(), new MultiTypeConstructorImpl(list, scope), type.isNullable(), type.getArguments(), scope);
			}
		}, TypeUtils.NO_EXPECTED_TYPE);
	}

	private List<JetType> substituteTypeArguments(List<TypeParameterDescriptor> typeParameters, DeclarationDescriptor ownerDescriptor, List<JetType> typeArguments, int recursionDepth)
	{
		if(typeArguments.isEmpty())
			return Collections.emptyList();
		List<JetType> substitutedArguments = new ArrayList<JetType>(typeArguments.size());
		for(int i = 0; i < typeParameters.size(); i++)
		{
			JetType typeArgument = typeArguments.get(i);

			JetType substitutedTypeArgument = unsafeSubstitute(typeArgument, ownerDescriptor, recursionDepth + 1);

			substitutedArguments.add(substitutedTypeArgument);
		}
		return substitutedArguments;
	}

	private static void assertRecursionDepth(int recursionDepth, JetType projection, TypeSubstitution substitution)
	{
		if(recursionDepth > MAX_RECURSION_DEPTH)
		{
			throw new IllegalStateException("Recursion too deep. Most likely infinite loop while substituting " +
					safeToString(projection) +
					"; substitution: " +
					safeToString(substitution));
		}
	}

	private static String safeToString(Object o)
	{
		try
		{
			return o.toString();
		}
		catch(ProcessCanceledException e)
		{
			throw e;
		}
		catch(Throwable e)
		{
			return "[Exception while computing toString(): " + e + "]";
		}
	}
}
