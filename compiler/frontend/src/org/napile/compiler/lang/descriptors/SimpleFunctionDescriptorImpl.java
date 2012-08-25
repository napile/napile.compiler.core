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
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.types.TypeSubstitutor;

/**
 * @author Stepan Koltsov
 */
public class SimpleFunctionDescriptorImpl extends FunctionDescriptorImpl implements SimpleFunctionDescriptor
{
	public SimpleFunctionDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, Kind kind, boolean isStatic)
	{
		super(containingDeclaration, annotations, name, kind, isStatic);
	}

	private SimpleFunctionDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull SimpleFunctionDescriptor original, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, Kind kind, boolean isStatic)
	{
		super(containingDeclaration, original, annotations, name, kind, isStatic);
	}

	@NotNull
	@Override
	public SimpleFunctionDescriptor getOriginal()
	{
		return (SimpleFunctionDescriptor) super.getOriginal();
	}

	@Override
	protected FunctionDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal, Kind kind)
	{
		if(preserveOriginal)
		{
			return new SimpleFunctionDescriptorImpl(newOwner, getOriginal(),
					// TODO : safeSubstitute
					getAnnotations(), getName(), kind, false);
		}
		else
		{
			return new SimpleFunctionDescriptorImpl(newOwner,
					// TODO : safeSubstitute
					getAnnotations(), getName(), kind, false);
		}
	}

	@NotNull
	@Override
	public SimpleFunctionDescriptor copy(DeclarationDescriptor newOwner, Modality modality, boolean makeInvisible, Kind kind, boolean copyOverrides)
	{
		return (SimpleFunctionDescriptorImpl) doSubstitute(TypeSubstitutor.EMPTY, newOwner, modality, makeInvisible ? Visibilities.INVISIBLE_FAKE : visibility, false, copyOverrides, kind);
	}
}
