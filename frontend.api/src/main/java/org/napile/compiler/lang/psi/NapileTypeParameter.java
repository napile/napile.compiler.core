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

package org.napile.compiler.lang.psi;

import org.jetbrains.annotations.NotNull;
import com.intellij.util.ArrayFactory;

/**
 * @author VISTALL
 * @since 20:05/19.10.12
 */
public interface NapileTypeParameter extends NapileNamedDeclaration, NapileSuperListOwner
{
	NapileTypeParameter[] EMPTY_ARRAY = new NapileTypeParameter[0];

	ArrayFactory<NapileTypeParameter> ARRAY_FACTORY = new ArrayFactory<NapileTypeParameter>()
	{
		@Override
		public NapileTypeParameter[] create(final int count)
		{
			return count == 0 ? EMPTY_ARRAY : new NapileTypeParameter[count];
		}
	};

	@NotNull
	NapileCallParameterList[] getConstructorParameterLists();
}
