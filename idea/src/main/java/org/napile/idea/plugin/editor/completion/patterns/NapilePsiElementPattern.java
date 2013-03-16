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

package org.napile.idea.plugin.editor.completion.patterns;

import org.jetbrains.annotations.NotNull;
import com.intellij.patterns.InitialPatternCondition;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 17:28/24.01.13
 */
public class NapilePsiElementPattern<T extends PsiElement, Self extends NapilePsiElementPattern<T, Self>> extends PsiElementPattern<T, Self>
{
	public static class Capture<T extends PsiElement> extends NapilePsiElementPattern<T, Capture<T>>
	{
		protected Capture(Class<T> aClass)
		{
			super(aClass);
		}

		protected Capture(@NotNull InitialPatternCondition<T> condition)
		{
			super(condition);
		}
	}

	public static <T extends PsiElement> NapilePsiElementPattern<T, ?> element()
	{
		return new Capture<T>((Class<T>) PsiElement.class);
	}

	protected NapilePsiElementPattern(Class<T> aClass)
	{
		super(aClass);
	}

	protected NapilePsiElementPattern(@NotNull InitialPatternCondition<T> condition)
	{
		super(condition);
	}

}
