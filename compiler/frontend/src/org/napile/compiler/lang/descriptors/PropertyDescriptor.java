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
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.OverridingUtil;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.resolve.scopes.receivers.TransientReceiver;
import org.napile.compiler.lang.types.DescriptorSubstitutor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeSubstitutor;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author abreslav
 */
public class PropertyDescriptor extends VariableDescriptorImpl implements CallableMemberDescriptor
{
	private Visibility visibility;

	private final Set<PropertyDescriptor> overriddenProperties = Sets.newLinkedHashSet(); // LinkedHashSet is essential here
	private final PropertyDescriptor original;
	private final Kind kind;

	private ReceiverDescriptor expectedThisObject;
	private List<TypeParameterDescriptor> typeParameters;

	private PropertyDescriptor(@Nullable PropertyDescriptor original, @NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Modality modality, @NotNull Visibility visibility, @NotNull Name name, @NotNull Kind kind, boolean isStatic)
	{
		super(containingDeclaration, annotations, name, modality, isStatic);

		this.visibility = visibility;
		this.original = original == null ? this : original.getOriginal();
		this.kind = kind;
	}

	public PropertyDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Modality modality, @NotNull Visibility visibility, @NotNull Name name, @NotNull Kind kind, boolean isStatic)
	{
		this(null, containingDeclaration, annotations, modality, visibility, name, kind, isStatic);
	}

	public PropertyDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Modality modality, @NotNull Visibility visibility, @NotNull ReceiverDescriptor expectedThisObject, @NotNull Name name, @NotNull JetType outType, @NotNull Kind kind, boolean isStatic)
	{
		this(containingDeclaration, annotations, modality, visibility, name, kind, isStatic);
		setType(outType, Collections.<TypeParameterDescriptor>emptyList(), expectedThisObject);
	}

	public void setType(@NotNull JetType outType, @NotNull List<? extends TypeParameterDescriptor> typeParameters, @NotNull ReceiverDescriptor expectedThisObject)
	{
		setOutType(outType);

		this.typeParameters = Lists.newArrayList(typeParameters);

		this.expectedThisObject = isStatic ? ReceiverDescriptor.NO_RECEIVER : expectedThisObject;
	}

	public void setVisibility(@NotNull Visibility visibility)
	{
		this.visibility = visibility;
	}

	@NotNull
	@Override
	public List<TypeParameterDescriptor> getTypeParameters()
	{
		return typeParameters;
	}

	@NotNull
	@Override
	public ReceiverDescriptor getExpectedThisObject()
	{
		return expectedThisObject;
	}

	@NotNull
	@Override
	public JetType getReturnType()
	{
		return getType();
	}

	@NotNull
	@Override
	public Visibility getVisibility()
	{
		return visibility;
	}

	@Override
	public PropertyDescriptor substitute(TypeSubstitutor originalSubstitutor)
	{
		if(originalSubstitutor.isEmpty())
			return this;

		return doSubstitute(originalSubstitutor, getContainingDeclaration(), getModality(), visibility, true, true, getKind());
	}

	private PropertyDescriptor doSubstitute(TypeSubstitutor originalSubstitutor, DeclarationDescriptor newOwner, Modality newModality, Visibility newVisibility, boolean preserveOriginal, boolean copyOverrides, Kind kind)
	{
		PropertyDescriptor substitutedDescriptor = new PropertyDescriptor(preserveOriginal ? getOriginal() : this, newOwner, getAnnotations(), newModality, newVisibility, getName(), kind, false);

		List<TypeParameterDescriptor> substitutedTypeParameters = Lists.newArrayList();
		TypeSubstitutor substitutor = DescriptorSubstitutor.substituteTypeParameters(getTypeParameters(), originalSubstitutor, substitutedDescriptor, substitutedTypeParameters);

		JetType originalOutType = getType();
		JetType outType = substitutor.substitute(originalOutType);
		if(outType == null)
		{
			return null; // TODO : tell the user that the property was projected out
		}

		ReceiverDescriptor substitutedExpectedThisObject;
		if(expectedThisObject.exists())
		{
			JetType substitutedExpectedThisObjectType = substitutor.substitute(getExpectedThisObject().getType());
			substitutedExpectedThisObject = new TransientReceiver(substitutedExpectedThisObjectType);
		}
		else
		{
			substitutedExpectedThisObject = ReceiverDescriptor.NO_RECEIVER;
		}

		substitutedDescriptor.setType(outType, substitutedTypeParameters, substitutedExpectedThisObject);

		if(copyOverrides)
		{
			for(PropertyDescriptor propertyDescriptor : overriddenProperties)
			{
				OverridingUtil.bindOverride(substitutedDescriptor, propertyDescriptor.substitute(substitutor));
			}
		}

		return substitutedDescriptor;
	}

	@Override
	public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data)
	{
		return visitor.visitPropertyDescriptor(this, data);
	}

	@NotNull
	@Override
	public PropertyDescriptor getOriginal()
	{
		return original;
	}

	@Override
	public Kind getKind()
	{
		return kind;
	}

	@Override
	public void addOverriddenDescriptor(@NotNull CallableMemberDescriptor overridden)
	{
		overriddenProperties.add((PropertyDescriptor) overridden);
	}

	@NotNull
	@Override
	public Set<? extends PropertyDescriptor> getOverriddenDescriptors()
	{
		return overriddenProperties;
	}

	@NotNull
	@Override
	public PropertyDescriptor copy(DeclarationDescriptor newOwner, Modality modality, boolean makeInvisible, Kind kind, boolean copyOverrides)
	{
		return doSubstitute(TypeSubstitutor.EMPTY, newOwner, modality, makeInvisible ? Visibility.INVISIBLE_FAKE : visibility, false, copyOverrides, kind);
	}
}
