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

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.doc.lang.psi.NapileDoc;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;

/**
 * @author abreslav
 */
public class NapileAnonymMethodImpl extends NapileDeclarationImpl implements NapileMethod
{
	public NapileAnonymMethodImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	@Nullable
	@Override
	public NapileTypeParameterList getTypeParameterList()
	{
		return (NapileTypeParameterList) findChildByType(NapileNodes.TYPE_PARAMETER_LIST);
	}

	@Override
	@NotNull
	public NapileTypeParameter[] getTypeParameters()
	{
		NapileTypeParameterList list = getTypeParameterList();
		if(list == null)
			return NapileTypeParameter.EMPTY_ARRAY;

		return list.getParameters();
	}

	@Override
	public boolean hasBlockBody()
	{
		return false;
	}

	@Override
	public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException
	{
		return null;
	}

	@Override
	public PsiElement getNameIdentifier()
	{
		return null;
	}

	public boolean hasParameterSpecification()
	{
		return findChildByType(NapileTokens.ARROW) != null;
	}

	@Override
	public NapileBlockExpression getBodyExpression()
	{
		return findChildByClass(NapileBlockExpression.class);
	}

	@Override
	@Nullable
	public NapileCallParameterList getCallParameterList()
	{
		return (NapileCallParameterList) findChildByType(NapileNodes.CALL_PARAMETER_LIST);
	}

	@Override
	@NotNull
	public NapileCallParameter[] getValueParameters()
	{
		NapileCallParameterList list = getCallParameterList();
		return list != null ? list.getParameters() : NapileCallParameter.EMPTY_ARRAY;
	}

	@Override
	public boolean hasDeclaredReturnType()
	{
		return getReturnTypeRef() != null;
	}

	@Override
	@Nullable
	public NapileTypeReference getReturnTypeRef()
	{
		boolean colonPassed = false;
		PsiElement child = getFirstChild();
		while(child != null)
		{
			IElementType tt = child.getNode().getElementType();
			if(tt == NapileTokens.COLON)
			{
				colonPassed = true;
			}
			if(colonPassed && child instanceof NapileTypeReference)
			{
				return (NapileTypeReference) child;
			}
			child = child.getNextSibling();
		}

		return null;
	}

	@NotNull
	public ASTNode getOpenBraceNode()
	{
		return getNode().findChildByType(NapileTokens.LBRACE);
	}

	@Nullable
	@IfNotParsed
	public ASTNode getClosingBraceNode()
	{
		return getNode().findChildByType(NapileTokens.RBRACE);
	}

	@Nullable
	public ASTNode getArrowNode()
	{
		return getNode().findChildByType(NapileTokens.ARROW);
	}

	@NotNull
	@Override
	public Name getNameAsSafeName()
	{
		return NapilePsiUtil.NO_NAME_PROVIDED;
	}

	@Override
	public NapileDoc getDocComment()
	{
		return null;
	}

	@NotNull
	@Override
	public Name getNameAsName()
	{
		return NapilePsiUtil.NO_NAME_PROVIDED;
	}
}
