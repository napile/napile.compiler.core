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

/*
 * @author max
 */
package org.napile.idea.plugin.highlighter;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.lexer.NapileLexer;
import org.napile.compiler.lang.lexer.NapileTokens;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

public class JetHighlighter extends SyntaxHighlighterBase
{
	private static final Map<IElementType, TextAttributesKey> keys;

	@NotNull
	public Lexer getHighlightingLexer()
	{
		return new NapileLexer();
	}

	@NotNull
	public TextAttributesKey[] getTokenHighlights(IElementType tokenType)
	{
		return pack(keys.get(tokenType));
	}

	static
	{
		keys = new HashMap<IElementType, TextAttributesKey>();

		fillMap(keys, NapileTokens.KEYWORDS, JetHighlightingColors.KEYWORD);

		keys.put(NapileTokens.AS_SAFE, JetHighlightingColors.KEYWORD);

		keys.put(NapileTokens.INTEGER_LITERAL, JetHighlightingColors.NUMBER);
		keys.put(NapileTokens.FLOAT_LITERAL, JetHighlightingColors.NUMBER);

		fillMap(keys, NapileTokens.OPERATIONS.minus(TokenSet.create(NapileTokens.IDENTIFIER)).minus(NapileTokens.KEYWORDS), JetHighlightingColors.OPERATOR_SIGN);
		keys.put(NapileTokens.LPAR, JetHighlightingColors.PARENTHESIS);
		keys.put(NapileTokens.RPAR, JetHighlightingColors.PARENTHESIS);
		keys.put(NapileTokens.LBRACE, JetHighlightingColors.BRACES);
		keys.put(NapileTokens.RBRACE, JetHighlightingColors.BRACES);
		keys.put(NapileTokens.LBRACKET, JetHighlightingColors.BRACKETS);
		keys.put(NapileTokens.RBRACKET, JetHighlightingColors.BRACKETS);
		keys.put(NapileTokens.COMMA, JetHighlightingColors.COMMA);
		keys.put(NapileTokens.SEMICOLON, JetHighlightingColors.SEMICOLON);
		keys.put(NapileTokens.DOT, JetHighlightingColors.DOT);
		keys.put(NapileTokens.ARROW, JetHighlightingColors.ARROW);

		keys.put(NapileTokens.OPEN_QUOTE, JetHighlightingColors.STRING);
		keys.put(NapileTokens.CLOSING_QUOTE, JetHighlightingColors.STRING);
		keys.put(NapileTokens.REGULAR_STRING_PART, JetHighlightingColors.STRING);
		keys.put(NapileTokens.LONG_TEMPLATE_ENTRY_END, JetHighlightingColors.STRING_ESCAPE);
		keys.put(NapileTokens.LONG_TEMPLATE_ENTRY_START, JetHighlightingColors.STRING_ESCAPE);
		keys.put(NapileTokens.SHORT_TEMPLATE_ENTRY_START, JetHighlightingColors.STRING_ESCAPE);

		keys.put(NapileTokens.ESCAPE_SEQUENCE, JetHighlightingColors.STRING_ESCAPE);

		keys.put(NapileTokens.CHARACTER_LITERAL, JetHighlightingColors.STRING);

		keys.put(NapileTokens.EOL_COMMENT, JetHighlightingColors.LINE_COMMENT);
		keys.put(NapileTokens.SHEBANG_COMMENT, JetHighlightingColors.LINE_COMMENT);
		keys.put(NapileTokens.BLOCK_COMMENT, JetHighlightingColors.BLOCK_COMMENT);
		keys.put(NapileTokens.DOC_COMMENT, JetHighlightingColors.DOC_COMMENT);

		keys.put(TokenType.BAD_CHARACTER, JetHighlightingColors.BAD_CHARACTER);
	}
}
