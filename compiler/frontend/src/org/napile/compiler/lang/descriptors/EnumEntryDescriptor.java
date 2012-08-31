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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.types.TypeSubstitutor;

/**
 * @author VISTALL
 * @date 20:44/28.08.12
 */
public class EnumEntryDescriptor extends VariableDescriptorImpl implements CallableMemberDescriptor
{
	private MutableClassDescriptor classDescriptor;

	public EnumEntryDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull Name name)
	{
		super(containingDeclaration, new ArrayList<AnnotationDescriptor>(), name, null, true);
	}

	@Override
	public VariableDescriptor substitute(TypeSubstitutor substitutor)
	{
		return this;
	}

	@Override
	public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data)
	{
		return visitor.visitEnumEntryDescriptor(this, data);
	}

	@Override
	public boolean isVar()
	{
		return false;
	}

	@Override
	public boolean isObjectDeclaration()
	{
		return false;
	}

	@NotNull
	@Override
	public Visibility getVisibility()
	{
		return Visibility.PUBLIC;
	}

	@Override
	@NotNull
	public EnumEntryDescriptor getOriginal()
	{
		return this;
	}

	@NotNull
	@Override
	public Set<? extends CallableMemberDescriptor> getOverriddenDescriptors()
	{
		return Collections.emptySet();
	}

	@Override
	public void addOverriddenDescriptor(@NotNull CallableMemberDescriptor overridden)
	{
	}

	@Override
	public Kind getKind()
	{
		return Kind.DECLARATION;
	}

	@NotNull
	@Override
	public CallableMemberDescriptor copy(DeclarationDescriptor newOwner, Modality modality, boolean makeInvisible, Kind kind, boolean copyOverrides)
	{
		throw new IllegalArgumentException();
	}

	@NotNull
	@Override
	public Modality getModality()
	{
		return Modality.OPEN;
	}

	public MutableClassDescriptor getClassDescriptor()
	{
		return classDescriptor;
	}

	public void setClassDescriptor(MutableClassDescriptor classDescriptor)
	{
		this.classDescriptor = classDescriptor;
	}
}
