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

package org.napile.idea.plugin.injection.regexp.highlighter;

import java.util.HashMap;
import java.util.Map;

import org.napile.compiler.injection.regexp.lang.parser.RegExpTokens;
import org.jetbrains.annotations.NotNull;
import org.napile.idea.plugin.highlighter.InjectionSyntaxHighlighter;
import org.napile.idea.plugin.highlighter.SyntaxHighlighterUtil;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.SyntaxHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.StringEscapesTokenTypes;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @since 7:38/08.11.12
 */
public class RegExpHighlighter implements InjectionSyntaxHighlighter
{
	private static final Map<IElementType, TextAttributesKey> keys1;

	static final TextAttributesKey NUMBER = TextAttributesKey.createTextAttributesKey("NAPILE-REGEXP.META", SyntaxHighlighterColors.NUMBER.getDefaultAttributes());
	static final TextAttributesKey META = TextAttributesKey.createTextAttributesKey("REGEXP.META", SyntaxHighlighterColors.KEYWORD.getDefaultAttributes());
	static final TextAttributesKey INVALID_CHARACTER_ESCAPE = TextAttributesKey.createTextAttributesKey("REGEXP.INVALID_STRING_ESCAPE", SyntaxHighlighterColors.INVALID_STRING_ESCAPE.getDefaultAttributes());
	static final TextAttributesKey BAD_CHARACTER = TextAttributesKey.createTextAttributesKey("REGEXP.BAD_CHARACTER", HighlighterColors.BAD_CHARACTER.getDefaultAttributes());
	static final TextAttributesKey REDUNDANT_ESCAPE = TextAttributesKey.createTextAttributesKey("REGEXP.REDUNDANT_ESCAPE", SyntaxHighlighterColors.VALID_STRING_ESCAPE.getDefaultAttributes());
	static final TextAttributesKey PARENTHS = TextAttributesKey.createTextAttributesKey("REGEXP.PARENTHS", SyntaxHighlighterColors.PARENTHS.getDefaultAttributes());
	static final TextAttributesKey BRACES = TextAttributesKey.createTextAttributesKey("REGEXP.BRACES", SyntaxHighlighterColors.BRACES.getDefaultAttributes());
	static final TextAttributesKey BRACKETS = TextAttributesKey.createTextAttributesKey("REGEXP.BRACKETS", SyntaxHighlighterColors.BRACKETS.getDefaultAttributes());
	static final TextAttributesKey COMMA = TextAttributesKey.createTextAttributesKey("REGEXP.COMMA", SyntaxHighlighterColors.COMMA.getDefaultAttributes());
	static final TextAttributesKey ESC_CHARACTER = TextAttributesKey.createTextAttributesKey("REGEXP.ESC_CHARACTER", SyntaxHighlighterColors.VALID_STRING_ESCAPE.getDefaultAttributes());
	static final TextAttributesKey CHAR_CLASS = TextAttributesKey.createTextAttributesKey("REGEXP.CHAR_CLASS", SyntaxHighlighterColors.VALID_STRING_ESCAPE.getDefaultAttributes());
	static final TextAttributesKey QUOTE_CHARACTER = TextAttributesKey.createTextAttributesKey("REGEXP.QUOTE_CHARACTER", SyntaxHighlighterColors.VALID_STRING_ESCAPE.getDefaultAttributes());
	static final TextAttributesKey COMMENT = TextAttributesKey.createTextAttributesKey("REGEXP.COMMENT", SyntaxHighlighterColors.LINE_COMMENT.getDefaultAttributes());

	static
	{
		keys1 = new HashMap<IElementType, TextAttributesKey>();

		SyntaxHighlighterUtil.fillMap(keys1, RegExpTokens.KEYWORDS, META);

		keys1.put(StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN, INVALID_CHARACTER_ESCAPE);
		keys1.put(StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN, INVALID_CHARACTER_ESCAPE);

		keys1.put(TokenType.BAD_CHARACTER, BAD_CHARACTER);
		keys1.put(RegExpTokens.BAD_HEX_VALUE, INVALID_CHARACTER_ESCAPE);
		keys1.put(RegExpTokens.BAD_OCT_VALUE, INVALID_CHARACTER_ESCAPE);

		keys1.put(RegExpTokens.PROPERTY, CHAR_CLASS);

		keys1.put(RegExpTokens.ESC_CHARACTER, ESC_CHARACTER);
		keys1.put(RegExpTokens.UNICODE_CHAR, ESC_CHARACTER);
		keys1.put(RegExpTokens.HEX_CHAR, ESC_CHARACTER);
		keys1.put(RegExpTokens.OCT_CHAR, ESC_CHARACTER);
		keys1.put(RegExpTokens.CHAR_CLASS, ESC_CHARACTER);
		keys1.put(RegExpTokens.BOUNDARY, ESC_CHARACTER);
		keys1.put(RegExpTokens.CTRL, ESC_CHARACTER);
		keys1.put(RegExpTokens.ESC_CTRL_CHARACTER, ESC_CHARACTER);

		keys1.put(RegExpTokens.REDUNDANT_ESCAPE, REDUNDANT_ESCAPE);

		keys1.put(RegExpTokens.QUOTE_BEGIN, QUOTE_CHARACTER);
		keys1.put(RegExpTokens.QUOTE_END, QUOTE_CHARACTER);

		keys1.put(RegExpTokens.GROUP_BEGIN, PARENTHS);
		keys1.put(RegExpTokens.GROUP_END, PARENTHS);

		keys1.put(RegExpTokens.LBRACE, BRACES);
		keys1.put(RegExpTokens.RBRACE, BRACES);

		keys1.put(RegExpTokens.NUMBER, NUMBER);

		keys1.put(RegExpTokens.CLASS_BEGIN, BRACKETS);
		keys1.put(RegExpTokens.CLASS_END, BRACKETS);

		keys1.put(RegExpTokens.COMMA, COMMA);

		keys1.put(RegExpTokens.COMMENT, COMMENT);
	}

	@NotNull
	@Override
	public TextAttributesKey[] getTokenHighlights(IElementType tokenType)
	{
		return SyntaxHighlighterUtil.pack(keys1.get(tokenType));
	}
}
