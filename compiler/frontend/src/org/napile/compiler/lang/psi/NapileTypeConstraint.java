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
import org.napile.compiler.NapileNodeTypes;
import org.napile.compiler.lexer.JetTokens;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;

/**
 * @author max
 */
public class NapileTypeConstraint extends NapileElementImpl
{
	public NapileTypeConstraint(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitTypeConstraint(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitTypeConstraint(this, data);
	}

	public boolean isClassObjectContraint()
	{
		return findChildByType(JetTokens.CLASS_KEYWORD) != null && findChildByType(JetTokens.ANONYM_KEYWORD) != null;
	}

	@Nullable
	@IfNotParsed
	public NapileSimpleNameExpression getSubjectTypeParameterName()
	{
		return (NapileSimpleNameExpression) findChildByType(NapileNodeTypes.REFERENCE_EXPRESSION);
	}

	@Nullable
	@IfNotParsed
	public NapileTypeReference getBoundTypeReference()
	{
		boolean passedColon = false;
		ASTNode node = getNode().getFirstChildNode();
		while(node != null)
		{
			IElementType tt = node.getElementType();
			if(tt == JetTokens.COLON)
				passedColon = true;
			if(passedColon && tt == NapileNodeTypes.TYPE_REFERENCE)
				return (NapileTypeReference) node.getPsi();
			node = node.getTreeNext();
		}

		return null;
	}
}
