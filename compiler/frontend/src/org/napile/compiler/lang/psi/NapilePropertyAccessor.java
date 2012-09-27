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

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.NapileNodeTypes;
import org.napile.compiler.lexer.NapileTokens;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author max
 */
public class NapilePropertyAccessor extends NapileDeclarationImpl implements NapileDeclarationWithBody, NapileModifierListOwner, NapileWithExpressionInitializer
{
	public NapilePropertyAccessor(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitPropertyAccessor(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitPropertyAccessor(this, data);
	}

	public boolean isSetter()
	{
		return findChildByType(NapileTokens.SET_KEYWORD) != null;
	}

	public boolean isGetter()
	{
		return findChildByType(NapileTokens.GET_KEYWORD) != null;
	}

	@Nullable
	public NapileElement getParameter()
	{
		NapileParameterList parameterList = (NapileParameterList) findChildByType(NapileNodeTypes.VALUE_PARAMETER_LIST);
		if(parameterList == null)
			return null;
		List<NapileElement> parameters = parameterList.getParameters();
		if(parameters.isEmpty())
			return null;
		return parameters.get(0);
	}

	@NotNull
	@Override
	public List<NapileElement> getValueParameters()
	{
		NapileElement parameter = getParameter();
		if(parameter == null)
		{
			return Collections.emptyList();
		}
		return Collections.singletonList(parameter);
	}

	@Nullable
	@Override
	public NapileExpression getBodyExpression()
	{
		return findChildByClass(NapileExpression.class);
	}

	@Override
	public boolean hasBlockBody()
	{
		return findChildByType(NapileTokens.EQ) == null;
	}

	@Override
	public boolean hasDeclaredReturnType()
	{
		return true;
	}

	@NotNull
	@Override
	public NapileElement asElement()
	{
		return this;
	}

	@Nullable
	public NapileTypeReference getReturnTypeReference()
	{
		return findChildByClass(NapileTypeReference.class);
	}

	@NotNull
	public PsiElement getNamePlaceholder()
	{
		PsiElement get = findChildByType(NapileTokens.GET_KEYWORD);
		if(get != null)
		{
			return get;
		}
		return findChildByType(NapileTokens.SET_KEYWORD);
	}

	@Nullable
	@Override
	public NapileExpression getInitializer()
	{
		return PsiTreeUtil.getNextSiblingOfType(findChildByType(NapileTokens.EQ), NapileExpression.class);
	}
}
