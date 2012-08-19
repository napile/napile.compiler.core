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

package org.napile.compiler.lang.resolve.calls;

import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.FunctionDescriptor;
import org.napile.compiler.lang.descriptors.FunctionDescriptorImpl;
import org.napile.compiler.lang.descriptors.Modality;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;

/**
 * @author alex.tkachman
 */
public class ExpressionAsFunctionDescriptor extends FunctionDescriptorImpl
{
	public ExpressionAsFunctionDescriptor(DeclarationDescriptor containingDeclaration, Name name)
	{
		super(containingDeclaration, Collections.<AnnotationDescriptor>emptyList(), name, Kind.DECLARATION, false);
	}

	@Override
	protected FunctionDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal, Kind kind)
	{
		throw new IllegalStateException();
	}

	@NotNull
	@Override
	public FunctionDescriptor copy(DeclarationDescriptor newOwner, Modality modality, boolean makeInvisible, Kind kind, boolean copyOverrides)
	{
		throw new IllegalStateException();
	}
}
