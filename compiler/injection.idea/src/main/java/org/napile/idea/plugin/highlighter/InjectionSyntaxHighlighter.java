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

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.injection.lexer.InjectionTokens;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @since 14:11/07.04.13
 */
public class InjectionSyntaxHighlighter
{
	public static final InjectionSyntaxHighlighter DEFAULT = new InjectionSyntaxHighlighter();

	protected final Map<IElementType, TextAttributesKey> keys = new HashMap<IElementType, TextAttributesKey>();

	protected InjectionSyntaxHighlighter()
	{
		keys.put(InjectionTokens.INNER_EXPRESSION_START, InjectionHighlightColors.INJECTION_INNER_EXPRESSION_MARK);
		keys.put(InjectionTokens.INNER_EXPRESSION_STOP, InjectionHighlightColors.INJECTION_INNER_EXPRESSION_MARK);
	}

	@NotNull
	public final TextAttributesKey[] getTokenHighlights(IElementType elementType)
	{
		return SyntaxHighlighterUtil.pack(keys.get(elementType));
	}
}
