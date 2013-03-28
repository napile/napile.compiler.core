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

package org.napile.idea.plugin.liveTemplates;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.NapileLanguage;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiUtilBase;

/**
 * @author VISTALL
 * @since 17:38/28.03.13
 */
public abstract class NapileLiveTemplateContextType extends TemplateContextType
{
	protected NapileLiveTemplateContextType(@NotNull @NonNls String id, @NotNull String presentableName, @Nullable Class<? extends TemplateContextType> baseContextType)
	{
		super(id, presentableName, baseContextType);
	}

	@Override
	public boolean isInContext(@NotNull final PsiFile file, final int offset)
	{
		if(PsiUtilBase.getLanguageAtOffset(file, offset).isKindOf(NapileLanguage.INSTANCE))
		{
			PsiElement element = file.findElementAt(offset);
			if(element instanceof PsiWhiteSpace)
			{
				return false;
			}

			return element != null && isInContext(element);
		}

		return false;
	}

	protected abstract boolean isInContext(@NotNull PsiElement element);
}
