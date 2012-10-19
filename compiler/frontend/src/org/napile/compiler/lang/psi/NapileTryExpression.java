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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.lexer.NapileNodes;
import com.intellij.lang.ASTNode;

/**
 * @author max
 */
public class NapileTryExpression extends NapileExpressionImpl
{
	public NapileTryExpression(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitTryExpression(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitTryExpression(this, data);
	}

	@NotNull
	public NapileBlockExpression getTryBlock()
	{
		return (NapileBlockExpression) findChildByType(NapileNodes.BLOCK);
	}

	@NotNull
	public List<NapileCatchClause> getCatchClauses()
	{
		return findChildrenByType(NapileNodes.CATCH);
	}

	@Nullable
	public NapileFinallySection getFinallyBlock()
	{
		return (NapileFinallySection) findChildByType(NapileNodes.FINALLY);
	}
}
