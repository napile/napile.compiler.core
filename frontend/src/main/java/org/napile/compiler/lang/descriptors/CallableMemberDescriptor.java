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

import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public interface CallableMemberDescriptor extends CallableDescriptor, DeclarationDescriptorNonRoot, DeclarationDescriptorWithVisibility
{
	@NotNull
	@Override
	Set<? extends CallableMemberDescriptor> getOverriddenDescriptors();

	@NotNull
	@Override
	CallableMemberDescriptor getOriginal();

	void addOverriddenDescriptor(@NotNull CallableMemberDescriptor overridden);

	enum Kind
	{
		DECLARATION,
		CREATED_BY_PLUGIN,
		FAKE_OVERRIDE,
		DELEGATION;

		public boolean isReal()
		{
			return this == DECLARATION || this == DELEGATION;
		}
	}

	/**
	 * Is this a real function or function projection.
	 */
	@NotNull
	Kind getKind();

	@NotNull
	CallableMemberDescriptor copy(DeclarationDescriptor newOwner, Modality modality, boolean makeInvisible, Kind kind, boolean copyOverrides);
}
