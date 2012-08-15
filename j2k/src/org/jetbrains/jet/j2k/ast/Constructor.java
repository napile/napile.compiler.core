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

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

/**
 * @author ignatov
 */
public class Constructor extends Method
{
	private final List<SuperConstructorCall> superConstructorCalls;

	public Constructor(Identifier identifier, Set<String> modifiers, Type type, List<Element> typeParameters, Element params, Block block, List<SuperConstructorCall> superConstructorCalls)
	{
		super(identifier, modifiers, type, typeParameters, params, block);
		this.superConstructorCalls = superConstructorCalls;
	}

	@NotNull
	@Override
	public Kind getKind()
	{
		return Kind.CONSTRUCTOR;
	}

	@NotNull
	@Override
	public String toKotlin()
	{
		return modifiersToKotlin() +
				"this" +
				typeParametersToKotlin() +
				"(" +
				myParams.toKotlin() +
				")" +
				(superConstructorCalls.isEmpty() ? EMPTY : (SPACE + COLON + SPACE + AstUtil.joinNodes(superConstructorCalls, COMMA_WITH_SPACE))) +
				myBlock.toKotlin();
	}
}
