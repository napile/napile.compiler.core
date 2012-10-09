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
import org.napile.compiler.lang.psi.stubs.PsiJetTypeParameterStub;
import org.napile.compiler.lang.psi.stubs.elements.JetStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.ArrayFactory;

/**
 * @author max
 */
public class NapileTypeParameter extends NapileNamedDeclarationStub<PsiJetTypeParameterStub>
{
	public static final NapileTypeParameter[] EMPTY_ARRAY = new NapileTypeParameter[0];

	public static final ArrayFactory<NapileTypeParameter> ARRAY_FACTORY = new ArrayFactory<NapileTypeParameter>()
	{
		@Override
		public NapileTypeParameter[] create(final int count)
		{
			return count == 0 ? EMPTY_ARRAY : new NapileTypeParameter[count];
		}
	};

	public NapileTypeParameter(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileTypeParameter(@NotNull PsiJetTypeParameterStub stub, @NotNull IStubElementType nodeType)
	{
		super(stub, nodeType);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitTypeParameter(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitTypeParameter(this, data);
	}

	@NotNull
	public NapileTypeReference[] getExtendsBound()
	{
		return findChildrenByClass(NapileTypeReference.class);
	}

	@NotNull
	public NapileParameterList[] getConstructorParameterLists()
	{
		return findChildrenByClass(NapileParameterList.class);
	}

	@NotNull
	@Override
	public IStubElementType getElementType()
	{
		return JetStubElementTypes.TYPE_PARAMETER;
	}
}
