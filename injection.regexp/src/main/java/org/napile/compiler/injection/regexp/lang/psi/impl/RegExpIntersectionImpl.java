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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.napile.compiler.injection.regexp.lang.psi.RegExpClassElement;
import org.napile.compiler.injection.regexp.lang.psi.RegExpElementVisitor;
import org.napile.compiler.injection.regexp.lang.psi.RegExpIntersection;
import org.napile.compiler.injection.regexp.lang.parser.RegExpElementTypes;

public class RegExpIntersectionImpl extends RegExpElementImpl implements RegExpIntersection
{
	public RegExpIntersectionImpl(ASTNode astNode)
	{
		super(astNode);
	}

	public void accept(RegExpElementVisitor visitor)
	{
		visitor.visitRegExpIntersection(this);
	}

	@NotNull
	public RegExpClassElement getLOperand()
	{
		final ASTNode[] nodes = getNode().getChildren(RegExpElementTypes.CLASS_ELEMENTS);
		return (RegExpClassElement) nodes[0].getPsi();
	}

	@Nullable
	public RegExpClassElement getROperand()
	{
		final ASTNode[] nodes = getNode().getChildren(RegExpElementTypes.CLASS_ELEMENTS);
		return nodes.length > 1 ? (RegExpClassElement) nodes[1].getPsi() : null;
	}
}
