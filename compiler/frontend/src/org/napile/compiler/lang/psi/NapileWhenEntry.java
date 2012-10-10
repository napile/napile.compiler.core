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
import org.napile.compiler.lexer.NapileTokens;
import org.napile.compiler.psi.NapileExpression;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

/**
 * @author abreslav
 */
public class NapileWhenEntry extends NapileElementImpl
{
	public NapileWhenEntry(@NotNull ASTNode node)
	{
		super(node);
	}

	public boolean isElse()
	{
		return getElseKeywordElement() != null;
	}

	@Nullable
	public PsiElement getElseKeywordElement()
	{
		return findChildByType(NapileTokens.ELSE_KEYWORD);
	}

	@Nullable
	public NapileExpression getExpression()
	{
		return findChildByClass(NapileExpression.class);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitWhenEntry(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitWhenEntry(this, data);
	}

	@NotNull
	public NapileWhenCondition[] getConditions()
	{
		return findChildrenByClass(NapileWhenCondition.class);
	}
}
