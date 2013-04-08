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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.types.TypeSubstitutor;

/**
 * @author VISTALL
 * @since 19:01/05.12.12
 */
public class VariableAccessorDescriptorImpl extends AbstractMethodDescriptorImpl implements VariableAccessorDescriptor
{
	private final boolean isDefault;
	private final VariableDescriptor variableDescriptor;

	public VariableAccessorDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, Kind kind, boolean isStatic, boolean isNative, boolean isDefault, VariableDescriptor variableDescriptor)
	{
		super(containingDeclaration, annotations, name, kind, isStatic, isNative);
		this.isDefault = isDefault;
		this.variableDescriptor = variableDescriptor;
	}

	public VariableAccessorDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull MethodDescriptor original, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, Kind kind, boolean isStatic, boolean isNative, boolean isDefault, VariableDescriptor variableDescriptor)
	{
		super(containingDeclaration, original, annotations, name, kind, isStatic, isNative);
		this.isDefault = isDefault;
		this.variableDescriptor = variableDescriptor;
	}

	@Override
	protected VariableAccessorDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal, Kind kind)
	{
		return preserveOriginal ? new VariableAccessorDescriptorImpl(newOwner, getOriginal(), getAnnotations(), getName(), kind, isStatic(), isNative(), isDefault(), variableDescriptor) : new VariableAccessorDescriptorImpl(newOwner ,getAnnotations(), getName(), kind, isStatic(), isNative(), isDefault(), variableDescriptor);
	}

	@NotNull
	@Override
	public VariableAccessorDescriptorImpl copy(DeclarationDescriptor newOwner, Modality modality, boolean makeInvisible, Kind kind, boolean copyOverrides)
	{
		return (VariableAccessorDescriptorImpl) doSubstitute(TypeSubstitutor.EMPTY, newOwner, modality, makeInvisible ? Visibility.INVISIBLE_FAKE : visibility, false, copyOverrides, kind);
	}

	@Override
	public boolean isDefault()
	{
		return isDefault;
	}

	@NotNull
	@Override
	public VariableDescriptor getVariable()
	{
		return variableDescriptor;
	}
}
