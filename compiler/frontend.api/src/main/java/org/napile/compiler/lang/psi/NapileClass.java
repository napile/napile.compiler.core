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
 * @date 21:23/09.10.12
 */
public interface NapileClass extends NapileNamedDeclaration, NapileTypeParameterListOwner, NapileClassLike
{
	NapileClass[] EMPTY_ARRAY = new NapileClass[0];

	ArrayFactory<NapileClass> ARRAY_FACTORY = new ArrayFactory<NapileClass>()
	{
		@Override
		public NapileClass[] create(int count)
		{
			return count == 0 ? EMPTY_ARRAY : new NapileClass[count];
		}
	};

	String getQualifiedName();

	@NotNull
	NapileConstructor[] getConstructors();
}
