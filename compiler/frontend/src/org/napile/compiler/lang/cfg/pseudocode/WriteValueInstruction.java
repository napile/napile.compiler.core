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
import org.napile.compiler.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileNamedDeclaration;

/**
 * @author abreslav
 */
public class WriteValueInstruction extends InstructionWithNext
{
	@NotNull
	private final NapileElement lValue;

	public WriteValueInstruction(@NotNull NapileElement assignment, @NotNull NapileElement lValue)
	{
		super(assignment);
		this.lValue = lValue;
	}

	@NotNull
	public NapileElement getlValue()
	{
		return lValue;
	}

	@Override
	public void accept(InstructionVisitor visitor)
	{
		visitor.visitWriteValue(this);
	}

	@Override
	public String toString()
	{
		if(lValue instanceof NapileNamedDeclaration)
		{
			NapileNamedDeclaration value = (NapileNamedDeclaration) lValue;
			return "w(" + value.getName() + ")";
		}
		return "w(" + lValue.getText() + ")";
	}
}
