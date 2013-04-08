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

package org.napile.compiler.lang.psi.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileBlockExpression;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpressionImpl;
import org.napile.compiler.lang.psi.NapileModifiableBlockHelper;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifiableCodeBlock;

/**
 * @author max
 */
public class NapileBlockExpressionImpl extends NapileExpressionImpl implements PsiModifiableCodeBlock, NapileBlockExpression
{
	public NapileBlockExpressionImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public boolean shouldChangeModificationCount(PsiElement place)
	{
		return NapileModifiableBlockHelper.shouldChangeModificationCount(place);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitBlockExpression(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitBlockExpression(this, data);
	}

	@Override
	@NotNull
	public NapileElement[] getStatements()
	{
		return findChildrenByClass(NapileElement.class);
	}

	@Override
	@Nullable
	public TextRange getLastBracketRange()
	{
		PsiElement rBrace = findChildByType(NapileTokens.RBRACE);
		return rBrace != null ? rBrace.getTextRange() : null;
	}
}
