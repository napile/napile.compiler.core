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

package org.jetbrains.jet.plugin;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.psi.JetTypeReference;

/**
 * @author yole
 */
public class JetMainDetector
{
	private JetMainDetector()
	{
	}

	public static boolean hasMain(@NotNull List<JetDeclaration> declarations)
	{
		return findMainFunction(declarations) != null;
	}

	public static boolean isMain(@NotNull JetNamedFunction function)
	{
		if("main".equals(function.getName()))
		{
			List<JetParameter> parameters = function.getValueParameters();
			if(parameters.size() == 1)
			{
				JetTypeReference reference = parameters.get(0).getTypeReference();
				if(reference != null && reference.getText().equals("Array<String>"))
				{  // TODO correct check
					return true;
				}
			}
		}
		return false;
	}


	@Nullable
	private static JetNamedFunction findMainFunction(@NotNull List<JetDeclaration> declarations)
	{
		for(JetDeclaration declaration : declarations)
		{
			if(declaration instanceof JetNamedFunction)
			{
				JetNamedFunction candidateFunction = (JetNamedFunction) declaration;
				if(isMain(candidateFunction))
				{
					return candidateFunction;
				}
			}
		}
		return null;
	}
}
