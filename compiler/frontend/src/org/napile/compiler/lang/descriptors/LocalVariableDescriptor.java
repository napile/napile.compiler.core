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
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeSubstitutor;

/**
 * @author abreslav
 */
public class LocalVariableDescriptor extends VariableDescriptorImpl
{
	private final PropertyKind propertyKind;

	public LocalVariableDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, @Nullable JetType type, PropertyKind propertyKind)
	{
		super(containingDeclaration, annotations, name, type, false);
		this.propertyKind = propertyKind;
	}

	@NotNull
	@Override
	public LocalVariableDescriptor substitute(TypeSubstitutor substitutor)
	{
		throw new UnsupportedOperationException(); // TODO
	}

	@Override
	public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data)
	{
		return visitor.visitLocalVariableDescriptor(this, data);
	}

	@NotNull
	@Override
	public PropertyKind getPropertyKind()
	{
		return propertyKind;
	}

	@NotNull
	@Override
	public Visibility getVisibility()
	{
		return Visibility.LOCAL2;
	}
}
