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

package org.jetbrains.jet.j2k.ast;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * @author ignatov
 */
public abstract class Member extends Node implements IMember
{
	Set<String> myModifiers;

	@NotNull
	String accessModifier()
	{
		for(String m : myModifiers)
		{
			if(m.equals(Modifier.HERITABLE) || m.equals(Modifier.LOCAL) || m.equals(Modifier.COVERED))
			{
				return m;
			}
		}
		return EMPTY;
	}

	@Override
	public boolean isAbstract()
	{
		return myModifiers.contains(Modifier.ABSTRACT);
	}

	@Override
	public boolean isStatic()
	{
		return myModifiers.contains(Modifier.STATIC);
	}
}
