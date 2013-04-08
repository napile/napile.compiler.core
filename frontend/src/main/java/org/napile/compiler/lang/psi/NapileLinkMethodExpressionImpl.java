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
import com.intellij.lang.ASTNode;

/**
 * @author VISTALL
 */
public class NapileLinkMethodExpressionImpl extends NapileExpressionImpl implements NapileLinkMethodExpression
{
	public NapileLinkMethodExpressionImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	@Nullable
	public NapileDotQualifiedExpressionImpl getClassTarget()
	{
		return findChildByClass(NapileDotQualifiedExpressionImpl.class);
	}

	@Override
	@Nullable
	public NapileSimpleNameExpression getTarget()
	{
		return findChildByClass(NapileSimpleNameExpression.class);
	}

	@Nullable
	@Override
	public NapileTypeArgumentList getTypeArgumentList()
	{
		return findChildByClass(NapileTypeArgumentList.class);
	}

	@Override
	@Nullable
	public NapileTypeList getTypeList()
	{
		return findChildByClass(NapileTypeList.class);
	}

	@Override
	public String getQualifiedName()
	{
		NapileDotQualifiedExpressionImpl expression = getClassTarget();
		if(expression == null)
			return null;
		StringBuilder builder = new StringBuilder();
		for(NapileSimpleNameExpression e : expression.getChildExpressions())
		{
			if(builder.length() > 0)
				builder.append(".");
			builder.append(e.getReferencedName());
		}
		return builder.toString();
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitLinkMethodExpression(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitLinkMethodExpression(this, data);
	}
}
