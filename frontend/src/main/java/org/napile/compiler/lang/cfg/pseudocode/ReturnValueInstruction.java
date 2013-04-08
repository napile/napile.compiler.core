/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.napile.compiler.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.cfg.Label;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpression;

/**
 * @author abreslav
 */
public class ReturnValueInstruction extends AbstractJumpInstruction implements JetElementInstruction
{

	private final NapileElement element;

	public ReturnValueInstruction(@NotNull NapileExpression returnExpression, @NotNull Label targetLabel)
	{
		super(targetLabel);
		this.element = returnExpression;
	}

	@Override
	public void accept(InstructionVisitor visitor)
	{
		visitor.visitReturnValue(this);
	}

	@Override
	public String toString()
	{
		return "ret(*) " + getTargetLabel();
	}

	@NotNull
	@Override
	public NapileElement getElement()
	{
		return element;
	}
}
