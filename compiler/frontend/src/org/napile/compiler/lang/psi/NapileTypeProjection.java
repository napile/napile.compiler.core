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

package org.napile.compiler.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.NapileNodeTypes;
import org.napile.compiler.lexer.NapileToken;
import org.napile.compiler.lexer.JetTokens;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

/**
 * @author abreslav
 */
public class NapileTypeProjection extends NapileElementImpl implements NapileModifierListOwner
{
	public NapileTypeProjection(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public NapileModifierList getModifierList()
	{
		return (NapileModifierList) findChildByType(NapileNodeTypes.MODIFIER_LIST);
	}

	@Override
	public boolean hasModifier(NapileToken modifier)
	{
		NapileModifierList modifierList = getModifierList();
		return modifierList != null && modifierList.hasModifier(modifier);
	}

	@Override
	public ASTNode getModifierNode(NapileToken token)
	{
		NapileModifierList modifierList = getModifierList();
		return modifierList == null ? null : modifierList.getModifierNode(token);
	}

	@NotNull
	public NapileProjectionKind getProjectionKind()
	{
		ASTNode projectionNode = getProjectionNode();
		if(projectionNode == null)
			return NapileProjectionKind.NONE;

		if(projectionNode.getElementType() == JetTokens.IN_KEYWORD)
			return NapileProjectionKind.IN;
		if(projectionNode.getElementType() == JetTokens.OUT_KEYWORD)
			return NapileProjectionKind.OUT;
		if(projectionNode.getElementType() == JetTokens.MUL)
			return NapileProjectionKind.STAR;

		throw new IllegalStateException(projectionNode.getText());
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitTypeProjection(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitTypeProjection(this, data);
	}

	@Nullable
	public NapileTypeReference getTypeReference()
	{
		return (NapileTypeReference) findChildByType(NapileNodeTypes.TYPE_REFERENCE);
	}

	@Nullable
	public ASTNode getProjectionNode()
	{
		NapileModifierList modifierList = getModifierList();
		if(modifierList != null)
		{
			ASTNode node = modifierList.getModifierNode(JetTokens.IN_KEYWORD);
			if(node != null)
			{
				return node;
			}
			node = modifierList.getModifierNode(JetTokens.OUT_KEYWORD);
			if(node != null)
			{
				return node;
			}
		}
		PsiElement star = findChildByType(JetTokens.MUL);
		if(star != null)
		{
			return star.getNode();
		}

		return null;
	}
}
