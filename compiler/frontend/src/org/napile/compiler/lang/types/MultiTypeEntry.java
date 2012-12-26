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

package org.napile.compiler.lang.types;

import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.Modality;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptorImpl;
import org.napile.compiler.lang.descriptors.Visibility;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;

/**
 * @author VISTALL
 * @date 20:12/26.12.12
 */
public class MultiTypeEntry
{
	@Nullable
	public final Boolean mutable;
	@Nullable
	public final Name name;
	@NotNull
	public final JetType type;

	public final VariableDescriptor descriptor;

	public MultiTypeEntry(@Nullable Boolean mutable, @Nullable Name name, @NotNull JetType type)
	{
		this.mutable = mutable;
		this.name = name;
		this.type = type;

		if(name != null)
		{
			VariableDescriptorImpl d = new VariableDescriptorImpl(DeclarationDescriptor.EMPTY, Collections.<AnnotationDescriptor>emptyList(), Modality.OPEN, Visibility.PUBLIC, name, CallableMemberDescriptor.Kind.DECLARATION, false, mutable);
			d.setType(type, Collections.<TypeParameterDescriptor>emptyList(), ReceiverDescriptor.NO_RECEIVER);
			descriptor = d;
		}
		else
			descriptor = null;
	}
}
