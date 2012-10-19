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
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;

/**
 * @author svtk
 */
public class PseudocodeUtil
{
	@Nullable
	public static VariableDescriptor extractVariableDescriptorIfAny(@NotNull Instruction instruction, boolean onlyReference, @NotNull BindingContext bindingContext)
	{
		NapileElement element = null;
		if(instruction instanceof ReadValueInstruction)
		{
			element = ((ReadValueInstruction) instruction).getElement();
		}
		else if(instruction instanceof WriteValueInstruction)
		{
			element = ((WriteValueInstruction) instruction).getlValue();
		}
		else if(instruction instanceof VariableDeclarationInstruction)
		{
			element = ((VariableDeclarationInstruction) instruction).getVariableDeclarationElement();
		}
		return BindingContextUtils.extractVariableDescriptorIfAny(bindingContext, element, onlyReference);
	}
}
