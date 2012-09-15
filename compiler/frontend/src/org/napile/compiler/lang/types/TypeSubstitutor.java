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

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.resolve.scopes.SubstitutingScope;
import org.napile.compiler.lang.types.impl.JetTypeImpl;
import com.google.common.collect.Lists;
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

	private static final class SubstitutionException extends Exception
	{
		public SubstitutionException(String message)
		{
			super(message);
		}
	}

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
		if(isEmpty())
		{
			return type;
		}

		try
		{
			return unsafeSubstitute(type, 0);
		}
		catch(SubstitutionException e)
		{
			return ErrorUtils.createErrorType(e.getMessage());
		}
	}

	@Nullable
	public JetType substitute(@NotNull JetType type)
	{
		if(isEmpty())
		{
			return type;
		}

		try
		{
			return unsafeSubstitute(type, 0);
		}
		catch(SubstitutionException e)
		{
			return null;
		}
	}

	@NotNull
	private JetType unsafeSubstitute(@NotNull JetType type, int recursionDepth) throws SubstitutionException
	{
		assertRecursionDepth(recursionDepth, type, substitution);
		// The type is within the substitution range, i.e. T or T?

		if(false || ErrorUtils.isErrorType(type))
			return type;

		JetType replacement = substitution.get(type.getConstructor());

		if(replacement != null)
		{
			boolean resultingIsNullable = type.isNullable() || replacement.isNullable();

			return TypeUtils.makeNullableAsSpecified(replacement, resultingIsNullable);
		}
		else
		{
			// The type is not within the substitution range, i.e. Foo, Bar<T> etc.
			List<JetType> substitutedArguments = substituteTypeArguments(type.getConstructor().getParameters(), type.getArguments(), recursionDepth);

			JetType substitutedType = new JetTypeImpl(type.getAnnotations(),   // Old annotations. This is questionable
					type.getConstructor(),   // The same constructor
					type.isNullable(),       // Same nullability
					substitutedArguments, new SubstitutingScope(type.getMemberScope(), this));
			return substitutedType;
		}
	}

	private List<JetType> substituteTypeArguments(List<TypeParameterDescriptor> typeParameters, List<JetType> typeArguments, int recursionDepth) throws SubstitutionException
	{
		List<JetType> substitutedArguments = Lists.newArrayList();
		for(int i = 0; i < typeParameters.size(); i++)
		{
			TypeParameterDescriptor typeParameter = typeParameters.get(i);
			JetType typeArgument = typeArguments.get(i);

			JetType substitutedTypeArgument = unsafeSubstitute(typeArgument, recursionDepth + 1);

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
