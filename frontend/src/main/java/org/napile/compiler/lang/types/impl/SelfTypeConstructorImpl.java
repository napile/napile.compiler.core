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

import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.SelfTypeConstructor;
import org.napile.compiler.lang.types.TypeConstructorVisitor;

/**
 * @author VISTALL
 * @since 19:02/12.09.12
 */
public class SelfTypeConstructorImpl extends AbstractTypeConstructorImpl implements SelfTypeConstructor
{
	private final ClassDescriptor descriptor;

	public SelfTypeConstructorImpl(@NotNull ClassDescriptor classDescriptor)
	{
		super(Collections.singletonList(classDescriptor.getDefaultType()));
		descriptor = classDescriptor;
	}

	@NotNull
	@Override
	public ClassDescriptor getDeclarationDescriptor()
	{
		return descriptor;
	}

	@Override
	public <A, R> R accept(NapileType type, TypeConstructorVisitor<A, R> visitor, A arg)
	{
		return visitor.visitSelfType(type, this, arg);
	}
}
