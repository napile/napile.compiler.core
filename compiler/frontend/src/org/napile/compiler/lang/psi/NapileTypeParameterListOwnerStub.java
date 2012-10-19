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
import org.napile.compiler.lang.lexer.NapileNodes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.NamedStub;

/**
 * @author Nikolay Krasko
 */
public abstract class NapileTypeParameterListOwnerStub<T extends NamedStub> extends NapileNamedDeclarationStub<T> implements NapileTypeParameterListOwner
{
	public NapileTypeParameterListOwnerStub(@NotNull T stub, @NotNull IStubElementType nodeType)
	{
		super(stub, nodeType);
	}

	public NapileTypeParameterListOwnerStub(@NotNull ASTNode node)
	{
		super(node);
	}

	@Nullable
	@Override
	public NapileTypeParameterList getTypeParameterList()
	{
		return (NapileTypeParameterList) findChildByType(NapileNodes.TYPE_PARAMETER_LIST);
	}

	@Override
	@NotNull
	public NapileTypeParameter[] getTypeParameters()
	{
		NapileTypeParameterList list = getTypeParameterList();
		if(list == null)
			return NapileTypeParameter.EMPTY_ARRAY;

		return list.getParameters();
	}
}
