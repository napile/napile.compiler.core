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
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.psi.stubs.NapilePsiParameterListStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;

/**
 * @author max
 */
public class NapileParameterListImpl extends NapileElementImplStub<NapilePsiParameterListStub> implements NapileParameterList
{
	private static final TokenSet PARAMETER_TYPES = TokenSet.create(NapileStubElementTypes.VALUE_PARAMETER, NapileNodes.REFERENCE_PARAMETER);

	public NapileParameterListImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileParameterListImpl(@NotNull NapilePsiParameterListStub stub)
	{
		super(stub, NapileStubElementTypes.VALUE_PARAMETER_LIST);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitParameterList(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitParameterList(this, data);
	}

	@Override
	public NapileElement[] getParameters()
	{
		return getStubOrPsiChildren(PARAMETER_TYPES, NapileElement.ARRAY_FACTORY);
	}
}
