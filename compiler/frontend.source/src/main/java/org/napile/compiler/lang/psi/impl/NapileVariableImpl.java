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
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileTypeParameterListOwnerStub;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.psi.NapileVariableAccessor;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.psi.stubs.NapilePsiVariableStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author max
 */
public class NapileVariableImpl extends NapileTypeParameterListOwnerStub<NapilePsiVariableStub> implements NapileVariable
{
	public NapileVariableImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileVariableImpl(@NotNull NapilePsiVariableStub stub)
	{
		super(stub, NapileStubElementTypes.VARIABLE);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitVariable(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitVariable(this, data);
	}

	@Override
	@Nullable
	public NapileTypeReference getType()
	{
		return findChildByClass(NapileTypeReference.class);
	}

	@Override
	@Nullable
	public NapileExpression getInitializer()
	{
		return PsiTreeUtil.getNextSiblingOfType(findChildByType(NapileTokens.EQ), NapileExpression.class);
	}

	@NotNull
	@Override
	public NapileVariableAccessor[] getAccessors()
	{
		return findChildrenByClass(NapileVariableAccessor.class);
	}

	@Override
	@NotNull
	public ASTNode getVarNode()
	{
		return getNode().findChildByType(NapileTokens.VARIABLE_KEYWORDS);
	}
}
