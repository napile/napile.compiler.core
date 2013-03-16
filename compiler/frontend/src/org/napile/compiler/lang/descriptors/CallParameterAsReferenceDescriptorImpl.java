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
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.types.JetType;
import com.google.common.collect.Lists;

/**
 * @author VISTALL
 * @since 13:56/06.09.12
 */
public class CallParameterAsReferenceDescriptorImpl extends AbstractCallParameterDescriptorImpl implements CallParameterAsReferenceDescriptor
{
	private final VariableDescriptor referenceProperty;

	public CallParameterAsReferenceDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, int index, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, @Nullable JetType outType, @NotNull VariableDescriptor referenceProperty)
	{
		super(containingDeclaration, index, annotations, name, outType, Modality.FINAL, false);
		this.referenceProperty = referenceProperty;
	}

	protected CallParameterAsReferenceDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull CallParameterDescriptor original, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, @NotNull VariableDescriptor referenceProperty)
	{
		super(containingDeclaration, original, annotations, name, Modality.FINAL, false);
		this.referenceProperty = referenceProperty;
	}

	@Override
	public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data)
	{
		return visitor.visitCallParameterAsReferenceDescriptor(this, data);
	}

	@Override
	public VariableDescriptor getReferenceVariableDescriptor()
	{
		return referenceProperty;
	}

	@Override
	public boolean isRef()
	{
		return false;
	}

	@NotNull
	@Override
	public CallParameterDescriptor copy(DeclarationDescriptor newOwner)
	{
		CallParameterAsReferenceDescriptorImpl c = new CallParameterAsReferenceDescriptorImpl(newOwner, index, Lists.newArrayList(getAnnotations()), getName(), getType(), referenceProperty);
		c.hasDefaultValue = hasDefaultValue;
		return c;
	}
}
