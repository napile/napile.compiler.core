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
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.types.NapileType;
import com.google.common.collect.Lists;

/**
 * @author abreslav
 */
public class CallParameterAsVariableDescriptorImpl extends AbstractCallParameterDescriptorImpl
{
	private final boolean ref;

	public CallParameterAsVariableDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, int index, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, @Nullable NapileType outType, @NotNull Modality modality, boolean mutable, boolean ref)
	{
		super(containingDeclaration, index, annotations, name, outType, modality, mutable);
		this.ref = ref;
	}

	protected CallParameterAsVariableDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull CallParameterDescriptor original, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, @Nullable NapileType outType, @NotNull Modality modality, boolean mutable, boolean ref)
	{
		super(containingDeclaration, original, annotations, name, modality, mutable);
		this.ref = ref;
		if(outType != null)
			setOutType(outType);
	}

	@Override
	public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data)
	{
		return visitor.visitCallParameterAsVariableDescriptor(this, data);
	}

	@Override
	public boolean isRef()
	{
		return ref;
	}

	@NotNull
	@Override
	public CallParameterDescriptor copy(@NotNull DeclarationDescriptor newOwner)
	{
		CallParameterAsVariableDescriptorImpl c =  new CallParameterAsVariableDescriptorImpl(newOwner, index, Lists.newArrayList(getAnnotations()), getName(), getType(), getModality(), isMutable(), ref);
		c.hasDefaultValue = hasDefaultValue;
		return c;
	}
}
