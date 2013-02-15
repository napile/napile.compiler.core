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

package org.napile.compiler.codegen.processors.adapters;

import java.util.Collection;
import java.util.Iterator;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.tree.members.bytecode.Instruction;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.tryCatch.TryCatchBlockNode;

/**
 * @author VISTALL
 * @date 12:35/15.02.13
 */
public class DelegateInstructionAdapter extends InstructionAdapter
{
	private final InstructionAdapter adapter;

	public DelegateInstructionAdapter(InstructionAdapter adapter)
	{
		this.adapter = adapter;
	}

	@Override
	public void tryCatch(@NotNull TryCatchBlockNode b)
	{
		adapter.tryCatch(b);
	}

	@NotNull
	@Override
	public Collection<Instruction> getInstructions()
	{
		return adapter.getInstructions();
	}

	@Override
	@NotNull
	public Iterator<Instruction> iterator()
	{
		return adapter.iterator();
	}

	@Override
	protected <T extends Instruction> T add(T t)
	{
		adapter.getInstructions().add(t);
		return t;
	}

	@Override
	public InstructionAdapter replace(@NotNull Instruction instruction1)
	{
		return adapter.replace(instruction1);
	}

	@Override
	public void visitLocalVariable(String name)
	{
		adapter.visitLocalVariable(name);
	}

	@Override
	public int size()
	{
		return adapter.size();
	}
	@Override
	public int getMaxLocals()
	{
		return adapter.getMaxLocals();
	}

	@NotNull
	@Override
	public Collection<TryCatchBlockNode> getTryCatchBlockNodes()
	{
		return adapter.getTryCatchBlockNodes();
	}
}
