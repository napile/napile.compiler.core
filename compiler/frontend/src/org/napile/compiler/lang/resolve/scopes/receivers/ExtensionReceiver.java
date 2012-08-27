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

package org.napile.compiler.lang.resolve.scopes.receivers;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.types.JetType;

/**
 * @author abreslav
 */
public class ExtensionReceiver extends AbstractReceiverDescriptor implements ThisReceiverDescriptor
{

	private final CallableDescriptor descriptor;

	public ExtensionReceiver(@NotNull CallableDescriptor callableDescriptor, @NotNull JetType receiverType)
	{
		super(receiverType);
		this.descriptor = callableDescriptor;
	}

	@NotNull
	@Override
	public DeclarationDescriptor getDeclarationDescriptor()
	{
		return descriptor;
	}

	@Override
	public <R, D> R accept(@NotNull ReceiverDescriptorVisitor<R, D> visitor, D data)
	{
		return visitor.visitExtensionReceiver(this, data);
	}

	@Override
	public String toString()
	{
		return getType() + "Ext{" + descriptor + "}";
	}
}