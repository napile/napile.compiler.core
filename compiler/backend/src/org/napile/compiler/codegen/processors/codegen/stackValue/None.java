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

package org.napile.compiler.codegen.processors.codegen.stackValue;

import org.napile.asm.adapters.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;

/**
 * @author VISTALL
 * @date 18:48/08.09.12
 */
public class None extends StackValue
{
	public static final None INSTANCE = new None();

	public None()
	{
		super(TypeConstants.NULL);
	}

	@Override
	public void put(TypeNode type, InstructionAdapter instructionAdapter)
	{
		castTo(type, instructionAdapter);
	}
}
