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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;

/**
 * @author abreslav
 */
public class ConstructorDescriptor extends MethodDescriptorImpl
{
	public static final Name NAME = Name.identifier("this");

	public ConstructorDescriptor(@NotNull ClassDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, boolean isStatic)
	{
		super(containingDeclaration, annotations, NAME, Kind.DECLARATION, isStatic, false);
	}

	public ConstructorDescriptor(@NotNull ClassDescriptor containingDeclaration, @NotNull ConstructorDescriptor original, @NotNull List<AnnotationDescriptor> annotations, boolean isStatic)
	{
		super(containingDeclaration, original, annotations, NAME, Kind.DECLARATION, isStatic, false);
	}

	public ConstructorDescriptor initialize(@NotNull List<TypeParameterDescriptor> typeParameters, @NotNull List<ParameterDescriptor> unsubstitutedValueParameters, Visibility visibility)
	{
		super.initialize(null, isStatic() ? ReceiverDescriptor.NO_RECEIVER : getExpectedThisObject(getContainingDeclaration()), typeParameters, unsubstitutedValueParameters, null, Modality.FINAL, visibility);
		return this;
	}

	@NotNull
	private static ReceiverDescriptor getExpectedThisObject(@NotNull ClassDescriptor descriptor)
	{
		DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
		return DescriptorUtils.getExpectedThisObjectIfNeeded(containingDeclaration);
	}

	@NotNull
	@Override
	public ClassDescriptor getContainingDeclaration()
	{
		return (ClassDescriptor) super.getContainingDeclaration();
	}

	@NotNull
	@Override
	public ConstructorDescriptor getOriginal()
	{
		return (ConstructorDescriptor) super.getOriginal();
	}

	@Override
	public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data)
	{
		return visitor.visitConstructorDescriptor(this, data);
	}

	@NotNull
	@Override
	public Set<? extends MethodDescriptor> getOverriddenDescriptors()
	{
		return Collections.emptySet();
	}

	@Override
	public void addOverriddenDescriptor(@NotNull CallableMemberDescriptor overriddenFunction)
	{
		throw new UnsupportedOperationException("Constructors cannot override anything");
	}

	@Override
	protected MethodDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal, Kind kind)
	{
		if(kind != Kind.DECLARATION)
		{
			throw new IllegalStateException();
		}
		return new ConstructorDescriptor((ClassDescriptor) newOwner, this, Collections.<AnnotationDescriptor>emptyList(), false);//TODO annotation list
	}

	@NotNull
	@Override
	public ConstructorDescriptor copy(DeclarationDescriptor newOwner, Modality modality, boolean makeInvisible, Kind kind, boolean copyOverrides)
	{
		throw new UnsupportedOperationException("Constructors should not be copied for overriding");
	}

	@Override
	public boolean isStatic()
	{
		return false;
	}
}
