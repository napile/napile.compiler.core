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
import org.napile.compiler.lang.types.NapileType;

/**
 * @author abreslav
 */
public abstract class AbstractReceiverDescriptor implements ReceiverDescriptor
{
	protected final NapileType receiverType;

	public AbstractReceiverDescriptor(@NotNull NapileType receiverType)
	{
		this.receiverType = receiverType;
	}

	@Override
	@NotNull
	public NapileType getType()
	{
		return receiverType;
	}

	@Override
	public boolean exists()
	{
		return true;
	}
}
