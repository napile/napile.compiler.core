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

import org.napile.asm.tree.members.bytecode.Instruction;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.compiler.codegen.processors.PositionMarker;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @date 12:38/15.02.13
 */
public class MarkerInstructionAdapter extends DelegateInstructionAdapter
{
	private final PsiElement element;
	private final PositionMarker marker;

	public MarkerInstructionAdapter(InstructionAdapter adapter, PsiElement element, PositionMarker marker)
	{
		super(adapter);
		this.element = element;
		this.marker = marker;
	}

	@Override
	protected <T extends Instruction> T add(T t)
	{
		t = super.add(t);
		marker.mark(t, element);
		return t;
	}
}
