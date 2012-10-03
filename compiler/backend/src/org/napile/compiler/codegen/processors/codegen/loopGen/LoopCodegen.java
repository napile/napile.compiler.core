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
import org.napile.compiler.codegen.processors.ExpressionGenerator;
import org.napile.compiler.lang.psi.NapileLoopExpression;
import org.napile.compiler.lang.resolve.BindingTrace;

/**
 * @author VISTALL
 * @date 21:16/02.10.12
 */
public abstract class LoopCodegen<E extends NapileLoopExpression>
{
	protected final E expression;

	protected LoopCodegen(@NotNull E expression)
	{
		this.expression = expression;
	}

	protected void beforeLoop(ExpressionGenerator gen, InstructionAdapter instructions)
	{

	}

	protected void afterLoop(ExpressionGenerator gen, InstructionAdapter instructions)
	{

	}

	public void gen(@NotNull ExpressionGenerator gen, @NotNull InstructionAdapter instructions, @NotNull BindingTrace bindingTrace)
	{
		beforeLoop(gen, instructions);

		gen.gen(expression.getBody());

		afterLoop(gen, instructions);
	}
}
