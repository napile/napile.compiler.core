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

package org.napile.compiler.lang.descriptors;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.SubstitutingScope;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.SubstitutionUtils;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeSubstitutor;
import org.napile.compiler.lang.types.TypeUtils;

/**
 * @author abreslav
 */
public abstract class ClassDescriptorBase implements ClassDescriptor
{

	protected JetType defaultType;

	protected abstract JetScope getScopeForMemberLookup();

	private final boolean isStatic;

	protected ClassDescriptorBase(boolean isStatic)
	{
		this.isStatic = isStatic;
	}

	@Override
	public boolean isStatic()
	{
		return isStatic;
	}

	@NotNull
	@Override
	public JetScope getMemberScope(List<JetType> typeArguments)
	{
		assert typeArguments.size() == getTypeConstructor().getParameters().size();
		if(typeArguments.isEmpty())
			return getScopeForMemberLookup();

		List<TypeParameterDescriptor> typeParameters = getTypeConstructor().getParameters();
		Map<TypeConstructor, JetType> substitutionContext = SubstitutionUtils.buildSubstitutionContext(typeParameters, typeArguments);

		// Unsafe substitutor is OK, because no recursion can hurt us upon a trivial substitution:
		// all the types are written explicitly in the code already, they can not get infinite.
		// One exception is *-projections, but they need to be handled separately anyways.
		TypeSubstitutor substitutor = TypeSubstitutor.createUnsafe(substitutionContext);
		return new SubstitutingScope(getScopeForMemberLookup(), substitutor);
	}

	@NotNull
	@Override
	public ClassDescriptor substitute(TypeSubstitutor substitutor)
	{
		if(substitutor.isEmpty())
		{
			return this;
		}
		return new LazySubstitutingClassDescriptor(this, substitutor, false);
	}

	@NotNull
	@Override
	public JetType getDefaultType()
	{
		if(defaultType == null)
		{
			defaultType = TypeUtils.makeUnsubstitutedType(this, getScopeForMemberLookup());
		}
		return defaultType;
	}

	@Override
	public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor)
	{
		visitor.visitClassDescriptor(this, null);
	}

	@Override
	public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data)
	{
		return visitor.visitClassDescriptor(this, data);
	}
}
