/*
 * Copyright 2010-2012 napile.org
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
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeSubstitutor;

/**
 * @author VISTALL
 * @date 13:56/06.09.12
 */
public class ReferenceParameterDescriptor implements ParameterDescriptor
{
	private final int index;
	private final DeclarationDescriptor ownerDescriptor;
	private JetType type;
	private Name name;
	private PropertyDescriptor referenceProperty;

	public ReferenceParameterDescriptor(int index, DeclarationDescriptor ownerDescriptor)
	{
		this.index = index;
		this.ownerDescriptor = ownerDescriptor;
	}

	public void initialize(@NotNull JetType type, @NotNull Name name, @Nullable PropertyDescriptor referenceProperty)
	{
		this.type = type;
		this.name = name;
		this.referenceProperty = referenceProperty;
	}

	@Override
	public int getIndex()
	{
		return index;
	}

	@Override
	public boolean hasDefaultValue()
	{
		return false;
	}

	@Override
	public boolean declaresDefaultValue()
	{
		return false;
	}

	@Nullable
	@Override
	public JetType getVarargElementType()
	{
		return null;
	}

	@NotNull
	@Override
	public JetType getType()
	{
		return type;
	}

	@NotNull
	@Override
	public DeclarationDescriptor getContainingDeclaration()
	{
		return ownerDescriptor;
	}

	@Override
	public VariableDescriptor substitute(TypeSubstitutor substitutor)
	{
		return null;
	}

	@Override
	public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data)
	{
		return visitor.visitReferenceParameterDescriptor(this, data);
	}

	@Override
	public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor)
	{
		accept(visitor, null);
	}

	@NotNull
	@Override
	public List<ParameterDescriptor> getValueParameters()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public PropertyKind getPropertyKind()
	{
		return PropertyKind.VAL;
	}

	@NotNull
	@Override
	public ReceiverDescriptor getReceiverParameter()
	{
		return ReceiverDescriptor.NO_RECEIVER;
	}

	@NotNull
	@Override
	public ReceiverDescriptor getExpectedThisObject()
	{
		return ReceiverDescriptor.NO_RECEIVER;
	}

	@NotNull
	@Override
	public List<TypeParameterDescriptor> getTypeParameters()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public JetType getReturnType()
	{
		return getType();
	}

	@NotNull
	@Override
	public ParameterDescriptor getOriginal()
	{
		return this;
	}

	@NotNull
	@Override
	public ParameterDescriptor copy(DeclarationDescriptor newOwner)
	{
		return this;
	}

	@NotNull
	@Override
	public Set<? extends ParameterDescriptor> getOverriddenDescriptors()
	{
		return Collections.emptySet();
	}

	@NotNull
	@Override
	public MethodDescriptor getCallableDescriptor()
	{
		throw new IllegalArgumentException();
	}

	@Override
	public void addOverriddenDescriptor(@NotNull ParameterDescriptor overridden)
	{
	}

	@NotNull
	@Override
	public Visibility getVisibility()
	{
		return Visibility.PUBLIC;
	}

	@Override
	public boolean isStatic()
	{
		return false;
	}

	@Override
	public List<AnnotationDescriptor> getAnnotations()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public Name getName()
	{
		return name;
	}

	public PropertyDescriptor getReferenceProperty()
	{
		return referenceProperty;
	}
}
