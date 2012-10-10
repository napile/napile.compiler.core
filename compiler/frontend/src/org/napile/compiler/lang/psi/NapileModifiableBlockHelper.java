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

package org.napile.compiler.lang.psi;

import org.napile.compiler.psi.NapileDeclaration;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author Nikolay Krasko
 */
public final class NapileModifiableBlockHelper
{
	private NapileModifiableBlockHelper()
	{
	}

	/**
	 * Tested in OutOfBlockModificationTest
	 */
	public static boolean shouldChangeModificationCount(PsiElement place)
	{
		NapileDeclaration declaration = PsiTreeUtil.getParentOfType(place, NapileDeclaration.class, true);
		if(declaration != null)
		{
			if(declaration instanceof NapileNamedMethod)
			{
				NapileNamedMethod function = (NapileNamedMethod) declaration;
				if(function.hasDeclaredReturnType() || function.hasBlockBody())
				{
					return takePartInDeclarationTypeInference(function);
				}

				return shouldChangeModificationCount(function);
			}
			else if(declaration instanceof NapileProperty)
			{
				NapileProperty property = (NapileProperty) declaration;
				if(property.getPropertyTypeRef() != null)
				{
					return takePartInDeclarationTypeInference(property);
				}

				return shouldChangeModificationCount(property);
			}
			else if(declaration instanceof NapileFunctionLiteral)
			{
				// TODO: Check return type
				return shouldChangeModificationCount(declaration);
			}
		}

		return true;
	}

	private static boolean takePartInDeclarationTypeInference(PsiElement place)
	{
		NapileDeclaration declaration = PsiTreeUtil.getParentOfType(place, NapileDeclaration.class, true);
		if(declaration != null)
		{
			if(declaration instanceof NapileNamedMethod)
			{
				NapileNamedMethod function = (NapileNamedMethod) declaration;
				if(!function.hasDeclaredReturnType() && !function.hasBlockBody())
				{
					return true;
				}
			}
			else if(declaration instanceof NapileProperty)
			{
				NapileProperty property = (NapileProperty) declaration;
				if(property.getPropertyTypeRef() == null)
				{
					return true;
				}
			}

			return takePartInDeclarationTypeInference(declaration);
		}

		return false;
	}
}
