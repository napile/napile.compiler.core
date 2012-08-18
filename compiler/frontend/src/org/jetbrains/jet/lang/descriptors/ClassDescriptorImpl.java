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

package org.jetbrains.jet.lang.descriptors;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetDelegationSpecifierListOwner;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.SubstitutingScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.SubstitutionUtils;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeConstructorImpl;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.TypeUtils;

/**
 * @author abreslav
 */
public class ClassDescriptorImpl extends DeclarationDescriptorNonRootImpl implements ClassDescriptorFromSource
{
	private TypeConstructor typeConstructor;

	private JetScope memberDeclarations;
	private Map<JetDelegationSpecifierListOwner, ConstructorDescriptor> constructors;
	private ConstructorDescriptor primaryConstructor;
	private ReceiverDescriptor implicitReceiver;
	private final Modality modality;

	public ClassDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Modality modality, @NotNull Name name)
	{
		super(containingDeclaration, annotations, name);
		this.modality = modality;
	}

	public final ClassDescriptorImpl initialize(boolean sealed, @NotNull List<? extends TypeParameterDescriptor> typeParameters, @NotNull Collection<JetType> supertypes, @NotNull JetScope memberDeclarations, @NotNull Map<JetDelegationSpecifierListOwner, ConstructorDescriptor> constructors)
	{
		this.typeConstructor = new TypeConstructorImpl(this, getAnnotations(), sealed, getName().getName(), typeParameters, supertypes);
		this.memberDeclarations = memberDeclarations;
		this.constructors = constructors;
		this.primaryConstructor = primaryConstructor;
		return this;
	}

	public void setPrimaryConstructor(@NotNull ConstructorDescriptor primaryConstructor)
	{
		this.primaryConstructor = primaryConstructor;
	}

	@Override
	@NotNull
	public TypeConstructor getTypeConstructor()
	{
		return typeConstructor;
	}

	@Override
	@NotNull
	public JetScope getMemberScope(List<TypeProjection> typeArguments)
	{
		assert typeArguments.size() == typeConstructor.getParameters().size() : typeArguments;
		if(typeConstructor.getParameters().isEmpty())
		{
			return memberDeclarations;
		}
		Map<TypeConstructor, TypeProjection> substitutionContext = SubstitutionUtils.buildSubstitutionContext(typeConstructor.getParameters(), typeArguments);
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
	public Map<JetDelegationSpecifierListOwner, ConstructorDescriptor> getConstructors()
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
	public ConstructorDescriptor getUnsubstitutedPrimaryConstructor()
	{
		return primaryConstructor;
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
