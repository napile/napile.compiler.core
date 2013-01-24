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

package org.napile.compiler.codegen.processors.visitors.loopCodegen;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.adapter.ReservedInstruction;
import org.napile.compiler.codegen.processors.ExpressionCodegen;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileLoopExpression;

/**
 * @author VISTALL
 * @date 21:16/02.10.12
 */
public abstract class LoopCodegen<E extends NapileLoopExpression>
{
	protected final E expression;
	protected int firstPos;

	private final List<ReservedInstruction> breakInstructions = new ArrayList<ReservedInstruction>();
	private final List<ReservedInstruction> continueInstructions = new ArrayList<ReservedInstruction>();

	protected LoopCodegen(@NotNull E expression)
	{
		this.expression = expression;
	}

	public String getName()
	{
		return null;
	}

	protected void beforeLoop(ExpressionCodegen gen, InstructionAdapter instructions)
	{
		firstPos = instructions.size();
	}

	protected void afterLoop(ExpressionCodegen gen, InstructionAdapter instructions)
	{

	}

	public void gen(@NotNull ExpressionCodegen gen)
	{
		beforeLoop(gen, gen.instructs);

		NapileExpression bodyExpression = expression.getBody();
		if(bodyExpression != null)
			gen.gen(bodyExpression);

		afterLoop(gen, gen.instructs);

		final int nextPosAfterLoop = gen.instructs.size();
		for(ReservedInstruction i : breakInstructions)
			gen.instructs.replace(i).jump(nextPosAfterLoop);

		for(ReservedInstruction i : continueInstructions)
			gen.instructs.replace(i).jump(firstPos);
	}

	public void addContinue(@NotNull InstructionAdapter instructions)
	{
		continueInstructions.add(instructions.reserve());
	}

	public void addBreak(@NotNull InstructionAdapter instructions)
	{
		breakInstructions.add(instructions.reserve());
	}
}
