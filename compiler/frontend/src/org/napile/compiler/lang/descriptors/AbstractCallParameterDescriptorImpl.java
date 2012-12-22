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
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeSubstitutor;
import com.google.common.collect.Sets;

/**
 * @author VISTALL
 * @date 20:34/01.12.12
 */
public abstract class AbstractCallParameterDescriptorImpl extends AbstractVariableDescriptorImpl implements CallParameterDescriptor
{
	protected final int index;

	protected Boolean hasDefaultValue;
	protected boolean declaresDefaultValue;

	protected final Set<CallParameterDescriptor> overriddenDescriptors = Sets.newLinkedHashSet(); // Linked is essential
	protected boolean overriddenDescriptorsLocked = false;
	protected final Set<? extends CallParameterDescriptor> readOnlyOverriddenDescriptors = Collections.unmodifiableSet(overriddenDescriptors);

	protected final CallParameterDescriptor original;

	public AbstractCallParameterDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, int index, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, @Nullable JetType outType, @NotNull Modality modality, boolean mutable)
	{
		super(containingDeclaration, annotations, name, outType, modality, false, mutable);
		this.original = this;
		this.index = index;
	}

	protected AbstractCallParameterDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull CallParameterDescriptor original, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, @NotNull Modality modality, boolean mutable)
	{
		super(containingDeclaration, annotations, name, modality, false, mutable);
		this.original = original;
		this.index = original.getIndex();
	}

	@Override
	public boolean hasDefaultValue()
	{
		computeDefaultValuePresence();
		return hasDefaultValue;
	}

	public void setHasDefaultValue(boolean a)
	{
		hasDefaultValue = a;
	}

	@Override
	public boolean declaresDefaultValue()
	{
		return declaresDefaultValue && ((CallableMemberDescriptor) getContainingDeclaration()).getKind().isReal();
	}

	@NotNull
	@Override
	public Visibility getVisibility()
	{
		return Visibility.LOCAL2;
	}

	@NotNull
	@Override
	public Set<? extends CallParameterDescriptor> getOverriddenDescriptors()
	{
		return readOnlyOverriddenDescriptors;
	}

	@Override
	public void addOverriddenDescriptor(@NotNull CallParameterDescriptor overridden)
	{
		assert !overriddenDescriptorsLocked : "Adding more overridden descriptors is not allowed at this point: " + "the presence of the default value has already been calculated";
		overriddenDescriptors.add(overridden);
	}

	@Override
	public int getIndex()
	{
		return index;
	}

	@NotNull
	@Override
	public CallParameterDescriptor getOriginal()
	{
		return original == this ? this : original.getOriginal();
	}

	@NotNull
	@Override
	public CallParameterDescriptor substitute(TypeSubstitutor substitutor)
	{
		throw new UnsupportedOperationException(); // TODO
	}

	private void computeDefaultValuePresence()
	{
		if(hasDefaultValue != null)
			return;
		overriddenDescriptorsLocked = true;
		if(declaresDefaultValue)
		{
			hasDefaultValue = true;
		}
		else
		{
			for(CallParameterDescriptor descriptor : overriddenDescriptors)
			{
				if(descriptor.hasDefaultValue())
				{
					hasDefaultValue = true;
					return;
				}
			}
			hasDefaultValue = false;
		}
	}
}
