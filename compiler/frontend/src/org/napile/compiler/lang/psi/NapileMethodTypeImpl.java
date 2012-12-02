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

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.lexer.NapileTokens;
import com.intellij.lang.ASTNode;

/**
 * @author max
 */
public class NapileMethodTypeImpl extends NapileElementImpl implements NapileMethodType
{
	public NapileMethodTypeImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	@NotNull
	@Override
	public List<NapileTypeReference> getTypeArguments()
	{
		return Collections.emptyList();
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitFunctionType(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitFunctionType(this, data);
	}

	@Override
	@Nullable
	public NapileCallParameterList getParameterList()
	{
		return (NapileCallParameterList) findChildByType(NapileNodes.CALL_PARAMETER_LIST);
	}

	@Override
	@NotNull
	public NapileElement[] getParameters()
	{
		NapileCallParameterList list = getParameterList();
		return list != null ? list.getParameters() : NapileElement.EMPTY_ARRAY;
	}

	@Override
	@Nullable
	public NapileTypeReference getReturnTypeRef()
	{
		return findChildByClass(NapileTypeReference.class);
	}

	@Override
	@NotNull
	public ASTNode getOpenBraceNode()
	{
		return getNode().findChildByType(NapileTokens.LBRACE);
	}

	@Override
	@Nullable
	@IfNotParsed
	public ASTNode getClosingBraceNode()
	{
		return getNode().findChildByType(NapileTokens.RBRACE);
	}

	@Override
	@Nullable
	public ASTNode getArrowNode()
	{
		return getNode().findChildByType(NapileTokens.ARROW);
	}
}
