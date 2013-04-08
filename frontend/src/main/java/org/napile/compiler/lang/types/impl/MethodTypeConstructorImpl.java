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
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.MethodTypeConstructor;
import org.napile.compiler.lang.types.TypeConstructorVisitor;

/**
 * @author VISTALL
 * @since 12:17/15.09.12
 */
public class MethodTypeConstructorImpl extends AbstractTypeConstructorImpl implements MethodTypeConstructor
{
	private final Name name;
	private final JetType returnType;
	private final Map<Name, JetType> parameterTypes;

	public MethodTypeConstructorImpl(@Nullable Name name, @NotNull JetType returnType, Map<Name, JetType> parameterTypes, @NotNull JetScope scope)
	{
		super(scope, new FqName("napile.lang.AnonymContext"));
		this.name = name;
		this.returnType = returnType;
		this.parameterTypes = parameterTypes;
	}

	@Override
	public <A, R> R accept(JetType type, TypeConstructorVisitor<A, R> visitor, A arg)
	{
		return visitor.visitMethodType(type, this, arg);
	}

	@Override
	@Nullable
	public Name getExpectedName()
	{
		return name;
	}

	@NotNull
	@Override
	public JetType getReturnType()
	{
		return returnType;
	}

	@NotNull
	@Override
	public Map<Name, JetType> getParameterTypes()
	{
		return parameterTypes;
	}

	@Override
	public boolean equals(Object o)
	{
		if(o == null || o.getClass() != MethodTypeConstructorImpl.class)
			return false;
		MethodTypeConstructor oConstructor = (MethodTypeConstructor) o;
		if(!returnType.equals(oConstructor.getReturnType()))
			return false;

		if(parameterTypes.size() != oConstructor.getParameterTypes().size())
			return false;

		Iterator<Map.Entry<Name, JetType>> it1 = parameterTypes.entrySet().iterator();
		Iterator<Map.Entry<Name, JetType>> it2 = oConstructor.getParameterTypes().entrySet().iterator();
		while(it1.hasNext() && it2.hasNext())
		{
			Map.Entry<Name, JetType> entry1 = it1.next();
			Map.Entry<Name, JetType> entry2 = it2.next();

			if(!entry1.getValue().equals(entry2.getValue()))
				return false;
		}
		return true;
	}
}
