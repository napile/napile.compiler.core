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

import com.intellij.util.ArrayFactory;

/**
 * @author VISTALL
 * @since 19:51/01.12.12
 */
public interface NapileCallParameter extends NapileDeclaration
{
	NapileCallParameter[] EMPTY_ARRAY = new NapileCallParameter[0];

	ArrayFactory<NapileCallParameter> ARRAY_FACTORY = new ArrayFactory<NapileCallParameter>()
	{
		@Override
		public NapileCallParameter[] create(final int count)
		{
			return count == 0 ? EMPTY_ARRAY : new NapileCallParameter[count];
		}
	};

	NapileExpression getDefaultValue();
}
