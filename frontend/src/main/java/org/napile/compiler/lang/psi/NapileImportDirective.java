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
import org.napile.compiler.lang.lexer.NapileTokens;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;

/**
 * @author max
 */
public class NapileImportDirective extends NapileElementImpl
{
	public NapileImportDirective(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitImportDirective(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitImportDirective(this, data);
	}

	public boolean isAbsoluteInRootNamespace()
	{
		return findChildByType(NapileTokens.PACKAGE_KEYWORD) != null;
	}

	@Nullable
	@IfNotParsed
	public NapileExpression getImportedReference()
	{
		return findChildByClass(NapileExpression.class);
	}

	@Nullable
	public ASTNode getAliasNameNode()
	{
		boolean asPassed = false;
		ASTNode childNode = getNode().getFirstChildNode();
		while(childNode != null)
		{
			IElementType tt = childNode.getElementType();
			if(tt == NapileTokens.AS_KEYWORD)
				asPassed = true;
			if(asPassed && tt == NapileTokens.IDENTIFIER)
			{
				return childNode;
			}

			childNode = childNode.getTreeNext();
		}
		return null;
	}

	@Nullable
	public String getAliasName()
	{
		ASTNode aliasNameNode = getAliasNameNode();
		if(aliasNameNode == null)
		{
			return null;
		}
		return aliasNameNode.getText();
	}

	public boolean isAllUnder()
	{
		return getNode().findChildByType(NapileTokens.MUL) != null;
	}
}
