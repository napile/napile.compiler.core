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

package org.napile.compiler.lang.psi.impl;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.lexer.NapileToken;
import org.napile.compiler.lang.psi.NapileAnnotation;
import org.napile.compiler.lang.psi.NapileElementImpl;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.lang.ASTNode;

/**
 * @author max
 */
public class NapileModifierListImpl extends NapileElementImpl implements NapileModifierList
{
	public NapileModifierListImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitModifierList(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitModifierList(this, data);
	}

	@Override
	@NotNull
	public List<NapileAnnotation> getAnnotations()
	{
		return findChildrenByType(NapileNodes.ANNOTATION);
	}

	@Override
	public boolean hasModifier(NapileToken token)
	{
		return getModifierNode(token) != null;
	}

	@Override
	@Nullable
	public ASTNode getModifierNode(NapileToken token)
	{
		ASTNode node = getNode().getFirstChildNode();
		while(node != null)
		{
			if(node.getElementType() == token)
				return node;
			node = node.getTreeNext();
		}
		return null;
	}
}
