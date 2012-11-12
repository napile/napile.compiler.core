/*
 * Copyright 2010-2012 napile.org
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

package org.napile.idea.plugin.injection.text.highlighter;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.injection.text.lang.lexer.TextTokens;
import org.napile.idea.plugin.highlighter.InjectionSyntaxHighlighter;
import org.napile.idea.plugin.highlighter.SyntaxHighlighterUtil;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @date 15:36/12.11.12
 */
public class TextHighlighter implements InjectionSyntaxHighlighter
{
	private static final Map<IElementType, TextAttributesKey> cache;

	static
	{
		cache = new HashMap<IElementType, TextAttributesKey>(1);
		cache.put(TextTokens.HASH, TextHighlighterColors.EXPRESSION_RANGES);
		cache.put(TextTokens.LBRACE, TextHighlighterColors.EXPRESSION_RANGES);
		cache.put(TextTokens.RBRACE, TextHighlighterColors.EXPRESSION_RANGES);
	}

	@NotNull
	@Override
	public TextAttributesKey[] getTokenHighlights(IElementType tokenType)
	{
		return SyntaxHighlighterUtil.pack(cache.get(tokenType));
	}
}
