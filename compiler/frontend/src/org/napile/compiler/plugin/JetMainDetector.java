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

package org.napile.compiler.plugin;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileNamedFunction;
import org.napile.compiler.lang.psi.NapileParameter;
import org.napile.compiler.lang.psi.NapileTypeReference;

/**
 * @author yole
 */
public class JetMainDetector
{
	private JetMainDetector()
	{
	}

	public static boolean hasMain(@NotNull List<NapileDeclaration> declarations)
	{
		return findMainFunction(declarations) != null;
	}

	public static boolean isMain(@NotNull NapileNamedFunction function)
	{
		if("main".equals(function.getName()))
		{
			List<NapileParameter> parameters = function.getValueParameters();
			if(parameters.size() == 1)
			{
				NapileTypeReference reference = parameters.get(0).getTypeReference();
				if(reference != null && reference.getText().equals("Array<String>"))
				{  // TODO correct check
					return true;
				}
			}
		}
		return false;
	}


	@Nullable
	private static NapileNamedFunction findMainFunction(@NotNull List<NapileDeclaration> declarations)
	{
		for(NapileDeclaration declaration : declarations)
		{
			if(declaration instanceof NapileNamedFunction)
			{
				NapileNamedFunction candidateFunction = (NapileNamedFunction) declaration;
				if(isMain(candidateFunction))
				{
					return candidateFunction;
				}
			}
		}
		return null;
	}
}
