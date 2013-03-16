/*
 * Copyright 2010-2013 napile.org
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
import org.napile.compiler.lang.psi.NapileTypeParameter;
import org.napile.compiler.lang.psi.NapileTypeParameterList;
import org.napile.compiler.lang.psi.NapileTypeParameterListOwner;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.psi.stubs.NamedStub;
import com.intellij.psi.stubs.StubElement;

/**
 * @author VISTALL
 * @since 21:31/15.02.13
 */
public abstract class NXmlTypeParameterOwnerStub<T extends NamedStub> extends NXmlNamedDeclarationImpl<T> implements NapileTypeParameterListOwner
{
	protected NXmlTypeParameterOwnerStub(T stub)
	{
		super(stub);
	}

	@Nullable
	@Override
	public NapileTypeParameterList getTypeParameterList()
	{
		final StubElement stubByType = getStub().findChildStubByType(NapileStubElementTypes.TYPE_PARAMETER_LIST);
		return stubByType == null ? null : (NapileTypeParameterList) stubByType.getPsi();
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
