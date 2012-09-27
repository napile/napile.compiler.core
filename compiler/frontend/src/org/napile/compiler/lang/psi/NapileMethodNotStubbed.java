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
import com.intellij.psi.tree.IElementType;

/**
 * @author abreslav
 */
abstract public class NapileMethodNotStubbed extends NapileTypeParameterListOwnerNotStubbed implements NapileMethod
{

	public NapileMethodNotStubbed(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	@Nullable
	public NapileParameterList getValueParameterList()
	{
		return (NapileParameterList) findChildByType(NapileNodeTypes.VALUE_PARAMETER_LIST);
	}

	@Override
	@NotNull
	public List<NapileElement> getValueParameters()
	{
		NapileParameterList list = getValueParameterList();
		return list != null ? list.getParameters() : Collections.<NapileElement>emptyList();
	}

	@Override
	@Nullable
	public NapileExpression getBodyExpression()
	{
		return findChildByClass(NapileExpression.class);
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
	@Override
	public NapileElement asElement()
	{
		return this;
	}

	@Override
	public boolean isLocal()
	{
		return false;
	}
}
