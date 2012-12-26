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

package org.napile.compiler.lang.types.impl;

import java.util.Collection;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeConstructor;

/**
 * @author VISTALL
 * @date 21:48/26.12.12
 */
public abstract class AbstractTypeConstructorImpl implements TypeConstructor
{
	private final Collection<JetType> superTypes;

	protected AbstractTypeConstructorImpl(JetScope scope)
	{
		ClassDescriptor classDescriptor = scope.getClass(NapileLangPackage.ANY);
		superTypes = classDescriptor != null ? Collections.singletonList(classDescriptor.getDefaultType()) : Collections.<JetType>emptyList();
	}

	protected AbstractTypeConstructorImpl(Collection<JetType> superTypes)
	{
		this.superTypes = superTypes;
	}

	@Override
	@NotNull
	public final Collection<? extends JetType> getSupertypes()
	{
		return superTypes;
	}
}
