/*
 * Copyright 2010-2013 napile.org
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

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.lexer.NapileToken;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.util.NXmlMirrorUtil;
import org.napile.doc.lang.psi.NapileDoc;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.util.IncorrectOperationException;

/**
 * @author VISTALL
 * @date 15:33/19.02.13
 */
public class NXmlAnonymMethodImpl extends NXmlParentedElementBase implements NapileAnonymMethod
{
	private NXmlTypeReferenceImpl returnType;
	private NXmlCallParameterListImpl callParameterList;
	private NapileBlockExpression bodyExpression;

	public NXmlAnonymMethodImpl(PsiElement parent)
	{
		super(parent);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileAnonymMethod mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		final NapileTypeReference returnTypeRef = mirror.getReturnTypeRef();
		if(returnTypeRef != null)
		{
			returnType = new NXmlTypeReferenceImpl(this);
			returnType.setMirror(returnTypeRef);
		}

		final NapileCallParameterList callParameterList = mirror.getCallParameterList();
		if(callParameterList != null)
		{
			this.callParameterList = new NXmlCallParameterListImpl(this);
			this.callParameterList.setMirror(callParameterList);
		}

		final NapileBlockExpression blockExpression = mirror.getBodyExpression();
		if(blockExpression != null)
		{
			bodyExpression = (NapileBlockExpression) NXmlMirrorUtil.mirrorExpression(this, blockExpression);
		}
	}

	@Override
	public NapileBlockExpression getBodyExpression()
	{
		return bodyExpression;
	}

	@Override
	public boolean hasBlockBody()
	{
		return bodyExpression != null;
	}

	@Override
	public boolean hasDeclaredReturnType()
	{
		return returnType != null;
	}

	@Nullable
	@Override
	public NapileCallParameterList getCallParameterList()
	{
		return callParameterList;
	}

	@NotNull
	@Override
	public NapileCallParameter[] getCallParameters()
	{
		NapileCallParameterList list = getCallParameterList();
		return list != null ? list.getParameters() : NapileCallParameter.EMPTY_ARRAY;
	}

	@Override
	public boolean hasParameterSpecification()
	{
		return callParameterList != null;
	}

	@NotNull
	@Override
	public ASTNode getOpenBraceNode()
	{
		return null;
	}

	@Nullable
	@Override
	public ASTNode getClosingBraceNode()
	{
		return null;
	}

	@Nullable
	@Override
	public ASTNode getArrowNode()
	{
		return null;
	}

	@Nullable
	@Override
	public NapileTypeReference getReturnTypeRef()
	{
		return returnType;
	}

	@Nullable
	@Override
	public NapileTypeParameterList getTypeParameterList()
	{
		return null;
	}

	@NotNull
	@Override
	public NapileTypeParameter[] getTypeParameters()
	{
		return NapileTypeParameter.EMPTY_ARRAY;
	}

	@NotNull
	@Override
	public Name getNameAsSafeName()
	{
		return NapilePsiUtil.NO_NAME_PROVIDED;
	}

	@Nullable
	@Override
	public NapileDoc getDocComment()
	{
		return null;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitExpression(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitExpression(this, data);
	}

	@Nullable
	@Override
	public NapileModifierList getModifierList()
	{
		return null;
	}

	@Override
	public boolean hasModifier(NapileToken modifier)
	{
		return false;
	}

	@Nullable
	@Override
	public ASTNode getModifierNode(NapileToken token)
	{
		return null;
	}

	@Nullable
	@Override
	public Name getNameAsName()
	{
		return NapilePsiUtil.NO_NAME_PROVIDED;
	}

	@Nullable
	@Override
	public PsiElement getNameIdentifier()
	{
		return null;
	}

	@Override
	public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException
	{
		return null;
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return NXmlMirrorUtil.getAllToPsiArray(returnType, callParameterList, bodyExpression);
	}
}
