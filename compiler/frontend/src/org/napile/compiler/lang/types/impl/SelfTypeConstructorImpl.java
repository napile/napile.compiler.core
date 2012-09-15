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
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.types.SelfTypeConstructor;
import org.napile.compiler.lang.types.impl.TypeConstructorImpl;

/**
 * @author VISTALL
 * @date 19:02/12.09.12
 */
public class SelfTypeConstructorImpl extends TypeConstructorImpl implements SelfTypeConstructor
{
	public SelfTypeConstructorImpl(@NotNull ClassDescriptor classDescriptor)
	{
		super(classDescriptor, Collections.<AnnotationDescriptor>emptyList(), false, classDescriptor.getTypeConstructor().toString(), Collections.<TypeParameterDescriptor>emptyList(), classDescriptor.getSupertypes());
	}

	@NotNull
	@Override
	public ClassDescriptor getDeclarationDescriptor()
	{
		return (ClassDescriptor) super.getDeclarationDescriptor();
	}
}
