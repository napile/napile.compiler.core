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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.NapileNodeTypes;
import org.napile.compiler.lexer.NapileToken;
import org.napile.compiler.lexer.JetTokens;
import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

/**
 * @author max
 */
public class NapileFunctionType extends NapileTypeElement
{

	public static final NapileToken RETURN_TYPE_SEPARATOR = JetTokens.ARROW;

	public NapileFunctionType(@NotNull ASTNode node)
	{
		super(node);
	}

	@NotNull
	@Override
	public List<NapileTypeReference> getTypeArgumentsAsTypes()
	{
		ArrayList<NapileTypeReference> result = Lists.newArrayList();
		NapileTypeReference receiverTypeRef = getReceiverTypeRef();
		if(receiverTypeRef != null)
		{
			result.add(receiverTypeRef);
		}
		for(NapileParameter jetParameter : getParameters())
		{
			result.add(jetParameter.getTypeReference());
		}
		NapileTypeReference returnTypeRef = getReturnTypeRef();
		if(returnTypeRef != null)
		{
			result.add(returnTypeRef);
		}
		return result;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitFunctionType(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitFunctionType(this, data);
	}

	@Nullable
	public NapileParameterList getParameterList()
	{
		return (NapileParameterList) findChildByType(NapileNodeTypes.VALUE_PARAMETER_LIST);
	}

	@NotNull
	public List<NapileParameter> getParameters()
	{
		NapileParameterList list = getParameterList();
		return list != null ? list.getParameters() : Collections.<NapileParameter>emptyList();
	}

	@Nullable
	public NapileTypeReference getReceiverTypeRef()
	{
		PsiElement child = getFirstChild();
		while(child != null)
		{
			IElementType tt = child.getNode().getElementType();
			if(tt == JetTokens.LPAR || tt == RETURN_TYPE_SEPARATOR)
				break;
			if(child instanceof NapileTypeReference)
			{
				return (NapileTypeReference) child;
			}
			child = child.getNextSibling();
		}

		return null;
	}

	@Nullable
	public NapileTypeReference getReturnTypeRef()
	{
		boolean colonPassed = false;
		PsiElement child = getFirstChild();
		while(child != null)
		{
			IElementType tt = child.getNode().getElementType();
			if(tt == RETURN_TYPE_SEPARATOR)
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
