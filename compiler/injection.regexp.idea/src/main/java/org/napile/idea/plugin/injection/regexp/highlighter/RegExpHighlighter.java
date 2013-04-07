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

import org.napile.compiler.injection.regexp.lang.parser.RegExpTokens;
import org.napile.idea.plugin.highlighter.InjectionSyntaxHighlighter;
import org.napile.idea.plugin.highlighter.SyntaxHighlighterUtil;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.StringEscapesTokenTypes;
import com.intellij.psi.TokenType;

/**
 * @author VISTALL
 * @since 7:38/08.11.12
 */
public class RegExpHighlighter extends InjectionSyntaxHighlighter
{
	static final TextAttributesKey NUMBER = TextAttributesKey.createTextAttributesKey("NAPILE-REGEXP.META", DefaultLanguageHighlighterColors.NUMBER);
	static final TextAttributesKey META = TextAttributesKey.createTextAttributesKey("REGEXP.META", DefaultLanguageHighlighterColors.KEYWORD);
	static final TextAttributesKey INVALID_CHARACTER_ESCAPE = TextAttributesKey.createTextAttributesKey("REGEXP.INVALID_STRING_ESCAPE", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE);
	static final TextAttributesKey BAD_CHARACTER = TextAttributesKey.createTextAttributesKey("REGEXP.BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);
	static final TextAttributesKey REDUNDANT_ESCAPE = TextAttributesKey.createTextAttributesKey("REGEXP.REDUNDANT_ESCAPE", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
	static final TextAttributesKey PARENTHS = TextAttributesKey.createTextAttributesKey("REGEXP.PARENTHS", DefaultLanguageHighlighterColors.PARENTHESES);
	static final TextAttributesKey BRACES = TextAttributesKey.createTextAttributesKey("REGEXP.BRACES", DefaultLanguageHighlighterColors.BRACES);
	static final TextAttributesKey BRACKETS = TextAttributesKey.createTextAttributesKey("REGEXP.BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
	static final TextAttributesKey COMMA = TextAttributesKey.createTextAttributesKey("REGEXP.COMMA", DefaultLanguageHighlighterColors.COMMA);
	static final TextAttributesKey ESC_CHARACTER = TextAttributesKey.createTextAttributesKey("REGEXP.ESC_CHARACTER", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
	static final TextAttributesKey CHAR_CLASS = TextAttributesKey.createTextAttributesKey("REGEXP.CHAR_CLASS", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
	static final TextAttributesKey QUOTE_CHARACTER = TextAttributesKey.createTextAttributesKey("REGEXP.QUOTE_CHARACTER", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
	static final TextAttributesKey COMMENT = TextAttributesKey.createTextAttributesKey("REGEXP.COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);

	public RegExpHighlighter()
	{
		SyntaxHighlighterUtil.fillMap(keys, RegExpTokens.KEYWORDS, META);

		keys.put(StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN, INVALID_CHARACTER_ESCAPE);
		keys.put(StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN, INVALID_CHARACTER_ESCAPE);

		keys.put(TokenType.BAD_CHARACTER, BAD_CHARACTER);
		keys.put(RegExpTokens.BAD_HEX_VALUE, INVALID_CHARACTER_ESCAPE);
		keys.put(RegExpTokens.BAD_OCT_VALUE, INVALID_CHARACTER_ESCAPE);

		keys.put(RegExpTokens.PROPERTY, CHAR_CLASS);

		keys.put(RegExpTokens.ESC_CHARACTER, ESC_CHARACTER);
		keys.put(RegExpTokens.UNICODE_CHAR, ESC_CHARACTER);
		keys.put(RegExpTokens.HEX_CHAR, ESC_CHARACTER);
		keys.put(RegExpTokens.OCT_CHAR, ESC_CHARACTER);
		keys.put(RegExpTokens.CHAR_CLASS, ESC_CHARACTER);
		keys.put(RegExpTokens.BOUNDARY, ESC_CHARACTER);
		keys.put(RegExpTokens.CTRL, ESC_CHARACTER);
		keys.put(RegExpTokens.ESC_CTRL_CHARACTER, ESC_CHARACTER);

		keys.put(RegExpTokens.REDUNDANT_ESCAPE, REDUNDANT_ESCAPE);

		keys.put(RegExpTokens.QUOTE_BEGIN, QUOTE_CHARACTER);
		keys.put(RegExpTokens.QUOTE_END, QUOTE_CHARACTER);

		keys.put(RegExpTokens.GROUP_BEGIN, PARENTHS);
		keys.put(RegExpTokens.GROUP_END, PARENTHS);

		keys.put(RegExpTokens.LBRACE, BRACES);
		keys.put(RegExpTokens.RBRACE, BRACES);

		keys.put(RegExpTokens.NUMBER, NUMBER);

		keys.put(RegExpTokens.CLASS_BEGIN, BRACKETS);
		keys.put(RegExpTokens.CLASS_END, BRACKETS);

		keys.put(RegExpTokens.COMMA, COMMA);

		keys.put(RegExpTokens.COMMENT, COMMENT);
	}
}
