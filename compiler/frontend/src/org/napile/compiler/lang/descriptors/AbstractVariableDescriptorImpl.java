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
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;

/**
 * @author abreslav
 */
public abstract class AbstractVariableDescriptorImpl extends DeclarationDescriptorNonRootImpl implements VariableDescriptor
{
	private JetType outType;
	private final boolean isStatic;
	private final boolean mutable;
	private final Modality modality;

	public AbstractVariableDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, @Nullable JetType outType, @NotNull Modality modality, boolean isStatic, boolean mutable)
	{
		super(containingDeclaration, annotations, name);

		if(outType != null)
			setOutType(outType);
		this.isStatic = isStatic;
		this.modality = modality;
		this.mutable = mutable;
	}

	protected AbstractVariableDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, @NotNull Modality modality, boolean isStatic, boolean mutable)
	{
		this(containingDeclaration, annotations, name, null, modality, isStatic, mutable);
	}

	@Override
	public boolean isStatic()
	{
		return isStatic;
	}

	@NotNull
	@Override
	public JetType getType()
	{
		return outType;
	}

	@Override
	@NotNull
	public Modality getModality()
	{
		return modality;
	}

	@Override
	public boolean isMutable()
	{
		return mutable;
	}

	public void setOutType(@NotNull JetType type)
	{
		assert this.outType == null;
		outType = type;
	}

	@Override
	@NotNull
	public VariableDescriptor getOriginal()
	{
		return (VariableDescriptor) super.getOriginal();
	}

	@Override
	public void addOverriddenDescriptor(@NotNull CallableMemberDescriptor overridden)
	{
	}

	@NotNull
	@Override
	public Kind getKind()
	{
		return Kind.DECLARATION;
	}

	@NotNull
	@Override
	public CallableMemberDescriptor copy(DeclarationDescriptor newOwner, Modality modality, boolean makeInvisible, Kind kind, boolean copyOverrides)
	{
		return null;
	}

	@NotNull
	@Override
	public List<CallParameterDescriptor> getValueParameters()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public Set<? extends VariableDescriptor> getOverriddenDescriptors()
	{
		return Collections.emptySet();
	}

	@NotNull
	@Override
	public List<TypeParameterDescriptor> getTypeParameters()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public ReceiverDescriptor getExpectedThisObject()
	{
		return ReceiverDescriptor.NO_RECEIVER;
	}

	@NotNull
	@Override
	public JetType getReturnType()
	{
		return getType();
	}

	@Override
	public boolean isEnumValue()
	{
		return false;
	}
}
