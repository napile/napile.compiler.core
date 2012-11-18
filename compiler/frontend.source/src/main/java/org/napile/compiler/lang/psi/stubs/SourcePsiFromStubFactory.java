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

package org.napile.compiler.lang.psi.stubs;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.compiler.lang.psi.NapilePropertyParameter;
import org.napile.compiler.lang.psi.NapileTypeParameter;
import org.napile.compiler.lang.psi.NapileTypeParameterImpl;
import org.napile.compiler.lang.psi.NapileTypeParameterList;
import org.napile.compiler.lang.psi.NapileTypeParameterListImpl;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.psi.impl.NapileClassImpl;
import org.napile.compiler.lang.psi.impl.NapileNamedMacroImpl;
import org.napile.compiler.lang.psi.impl.NapileNamedMethodImpl;
import org.napile.compiler.lang.psi.impl.NapileVariableImpl;

/**
 * @author VISTALL
 * @date 18:36/19.10.12
 */
public class SourcePsiFromStubFactory implements NapilePsiFromStubFactory
{
	@NotNull
	@Override
	public NapileClass createClass(NapilePsiClassStub stub)
	{
		return new NapileClassImpl(stub);
	}

	@NotNull
	@Override
	public NapileNamedMethodOrMacro createNamedMethod(NapilePsiMethodOrMacroStub stub)
	{
		return new NapileNamedMethodImpl(stub);
	}

	@Override
	public NapileNamedMethodOrMacro createNamedMacro(NapilePsiMethodOrMacroStub stub)
	{
		return new NapileNamedMacroImpl(stub);
	}

	@NotNull
	@Override
	public NapileVariable createVariable(NapilePsiVariableStub stub)
	{
		return new NapileVariableImpl(stub);
	}

	@NotNull
	@Override
	public NapilePropertyParameter createParameter(NapilePsiMethodParameterStub stub)
	{
		return new NapilePropertyParameter(stub);
	}

	@NotNull
	@Override
	public NapileTypeParameterList createTypeParameterList(NapilePsiTypeParameterListStub stub)
	{
		return new NapileTypeParameterListImpl(stub);
	}

	@NotNull
	@Override
	public NapileTypeParameter createTypeParameter(NapilePsiTypeParameterStub stub)
	{
		return new NapileTypeParameterImpl(stub);
	}
}
