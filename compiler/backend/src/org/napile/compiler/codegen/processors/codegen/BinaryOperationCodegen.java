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

package org.napile.compiler.codegen.processors.codegen;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.adapter.ReservedInstruction;
import org.napile.asm.tree.members.bytecode.impl.JumpIfInstruction;
import org.napile.asm.tree.members.bytecode.impl.JumpInstruction;
import org.napile.compiler.codegen.processors.ExpressionGenerator;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.psi.NapileBinaryExpression;

/**
 * @author VISTALL
 * @date 10:50/05.10.12
 */
public class BinaryOperationCodegen
{
	public static StackValue genAndAnd(@NotNull NapileBinaryExpression expression, @NotNull ExpressionGenerator gen, @NotNull InstructionAdapter instructs)
	{
		gen.gen(expression.getLeft(), TypeConstants.BOOL);

		StackValue.putTrue(instructs);

		ReservedInstruction ifSlot = instructs.reserve();

		gen.gen(expression.getRight(), TypeConstants.BOOL);

		StackValue.putTrue(instructs);

		ReservedInstruction ifSlot2 = instructs.reserve();

		StackValue.putTrue(instructs);

		ReservedInstruction ignoreFalseSlot = instructs.reserve();

		// if left of right exp failed jump to false
		instructs.replace(ifSlot, new JumpIfInstruction(instructs.size()));
		instructs.replace(ifSlot2, new JumpIfInstruction(instructs.size()));

		StackValue.putFalse(instructs);

		instructs.replace(ignoreFalseSlot, new JumpInstruction(instructs.size()));

		return StackValue.onStack(TypeConstants.BOOL);
	}

	public static StackValue genOrOr(@NotNull NapileBinaryExpression expression, @NotNull ExpressionGenerator gen, @NotNull InstructionAdapter instructs)
	{
		gen.gen(expression.getLeft(), TypeConstants.BOOL);

		StackValue.putTrue(instructs);

		ReservedInstruction ifSlot = instructs.reserve();

		// result
		StackValue.putTrue(instructs);

		ReservedInstruction skipNextSlot = instructs.reserve();

		// is first is failed - jump to right part
		instructs.replace(ifSlot, new JumpIfInstruction(instructs.size()));

		gen.gen(expression.getRight(), TypeConstants.BOOL);

		StackValue.putTrue(instructs);

		ReservedInstruction ifSlot2 = instructs.reserve();

		StackValue.putTrue(instructs);

		ReservedInstruction skipNextSlot2 = instructs.reserve();

		// jump to false
		instructs.replace(ifSlot2, new JumpIfInstruction(instructs.size()));

		// result
		StackValue.putFalse(instructs);

		// skips instructions - jump over expression
		instructs.replace(skipNextSlot, new JumpInstruction(instructs.size()));
		instructs.replace(skipNextSlot2, new JumpInstruction(instructs.size()));

		return StackValue.onStack(TypeConstants.BOOL);
	}
}
