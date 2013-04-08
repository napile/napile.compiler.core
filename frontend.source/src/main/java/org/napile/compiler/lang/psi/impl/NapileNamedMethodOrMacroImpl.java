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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileCallParameter;
import org.napile.compiler.lang.psi.NapileCallParameterList;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.compiler.lang.psi.NapileTypeParameterListOwnerStub;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.NamedStub;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author max
 */
public abstract class NapileNamedMethodOrMacroImpl<S extends NamedStub> extends NapileTypeParameterListOwnerStub<S> implements NapileNamedMethodOrMacro
{
	public NapileNamedMethodOrMacroImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileNamedMethodOrMacroImpl(@NotNull S stub, @NotNull IStubElementType elementType)
	{
		super(stub, elementType);
	}

	@Override
	public abstract String getName();

	@Override
	public abstract void accept(@NotNull NapileVisitorVoid visitor);


	@Override
	public abstract <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data);

	@Override
	public boolean hasBlockBody()
	{
		return getEqualsToken() == null;
	}

	@Nullable
	@Override
	public PsiElement getEqualsToken()
	{
		return findChildByType(NapileTokens.EQ);
	}


	@Override
	@Nullable
	public NapileExpression getInitializer()
	{
		return PsiTreeUtil.getNextSiblingOfType(getEqualsToken(), NapileExpression.class);
	}

	@NotNull
	@Override
	public abstract IStubElementType getElementType();

	@Override
	public ItemPresentation getPresentation()
	{
		return ItemPresentationProviders.getItemPresentation(this);
	}

	@Override
	@Nullable
	public NapileCallParameterList getCallParameterList()
	{
		return getStubOrPsiChild(NapileStubElementTypes.CALL_PARAMETER_LIST);
	}

	@Override
	@NotNull
	public NapileCallParameter[] getCallParameters()
	{
		NapileCallParameterList list = getCallParameterList();
		return list != null ? list.getParameters() : NapileCallParameter.EMPTY_ARRAY;
	}

	@Override
	@Nullable
	public NapileExpression getBodyExpression()
	{
		NapileExpression[] ex = findChildrenByClass(NapileExpression.class);
		for(NapileExpression e : ex)
			if(e.getNode().getElementType() != NapileNodes.VARIABLE_REFERENCE)
				return e;
		return null;
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

}
