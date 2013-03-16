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

import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.MultiTypeConstructor;
import org.napile.compiler.lang.types.MultiTypeEntry;
import org.napile.compiler.lang.types.TypeConstructorVisitor;

/**
 * @author VISTALL
 * @since 18:12/26.12.12
 */
public class MultiTypeConstructorImpl extends AbstractTypeConstructorImpl implements MultiTypeConstructor
{
	private final List<MultiTypeEntry> entries;

	public MultiTypeConstructorImpl(@NotNull List<MultiTypeEntry> entries, @NotNull JetScope scope)
	{
		super(scope, NapileLangPackage.MULTI);
		this.entries = entries;
	}

	@Override
	public <A, R> R accept(JetType type, TypeConstructorVisitor<A, R> visitor, A arg)
	{
		return visitor.visitMultiType(type, this, arg);
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
