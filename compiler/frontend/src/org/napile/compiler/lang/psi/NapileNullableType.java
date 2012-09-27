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
import org.napile.compiler.lexer.NapileTokens;
import com.intellij.lang.ASTNode;

/**
 * @author max
 */
public class NapileNullableType extends NapileTypeElement
{
	public NapileNullableType(@NotNull ASTNode node)
	{
		super(node);
	}

	@NotNull
	public ASTNode getQuestionMarkNode()
	{
		return getNode().findChildByType(NapileTokens.QUEST);
	}

	@NotNull
	@Override
	public List<NapileTypeReference> getTypeArguments()
	{
		return getInnerType().getTypeArguments();
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitNullableType(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitNullableType(this, data);
	}

	@NotNull
	public NapileTypeElement getInnerType()
	{
		return findChildByClass(NapileTypeElement.class);
	}
}
