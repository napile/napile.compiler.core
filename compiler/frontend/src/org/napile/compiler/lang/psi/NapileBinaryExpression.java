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
import org.napile.compiler.lang.lexer.NapileNodes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

/**
 * @author max
 */
public class NapileBinaryExpression extends NapileExpressionImpl implements NapileOperationExpression
{
	public NapileBinaryExpression(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitBinaryExpression(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitBinaryExpression(this, data);
	}

	@NotNull
	public NapileExpression getLeft()
	{
		NapileExpression left = findChildByClass(NapileExpression.class);
		assert left != null;
		return left;
	}

	@Nullable
	@IfNotParsed
	public NapileExpression getRight()
	{
		ASTNode node = getOperationReference().getNode().getTreeNext();
		while(node != null)
		{
			PsiElement psi = node.getPsi();
			if(psi instanceof NapileExpression)
			{
				return (NapileExpression) psi;
			}
			node = node.getTreeNext();
		}

		return null;
	}

	@Override
	@NotNull
	public NapileSimpleNameExpression getOperationReference()
	{
		return (NapileSimpleNameExpression) findChildByType(NapileNodes.OPERATION_REFERENCE);
	}

	public IElementType getOperationToken()
	{
		return getOperationReference().getReferencedNameElementType();
	}
}
