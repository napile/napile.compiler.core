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
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileCallParameterAsVariable;
import org.napile.compiler.lang.psi.NapileContainerNode;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileNamedDeclarationStub;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.psi.stubs.NapilePsiCallParameterAsVariableStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.lang.ASTNode;

/**
 * @author max
 */
public class NapileCallParameterAsVariableImpl extends NapileNamedDeclarationStub<NapilePsiCallParameterAsVariableStub> implements NapileCallParameterAsVariable
{
	public NapileCallParameterAsVariableImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileCallParameterAsVariableImpl(@NotNull NapilePsiCallParameterAsVariableStub stub)
	{
		super(stub, NapileStubElementTypes.CALL_PARAMETER_AS_VARIABLE);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitCallParameterAsVariable(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitCallParameterAsVariable(this, data);
	}

	@Nullable
	@Override
	public NapileTypeReference getTypeReference()
	{
		return (NapileTypeReference) findChildByType(NapileNodes.TYPE_REFERENCE);
	}

	@Nullable
	@Override
	public NapileExpression getDefaultValue()
	{
		NapileContainerNode containerNode = (NapileContainerNode) findChildByType(NapileNodes.DEFAULT_VALUE_NODE);

		return containerNode == null ? null : containerNode.findChildByClass(NapileExpression.class);
	}

	@Override
	@Nullable
	public ASTNode getVarOrValNode()
	{
		return getNode().findChildByType(NapileTokens.VARIABLE_LIKE_KEYWORDS);
	}

	@Override
	public boolean isMutable()
	{
		ASTNode ast = getVarOrValNode();
		return ast == null || ast.getElementType() == NapileTokens.VAR_KEYWORD;
	}
}
