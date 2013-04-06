/*
 * Copyright 2010-2013 napile.org
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
import org.napile.compiler.lang.psi.NapileDoubleArrowExpression;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileExpressionImpl;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.lang.ASTNode;

/**
 * @author VISTALL
 * @since 17:38/03.04.13
 */
public class NapileDoubleArrowExpressionImpl extends NapileExpressionImpl implements NapileDoubleArrowExpression
{
	public NapileDoubleArrowExpressionImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	@NotNull
	@Override
	public NapileSimpleNameExpression getArrow()
	{
		final NapileExpression[] childrenByClass = findChildrenByClass(NapileExpression.class);
		return (NapileSimpleNameExpression) childrenByClass[0];
	}

	@Nullable
	@Override
	public NapileExpression getTargetExpression()
	{
		final NapileExpression[] childrenByClass = findChildrenByClass(NapileExpression.class);
		return childrenByClass.length == 2 ? childrenByClass[1] : null;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitDoubleArrowExpression(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitDoubleArrowExpression(this, data);
	}
}
