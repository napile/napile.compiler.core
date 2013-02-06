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

package org.napile.compiler.codegen.processors.codegen.stackValue;

import org.napile.asm.resolve.name.FqName;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.PositionMarker;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;

/**
 * @author VISTALL
 * @date 10:20/13.01.13
 */
public class Outer extends StackValue
{
	private final TypeNode ownType;
	private final StackValue caller;

	public Outer(TypeNode ownType, FqName ownerFq, VariableNode variableNode)
	{
		super(null, variableNode.returnType);

		this.ownType = ownType;
		caller = StackValue.simpleVariableAccessor(ownerFq.child(variableNode.name), variableNode.returnType, CallableMethod.CallType.SPECIAL);
	}

	@Override
	public void put(TypeNode type, InstructionAdapter instructionAdapter, PositionMarker positionMarker)
	{
		instructionAdapter.localGet(0);
		caller.put(type, instructionAdapter, PositionMarker.EMPTY);
	}
}
