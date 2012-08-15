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

import static org.jetbrains.jet.j2k.Converter.getDefaultInitializer;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

/**
 * @author ignatov
 */
public class Field extends Member
{
	final Identifier myIdentifier;
	private final int myWritingAccesses;
	final Type myType;
	final Element myInitializer;

	public Field(Identifier identifier, Set<String> modifiers, Type type, Element initializer, int writingAccesses)
	{
		myIdentifier = identifier;
		myWritingAccesses = writingAccesses;
		myModifiers = modifiers;
		myType = type;
		myInitializer = initializer;
	}

	public Element getInitializer()
	{
		return myInitializer;
	}

	public Identifier getIdentifier()
	{
		return myIdentifier;
	}

	public Type getType()
	{
		return myType;
	}

	@NotNull
	String modifiersToKotlin()
	{
		List<String> modifierList = new LinkedList<String>();

		modifierList.add(accessModifier());

		if(isAbstract())
			modifierList.add(Modifier.ABSTRACT);

		if(myModifiers.contains(Modifier.FINAL))
			modifierList.add(Modifier.FINAL);
		if(myModifiers.contains(Modifier.STATIC))
			modifierList.add(Modifier.STATIC);

		modifierList.add("var");

		if(modifierList.size() > 0)
		{
			return AstUtil.join(modifierList, SPACE) + SPACE;
		}

		return EMPTY;
	}

	@Override
	public boolean isStatic()
	{
		return myModifiers.contains(Modifier.STATIC);
	}

	@NotNull
	@Override
	public String toKotlin()
	{
		final String declaration = modifiersToKotlin() + myIdentifier.toKotlin() + SPACE + COLON + SPACE + myType.toKotlin();

		if(myInitializer.isEmpty())
		{
			return declaration + (myModifiers.contains(Modifier.FINAL) && !isStatic() && myWritingAccesses == 1 ? EMPTY : SPACE + EQUAL + SPACE + getDefaultInitializer(this));
		}

		return declaration + SPACE + EQUAL + SPACE + myInitializer.toKotlin();
	}
}
