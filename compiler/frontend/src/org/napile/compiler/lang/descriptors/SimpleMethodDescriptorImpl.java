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

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.types.TypeSubstitutor;

/**
 * @author Stepan Koltsov
 */
public class SimpleMethodDescriptorImpl extends AbstractMethodDescriptorImpl implements SimpleMethodDescriptor
{
	private final boolean macro;

	public SimpleMethodDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, Kind kind, boolean isStatic, boolean isNative, boolean macro)
	{
		super(containingDeclaration, annotations, name, kind, isStatic, isNative);
		this.macro = macro;
	}

	private SimpleMethodDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull SimpleMethodDescriptor original, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, Kind kind, boolean isStatic, boolean isNative, boolean macro)
	{
		super(containingDeclaration, original, annotations, name, kind, isStatic, isNative);
		this.macro = macro;
	}

	@NotNull
	@Override
	public SimpleMethodDescriptor getOriginal()
	{
		return (SimpleMethodDescriptor) super.getOriginal();
	}

	@Override
	public boolean isMacro()
	{
		return macro;
	}

	@Override
	protected AbstractMethodDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal, Kind kind)
	{
		if(preserveOriginal)
		{
			return new SimpleMethodDescriptorImpl(newOwner, getOriginal(),
					// TODO : safeSubstitute
					getAnnotations(), getName(), kind, isStatic(), isNative(), isMacro());
		}
		else
		{
			return new SimpleMethodDescriptorImpl(newOwner,
					// TODO : safeSubstitute
					getAnnotations(), getName(), kind, isStatic(), isNative(), isMacro());
		}
	}

	@NotNull
	@Override
	public SimpleMethodDescriptor copy(DeclarationDescriptor newOwner, Modality modality, boolean makeInvisible, Kind kind, boolean copyOverrides)
	{
		return (SimpleMethodDescriptorImpl) doSubstitute(TypeSubstitutor.EMPTY, newOwner, modality, makeInvisible ? Visibility.INVISIBLE_FAKE : visibility, false, copyOverrides, kind);
	}
}
