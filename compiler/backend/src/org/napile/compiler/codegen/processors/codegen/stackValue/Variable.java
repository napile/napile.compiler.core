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

import org.jetbrains.annotations.NotNull;
import org.napile.asm.adapters.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.VariableRef;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.lang.resolve.name.FqName;

/**
 * @author VISTALL
 * @date 23:21/18.09.12
 */
public class Variable extends StackValue
{
	private final VariableRef variableRef;
	private final boolean staticVar;

	public Variable(@NotNull FqName fqName, @NotNull TypeNode type, boolean s)
	{
		super(type);

		variableRef = new VariableRef(fqName, type);
		staticVar = s;
	}

	@Override
	public void put(TypeNode type, InstructionAdapter instructionAdapter)
	{
		if(staticVar)
			instructionAdapter.getVar(variableRef);
		else
			instructionAdapter.getStaticVar(variableRef);

		castTo(type, instructionAdapter);
	}

	@Override
	public void store(TypeNode topOfStackType, InstructionAdapter instructionAdapter)
	{
		if(staticVar)
			instructionAdapter.putToVar(variableRef);
		else
			instructionAdapter.putToStaticVar(variableRef);
	}

	@Override
	public void dupReceiver(InstructionAdapter v)
	{
		if(!staticVar)
			v.dup();
	}

	@Override
	public int receiverSize()
	{
		return staticVar ? 0 : 1;
	}
}
