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
import org.napile.compiler.lang.psi.*;

/**
 * @author VISTALL
 * @since 18:33/19.10.12
 */
public interface NapilePsiFromStubFactory
{
	@NotNull
	NapileClass createClass(NapilePsiClassStub stub);

	@NotNull
	NapileNamedMethod createNamedMethod(NapilePsiMethodStub stub);

	@NotNull
	NapileNamedMacro createNamedMacro(NapilePsiMacroStub stub);

	@NotNull
	NapileConstructor createConstructor(NapilePsiConstructorStub stub);

	@NotNull
	NapileVariable createVariable(NapilePsiVariableStub stub);

	@NotNull
	NapileEnumValue createEnumValue(NapilePsiEnumValueStub stub);

	@NotNull
	NapileCallParameterAsVariable createCallParameterAsVariable(NapilePsiCallParameterAsVariableStub stub);

	@NotNull
	NapileTypeParameterList createTypeParameterList(NapilePsiTypeParameterListStub stub);

	@NotNull
	NapileTypeParameter createTypeParameter(NapilePsiTypeParameterStub stub);

	@NotNull
	NapileCallParameterList createCallParameterList(NapilePsiCallParameterListStub stub);

	@NotNull
	NapileModifierList createModifierList(NapilePsiModifierListStub stub);
}
