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
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.MultiTypeConstructor;
import org.napile.compiler.lang.types.MultiTypeEntry;

/**
 * @author VISTALL
 * @date 18:12/26.12.12
 */
public class MultiTypeConstructorImpl extends AbstractTypeConstructorImpl implements MultiTypeConstructor
{
	private final List<MultiTypeEntry> entries;

	public MultiTypeConstructorImpl(@NotNull List<MultiTypeEntry> entries, @NotNull JetScope scope)
	{
		super(scope);
		this.entries = entries;
	}

	@NotNull
	@Override
	public List<TypeParameterDescriptor> getParameters()
	{
		return Collections.emptyList();
	}

	@Override
	public boolean isSealed()
	{
		return false;
	}

	@Nullable
	@Override
	public ClassifierDescriptor getDeclarationDescriptor()
	{
		return null;
	}

	@Override
	public List<AnnotationDescriptor> getAnnotations()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public List<MultiTypeEntry> getEntries()
	{
		return entries;
	}

	@Override
	public boolean equals(Object o)
	{
		if(o == null || o.getClass() != MultiTypeConstructorImpl.class)
			return false;
		MultiTypeConstructor oConstructor = (MultiTypeConstructor) o;

		if(entries.size() != oConstructor.getEntries().size())
			return false;

		Iterator<MultiTypeEntry> it1 = entries.iterator();
		Iterator<MultiTypeEntry> it2 = oConstructor.getEntries().iterator();
		while(it1.hasNext() && it2.hasNext())
		{
			MultiTypeEntry entry1 = it1.next();
			MultiTypeEntry entry2 = it2.next();

			if(!entry1.type.equals(entry2.type))
				return false;
		}
		return true;
	}
}
