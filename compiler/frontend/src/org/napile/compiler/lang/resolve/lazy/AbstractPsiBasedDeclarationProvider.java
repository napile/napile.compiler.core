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

package org.napile.compiler.lang.resolve.lazy;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileClassInitializer;
import org.napile.compiler.lang.psi.NapileLikeClass;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileNamedFunction;
import org.napile.compiler.lang.psi.NapileParameter;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.resolve.name.Name;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * @author abreslav
 */
public abstract class AbstractPsiBasedDeclarationProvider implements DeclarationProvider
{
	private final List<NapileDeclaration> allDeclarations = Lists.newArrayList();
	private final Multimap<Name, NapileNamedFunction> functions = HashMultimap.create();
	private final Multimap<Name, NapileProperty> properties = HashMultimap.create();
	private final Map<Name, NapileLikeClass> classesAndObjects = Maps.newHashMap();

	private boolean indexCreated = false;

	protected final void createIndex()
	{
		if(indexCreated)
			return;
		indexCreated = true;

		doCreateIndex();
	}

	protected abstract void doCreateIndex();

	protected void putToIndex(NapileDeclaration declaration)
	{
		if(declaration instanceof NapileClassInitializer)
		{
			return;
		}
		allDeclarations.add(declaration);
		if(declaration instanceof NapileNamedFunction)
		{
			NapileNamedFunction namedFunction = (NapileNamedFunction) declaration;
			functions.put(namedFunction.getNameAsName(), namedFunction);
		}
		else if(declaration instanceof NapileProperty)
		{
			NapileProperty property = (NapileProperty) declaration;
			properties.put(property.getNameAsName(), property);
		}
		else if(declaration instanceof NapileLikeClass)
		{
			NapileLikeClass classOrObject = (NapileLikeClass) declaration;
			classesAndObjects.put(classOrObject.getNameAsName(), classOrObject);
		}
		else if(declaration instanceof NapileParameter)
		{
			// Do nothing, just put it into allDeclarations is enough
		}
		else
		{
			throw new IllegalArgumentException("Unknown declaration: " + declaration);
		}
	}

	@Override
	public List<NapileDeclaration> getAllDeclarations()
	{
		createIndex();
		return allDeclarations;
	}

	@NotNull
	@Override
	public List<NapileNamedFunction> getFunctionDeclarations(@NotNull Name name)
	{
		createIndex();
		return Lists.newArrayList(functions.get(name));
	}

	@NotNull
	@Override
	public List<NapileProperty> getPropertyDeclarations(@NotNull Name name)
	{
		createIndex();
		return Lists.newArrayList(properties.get(name));
	}

	@Override
	public NapileLikeClass getClassOrObjectDeclaration(@NotNull Name name)
	{
		createIndex();
		return classesAndObjects.get(name);
	}
}
