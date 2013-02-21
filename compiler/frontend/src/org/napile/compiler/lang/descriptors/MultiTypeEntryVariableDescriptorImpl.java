/*
 * Copyright 2010-2013 napile.org
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;

/**
 * @author VISTALL
 * @date 18:19/05.01.13
 */
public class MultiTypeEntryVariableDescriptorImpl extends VariableDescriptorImpl implements MultiTypeEntryVariableDescriptor
{
	private final int index;

	protected MultiTypeEntryVariableDescriptorImpl(@Nullable VariableDescriptorImpl original, @NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Modality modality, @NotNull Visibility visibility, @NotNull Name name, @NotNull Kind kind, boolean isStatic, boolean mutable, int index)
	{
		super(original, containingDeclaration, annotations, modality, visibility, name, kind, isStatic, mutable, false);
		this.index = index;
	}

	public MultiTypeEntryVariableDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Modality modality, @NotNull Visibility visibility, @NotNull Name name, @NotNull Kind kind, boolean isStatic, boolean mutable, int index)
	{
		super(containingDeclaration, annotations, modality, visibility, name, kind, isStatic, mutable, false);
		this.index = index;
	}

	public MultiTypeEntryVariableDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Modality modality, @NotNull Visibility visibility, @NotNull ReceiverDescriptor expectedThisObject, @NotNull Name name, @NotNull JetType outType, @NotNull Kind kind, boolean isStatic, boolean mutable, int index)
	{
		super(containingDeclaration, annotations, modality, visibility, expectedThisObject, name, outType, kind, isStatic, mutable, false);
		this.index = index;
	}

	@Override
	protected VariableDescriptorImpl createInstance(DeclarationDescriptor newOwner, Modality newModality, Visibility newVisibility, boolean preserveOriginal, Kind kind)
	{
		return new MultiTypeEntryVariableDescriptorImpl(preserveOriginal ? getOriginal() : this, newOwner, getAnnotations(), newModality, newVisibility, getName(), kind, isStatic(), isMutable(), index);
	}

	@Override
	public int getIndex()
	{
		return index;
	}
}
