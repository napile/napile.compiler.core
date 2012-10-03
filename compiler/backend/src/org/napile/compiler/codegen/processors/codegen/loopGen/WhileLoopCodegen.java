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

package org.napile.compiler.codegen.processors.codegen.loopGen;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.adapters.InstructionAdapter;
import org.napile.asm.adapters.ReservedInstruction;
import org.napile.asm.tree.members.bytecode.impl.JumpIfInstruction;
import org.napile.compiler.codegen.processors.ExpressionGenerator;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.psi.NapileWhileExpression;

/**
 * @author VISTALL
 * @date 11:29/03.10.12
 */
public class WhileLoopCodegen extends LoopCodegen<NapileWhileExpression>
{
	private ReservedInstruction ifSlot;

	public WhileLoopCodegen(@NotNull NapileWhileExpression expression)
	{
		super(expression);
	}

	@Override
	protected void beforeLoop(ExpressionGenerator gen, InstructionAdapter instructions)
	{
		super.beforeLoop(gen, instructions);

		gen.gen(expression.getCondition(), TypeConstants.BOOL);

		StackValue.putTrue(instructions);

		ifSlot = instructions.reserve();
	}

	@Override
	protected void afterLoop(ExpressionGenerator gen, InstructionAdapter instructions)
	{
		int jumpOutPos = instructions.size() + 1;

		instructions.replace(ifSlot, new JumpIfInstruction(jumpOutPos));

		instructions.jump(firstPos);
	}
}
