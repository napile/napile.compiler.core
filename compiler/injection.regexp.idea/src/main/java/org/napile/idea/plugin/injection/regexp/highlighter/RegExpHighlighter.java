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

import org.intellij.lang.regexp.RegExpTT;
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
 * @date 7:38/08.11.12
 */
public class RegExpHighlighter implements InjectionSyntaxHighlighter
{
	private static final Map<IElementType, TextAttributesKey> keys1;

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

		SyntaxHighlighterUtil.fillMap(keys1, RegExpTT.KEYWORDS, META);

		keys1.put(StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN, INVALID_CHARACTER_ESCAPE);
		keys1.put(StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN, INVALID_CHARACTER_ESCAPE);

		keys1.put(TokenType.BAD_CHARACTER, BAD_CHARACTER);
		keys1.put(RegExpTT.BAD_HEX_VALUE, INVALID_CHARACTER_ESCAPE);
		keys1.put(RegExpTT.BAD_OCT_VALUE, INVALID_CHARACTER_ESCAPE);

		keys1.put(RegExpTT.PROPERTY, CHAR_CLASS);

		keys1.put(RegExpTT.ESC_CHARACTER, ESC_CHARACTER);
		keys1.put(RegExpTT.UNICODE_CHAR, ESC_CHARACTER);
		keys1.put(RegExpTT.HEX_CHAR, ESC_CHARACTER);
		keys1.put(RegExpTT.OCT_CHAR, ESC_CHARACTER);
		keys1.put(RegExpTT.CHAR_CLASS, ESC_CHARACTER);
		keys1.put(RegExpTT.BOUNDARY, ESC_CHARACTER);
		keys1.put(RegExpTT.CTRL, ESC_CHARACTER);
		keys1.put(RegExpTT.ESC_CTRL_CHARACTER, ESC_CHARACTER);

		keys1.put(RegExpTT.REDUNDANT_ESCAPE, REDUNDANT_ESCAPE);

		keys1.put(RegExpTT.QUOTE_BEGIN, QUOTE_CHARACTER);
		keys1.put(RegExpTT.QUOTE_END, QUOTE_CHARACTER);

		keys1.put(RegExpTT.GROUP_BEGIN, PARENTHS);
		keys1.put(RegExpTT.GROUP_END, PARENTHS);

		keys1.put(RegExpTT.LBRACE, BRACES);
		keys1.put(RegExpTT.RBRACE, BRACES);

		keys1.put(RegExpTT.CLASS_BEGIN, BRACKETS);
		keys1.put(RegExpTT.CLASS_END, BRACKETS);

		keys1.put(RegExpTT.COMMA, COMMA);

		keys1.put(RegExpTT.COMMENT, COMMENT);
	}

	@NotNull
	@Override
	public TextAttributesKey[] getTokenHighlights(IElementType tokenType)
	{
		return SyntaxHighlighterUtil.pack(keys1.get(tokenType));
	}
}
