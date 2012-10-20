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
import org.napile.compiler.lang.lexer.NapileNodes;
import com.intellij.lang.ASTNode;

/**
 * @author max
 */
public class NapileAnnotation extends NapileElementImpl implements NapileCallElement
{
	public NapileAnnotation(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitAnnotation(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitAnnotation(this, data);
	}


	@Nullable
	@IfNotParsed
	public NapileTypeReference getTypeReference()
	{
		NapileConstructorCalleeExpression calleeExpression = getCalleeExpression();
		if(calleeExpression == null)
		{
			return null;
		}
		return calleeExpression.getTypeReference();
	}

	@Override
	public NapileConstructorCalleeExpression getCalleeExpression()
	{
		return (NapileConstructorCalleeExpression) findChildByType(NapileNodes.CONSTRUCTOR_CALLEE);
	}

	@Override
	public NapileValueArgumentList getValueArgumentList()
	{
		return (NapileValueArgumentList) findChildByType(NapileNodes.VALUE_ARGUMENT_LIST);
	}

	@NotNull
	@Override
	public List<? extends ValueArgument> getValueArguments()
	{
		NapileValueArgumentList list = getValueArgumentList();
		return list != null ? list.getArguments() : Collections.<NapileValueArgument>emptyList();
	}

	@NotNull
	@Override
	public List<NapileExpression> getFunctionLiteralArguments()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public List<NapileTypeReference> getTypeArguments()
	{
		NapileTypeArgumentList typeArgumentList = getTypeArgumentList();
		if(typeArgumentList == null)
		{
			return Collections.emptyList();
		}
		return typeArgumentList.getArguments();
	}

	@Override
	public NapileTypeArgumentList getTypeArgumentList()
	{
		NapileTypeReference typeReference = getTypeReference();
		if(typeReference == null)
		{
			return null;
		}
		NapileTypeElement typeElement = typeReference.getTypeElement();
		if(typeElement instanceof NapileUserType)
		{
			NapileUserType userType = (NapileUserType) typeElement;
			return userType.getTypeArgumentList();
		}
		return null;
	}
}
