/*
 * Copyright 2010-2012 napile.org
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
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.psi.NapileCallParameterAsReference;
import org.napile.compiler.lang.psi.NapileContainerNode;
import org.napile.compiler.lang.psi.NapileDeclarationImpl;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.lang.ASTNode;

/**
 * @author VISTALL
 * @since 13:34/06.09.12
 */
public class NapileCallParameterAsReferenceImpl extends NapileDeclarationImpl implements NapileCallParameterAsReference
{
	public NapileCallParameterAsReferenceImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitCallParameterAsReference(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitCallParameterAsReference(this, data);
	}

	@Override
	@Nullable
	public NapileSimpleNameExpression getReferenceExpression()
	{
		return (NapileSimpleNameExpression) findChildByType(NapileNodes.REFERENCE_EXPRESSION);
	}

	@Nullable
	@Override
	public NapileExpression getDefaultValue()
	{
		NapileContainerNode containerNode = (NapileContainerNode) findChildByType(NapileNodes.DEFAULT_VALUE_NODE);

		return containerNode == null ? null : containerNode.findChildByClass(NapileExpression.class);
	}
}
