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
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;

import org.napile.compiler.injection.regexp.lang.parser.RegExpElementTypes;
import org.napile.compiler.injection.regexp.lang.psi.RegExpAtom;
import org.napile.compiler.injection.regexp.lang.psi.RegExpElementVisitor;
import org.napile.compiler.injection.regexp.lang.psi.RegExpQuantifier;
import org.napile.compiler.injection.regexp.lang.psi.RegExpClosure;

public class RegExpClosureImpl extends RegExpElementImpl implements RegExpClosure
{

	public RegExpClosureImpl(ASTNode astNode)
	{
		super(astNode);
	}

	public void accept(RegExpElementVisitor visitor)
	{
		visitor.visitRegExpClosure(this);
	}

	@NotNull
	public RegExpQuantifier getQuantifier()
	{
		final ASTNode node = getNode().findChildByType(RegExpElementTypes.QUANTIFIER);
		assert node != null;
		return (RegExpQuantifier) node.getPsi();
	}

	@NotNull
	public RegExpAtom getAtom()
	{
		final RegExpAtom atom = PsiTreeUtil.getChildOfType(this, RegExpAtom.class);
		assert atom != null;
		return atom;
	}
}
