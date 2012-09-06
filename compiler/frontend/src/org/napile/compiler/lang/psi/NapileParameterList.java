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

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.NapileNodeTypes;
import org.napile.compiler.lang.psi.stubs.PsiJetParameterListStub;
import org.napile.compiler.lang.psi.stubs.elements.JetStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * @author max
 */
public class NapileParameterList extends NapileElementImplStub<PsiJetParameterListStub>
{
	private static final TokenSet PARAMETER_TYPES = TokenSet.create(JetStubElementTypes.VALUE_PARAMETER, NapileNodeTypes.IS_PARAMETER);

	public NapileParameterList(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileParameterList(@NotNull PsiJetParameterListStub stub, @NotNull IStubElementType nodeType)
	{
		super(stub, nodeType);
	}

	@NotNull
	@Override
	public IStubElementType getElementType()
	{
		return JetStubElementTypes.VALUE_PARAMETER_LIST;
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

	public List<NapileElement> getParameters()
	{
		return Arrays.asList(getStubOrPsiChildren(PARAMETER_TYPES, NapileElement.ARRAY_FACTORY));
	}
}
