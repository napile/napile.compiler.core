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

/**
 * @author max
 */
public class NapileValueArgument extends NapileElementImpl implements ValueArgument
{
	public NapileValueArgument(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitArgument(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitArgument(this, data);
	}

	@Override
	@Nullable
	@IfNotParsed
	public NapileExpression getArgumentExpression()
	{
		return findChildByClass(NapileExpression.class);
	}

	@Override
	@Nullable
	public NapileValueArgumentName getArgumentName()
	{
		return (NapileValueArgumentName) findChildByType(NapileNodes.VALUE_ARGUMENT_NAME);
	}

	@Override
	public boolean isNamed()
	{
		return getArgumentName() != null;
	}

	@NotNull
	@Override
	public PsiElement asElement()
	{
		return this;
	}
}
