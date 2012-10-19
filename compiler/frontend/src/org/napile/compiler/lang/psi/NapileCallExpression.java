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
import org.napile.compiler.lang.psi.NapileTypeReference;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.SmartList;

/**
 * @author max
 */
public class NapileCallExpression extends NapileExpressionImpl implements NapileCallElement
{
	public NapileCallExpression(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitCallExpression(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitCallExpression(this, data);
	}

	@Override
	@Nullable
	public NapileExpression getCalleeExpression()
	{
		return findChildByClass(NapileExpression.class);
	}

	@Override
	@Nullable
	public NapileValueArgumentList getValueArgumentList()
	{
		return (NapileValueArgumentList) findChildByType(NapileNodes.VALUE_ARGUMENT_LIST);
	}

	@Nullable
	@Override
	public NapileTypeArgumentList getTypeArgumentList()
	{
		return (NapileTypeArgumentList) findChildByType(NapileNodes.TYPE_ARGUMENT_LIST);
	}

	@Override
	@NotNull
	public List<NapileExpression> getFunctionLiteralArguments()
	{
		NapileExpression calleeExpression = getCalleeExpression();
		ASTNode node;
		if(calleeExpression instanceof NapileFunctionLiteralExpression)
		{
			node = calleeExpression.getNode().getTreeNext();
		}
		else
		{
			node = getNode().getFirstChildNode();
		}
		List<NapileExpression> result = new SmartList<NapileExpression>();
		while(node != null)
		{
			PsiElement psi = node.getPsi();
			if(psi instanceof NapileFunctionLiteralExpression)
			{
				result.add((NapileFunctionLiteralExpression) psi);
			}
			node = node.getTreeNext();
		}
		return result;
	}

	@Override
	@NotNull
	public List<? extends ValueArgument> getValueArguments()
	{
		NapileValueArgumentList list = getValueArgumentList();
		return list != null ? list.getArguments() : Collections.<NapileValueArgument>emptyList();
	}

	@Override
	@NotNull
	public List<NapileTypeReference> getTypeArguments()
	{
		NapileTypeArgumentList list = getTypeArgumentList();
		return list != null ? list.getArguments() : Collections.<NapileTypeReference>emptyList();
	}
}
