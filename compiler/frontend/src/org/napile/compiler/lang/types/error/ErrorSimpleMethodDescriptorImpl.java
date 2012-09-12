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

package org.napile.compiler.lang.types.error;

import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptorImpl;
import org.napile.compiler.lang.descriptors.Modality;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptorImpl;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.types.ErrorUtils;

/**
 * @author Stepan Koltsov
 */
public class ErrorSimpleMethodDescriptorImpl extends SimpleMethodDescriptorImpl
{
	// used for diagnostic only
	@NotNull
	private final ErrorUtils.ErrorScope ownerScope;

	public ErrorSimpleMethodDescriptorImpl(ErrorUtils.ErrorScope ownerScope)
	{
		super(ErrorUtils.getErrorClass(), Collections.<AnnotationDescriptor>emptyList(), Name.special("<ERROR METHOD>"), Kind.DECLARATION, false, false);
		this.ownerScope = ownerScope;
	}

	@Override
	protected MethodDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal, Kind kind)
	{
		return this;
	}

	@NotNull
	@Override
	public SimpleMethodDescriptor copy(DeclarationDescriptor newOwner, Modality modality, boolean makeInvisible, Kind kind, boolean copyOverrides)
	{
		return this;
	}

	@Override
	public void addOverriddenDescriptor(@NotNull CallableMemberDescriptor overriddenFunction)
	{
		// nop
	}
}
