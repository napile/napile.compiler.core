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

import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.PositionMarker;
import com.intellij.psi.PsiElement;

public class Local extends StackValue
{
	private final int index;

	public Local(PsiElement target, int index, TypeNode typeNode)
	{
		super(target, typeNode);
		this.index = index;
	}

	@Override
	public void put(TypeNode type, InstructionAdapter instructionAdapter, PositionMarker positionMarker)
	{
		join(instructionAdapter, positionMarker).localGet(index);

		castTo(type, instructionAdapter);
	}

	@Override
	public void store(TypeNode topOfStackType, InstructionAdapter instructionAdapter, PositionMarker positionMarker)
	{
		castTo(topOfStackType, getType(), instructionAdapter);

		join(instructionAdapter, positionMarker).localPut(index);
	}
}
