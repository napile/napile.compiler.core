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

import static org.jetbrains.jet.j2k.util.AstUtil.join;
import static org.jetbrains.jet.j2k.util.AstUtil.nodesToKotlin;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.J2KConverterFlags;
import org.jetbrains.jet.j2k.util.AstUtil;

/**
 * @author ignatov
 */
public class Class extends Member
{
	@NotNull
	String TYPE = "class";
	final Identifier myName;
	protected final List<Member> myMembers;
	private final List<Element> myTypeParameters;
	private final List<Type> myExtendsTypes;
	private final List<Type> myImplementsTypes;

	public Class(Converter converter, Identifier name, Set<String> modifiers, List<Element> typeParameters, List<Type> extendsTypes, List<Type> implementsTypes, List<Member> members)
	{
		myName = name;
		myModifiers = modifiers;
		myTypeParameters = typeParameters;
		myExtendsTypes = extendsTypes;
		myImplementsTypes = implementsTypes;
		myMembers = getMembers(members, converter);
	}

	/*package*/
	static List<Member> getMembers(List<Member> members, Converter converter)
	{
		List<Member> withoutPrivate = new LinkedList<Member>();
		if(converter.hasFlag(J2KConverterFlags.SKIP_NON_PUBLIC_MEMBERS))
		{
			for(Member m : members)
			{
				if(m.accessModifier().equals("public") || m.accessModifier().equals("protected"))
				{
					withoutPrivate.add(m);
				}
			}
		}
		else
		{
			withoutPrivate = members;
		}
		return withoutPrivate;
	}


	private boolean hasWhere()
	{
		for(Element t : myTypeParameters)
		{
			if(t instanceof TypeParameter && ((TypeParameter) t).hasWhere())
			{
				return true;
			}
		}
		return false;
	}

	@NotNull
	String typeParameterWhereToKotlin()
	{
		if(hasWhere())
		{
			List<String> wheres = new LinkedList<String>();
			for(Element t : myTypeParameters)
			{
				if(t instanceof TypeParameter)
				{
					wheres.add(((TypeParameter) t).getWhereToKotlin());
				}
			}
			return SPACE + "where" + SPACE + join(wheres, COMMA_WITH_SPACE) + SPACE;
		}
		return EMPTY;
	}

	@NotNull
	String typeParametersToKotlin()
	{
		return myTypeParameters.size() > 0 ? "<" + AstUtil.joinNodes(myTypeParameters, COMMA_WITH_SPACE) + ">" : EMPTY;
	}

	List<String> baseClassSignatureWithParams()
	{
		if(TYPE.equals("class") && myExtendsTypes.size() == 1)
		{
			LinkedList<String> result = new LinkedList<String>();
			result.add(myExtendsTypes.get(0).toKotlin());
			return result;
		}
		else
		{
			return nodesToKotlin(myExtendsTypes);
		}
	}

	@NotNull
	String implementTypesToKotlin()
	{
		List<String> allTypes = new LinkedList<String>()
		{
			{
				addAll(baseClassSignatureWithParams());
				addAll(nodesToKotlin(myImplementsTypes));
			}
		};
		return allTypes.size() == 0 ? EMPTY : SPACE + COLON + SPACE + join(allTypes, COMMA_WITH_SPACE);
	}

	@NotNull
	String modifiersToKotlin()
	{
		List<String> modifierList = new LinkedList<String>();

		modifierList.add(accessModifier());

		if(needAbstractModifier())
		{
			modifierList.add(Modifier.ABSTRACT);
		}

		if(modifierList.size() > 0)
		{
			return join(modifierList, SPACE) + SPACE;
		}

		return EMPTY;
	}

	boolean needAbstractModifier()
	{
		return isAbstract();
	}

	@NotNull
	String bodyToKotlin()
	{
		return N +
				"{" + N +
				AstUtil.joinNodes(myMembers, N) + N +
				"}";
	}

	@NotNull
	@Override
	public String toKotlin()
	{
		return modifiersToKotlin() + TYPE + SPACE + myName.toKotlin() + typeParametersToKotlin() +
				implementTypesToKotlin() +
				typeParameterWhereToKotlin() +
				bodyToKotlin();
	}
}
