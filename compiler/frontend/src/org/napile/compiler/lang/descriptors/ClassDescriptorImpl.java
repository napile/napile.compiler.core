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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.psi.NapileDelegationSpecifierListOwner;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.SubstitutingScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ClassReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.SubstitutionUtils;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeConstructorImpl;
import org.napile.compiler.lang.types.TypeSubstitutor;
import org.napile.compiler.lang.types.TypeUtils;

/**
 * @author abreslav
 */
public class ClassDescriptorImpl extends DeclarationDescriptorNonRootImpl implements ClassDescriptor
{
	private TypeConstructor typeConstructor;

	private JetScope memberDeclarations;
	private Map<NapileDelegationSpecifierListOwner, ConstructorDescriptor> constructors;

	private ReceiverDescriptor implicitReceiver;
	private final Modality modality;
	private final boolean isStatic;

	public ClassDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Modality modality, @NotNull Name name, boolean isStatic)
	{
		super(containingDeclaration, annotations, name);
		this.modality = modality;
		this.isStatic = isStatic;
	}

	public final ClassDescriptorImpl initialize(boolean sealed, @NotNull List<? extends TypeParameterDescriptor> typeParameters, @NotNull Collection<JetType> supertypes, @NotNull JetScope memberDeclarations, @NotNull Map<NapileDelegationSpecifierListOwner, ConstructorDescriptor> constructors)
	{
		this.typeConstructor = new TypeConstructorImpl(this, getAnnotations(), sealed, getName().getName(), typeParameters, supertypes);
		this.memberDeclarations = memberDeclarations;
		this.constructors = constructors;

		return this;
	}

	@Override
	public boolean isStatic()
	{
		return isStatic;
	}

	@Override
	@NotNull
	public TypeConstructor getTypeConstructor()
	{
		return typeConstructor;
	}

	@Override
	@NotNull
	public JetScope getMemberScope(List<JetType> typeArguments)
	{
		assert typeArguments.size() == typeConstructor.getParameters().size() : typeArguments;
		if(typeConstructor.getParameters().isEmpty())
		{
			return memberDeclarations;
		}
		Map<TypeConstructor, JetType> substitutionContext = SubstitutionUtils.buildSubstitutionContext(typeConstructor.getParameters(), typeArguments);
		return new SubstitutingScope(memberDeclarations, TypeSubstitutor.create(substitutionContext));
	}

	@NotNull
	@Override
	public JetType getDefaultType()
	{
		return TypeUtils.makeUnsubstitutedType(this, memberDeclarations);
	}

	@NotNull
	@Override
	public Map<NapileDelegationSpecifierListOwner, ConstructorDescriptor> getConstructors()
	{
		return constructors;
	}

	@NotNull
	@Override
	public ClassDescriptor substitute(TypeSubstitutor substitutor)
	{
		throw new UnsupportedOperationException(); // TODO
	}

	@Override
	public JetType getClassObjectType()
	{
		return null;
	}

	@NotNull
	@Override
	public Collection<JetType> getSupertypes()
	{
		return Collections.emptyList();
	}

	@Override
	public ClassDescriptor getClassObjectDescriptor()
	{
		return null;
	}

	@NotNull
	@Override
	public ClassKind getKind()
	{
		return ClassKind.CLASS;
	}

	@Override
	public boolean isClassObjectAValue()
	{
		return true;
	}

	@Override
	public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data)
	{
		return visitor.visitClassDescriptor(this, data);
	}

	@Override
	@NotNull
	public Modality getModality()
	{
		return modality;
	}

	@NotNull
	@Override
	public Visibility getVisibility()
	{
		return Visibilities.PUBLIC;
	}

	@NotNull
	@Override
	public ReceiverDescriptor getImplicitReceiver()
	{
		if(implicitReceiver == null)
		{
			implicitReceiver = new ClassReceiver(this);
		}
		return implicitReceiver;
	}

	@NotNull
	@Override
	public JetScope getUnsubstitutedInnerClassesScope()
	{
		return JetScope.EMPTY;
	}
}
