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

package org.napile.idea.plugin.highlighter;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.NapileLanguage;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

/**
 * @author VISTALL
 * @since 14:12/07.04.13
 */
public class InjectionHighlightColors
{
	public static final TextAttributesKey INJECTION_INNER_EXPRESSION_MARK = createKey(NapileLanguage.INSTANCE, "INJECTION_INNER_EXPRESSION_MARK", DefaultLanguageHighlighterColors.OPERATION_SIGN);

	public static final TextAttributesKey INJECTION_BLOCK = createKey(NapileLanguage.INSTANCE, "INJECTION_BLOCK", DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR);

	@NotNull
	public static TextAttributesKey createKey(@NotNull Language language, @NotNull TextAttributesKey defaultKey)
	{
		return createKey(language, defaultKey.getExternalName(), defaultKey);
	}

	@NotNull
	public static TextAttributesKey createKey(@NotNull Language language, @NotNull String name, @NotNull TextAttributesKey defaultKey)
	{
		return TextAttributesKey.createTextAttributesKey(language.getID() + "." + name, defaultKey);
	}
}
