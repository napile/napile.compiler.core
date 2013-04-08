/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.napile.compiler.injection.regexp.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import org.napile.compiler.injection.regexp.lang.parser.RegExpElementTypes;
import org.napile.compiler.injection.regexp.lang.psi.RegExpCharRange;
import org.napile.compiler.injection.regexp.lang.psi.RegExpElementVisitor;

public class RegExpCharRangeImpl extends RegExpElementImpl implements RegExpCharRange
{
	private static final TokenSet E = TokenSet.create(RegExpElementTypes.CHAR, RegExpElementTypes.SIMPLE_CLASS);

	public RegExpCharRangeImpl(ASTNode astNode)
	{
		super(astNode);
	}

	@NotNull
	public Endpoint getFrom()
	{
		return (Endpoint) getCharNode(0);
	}

	@NotNull
	public Endpoint getTo()
	{
		return (Endpoint) getCharNode(1);
	}

	private PsiElement getCharNode(int idx)
	{
		final ASTNode[] ch = getNode().getChildren(E);
		assert ch.length == 2;
		return ch[idx].getPsi();
	}

	public void accept(RegExpElementVisitor visitor)
	{
		visitor.visitRegExpCharRange(this);
	}
}
