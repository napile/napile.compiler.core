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
package org.napile.idea.plugin.editor.highlight;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.lexer.NapileLexer;
import org.napile.compiler.lang.lexer.NapileTokens;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.StringEscapesTokenTypes;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

public class NapileHighlighter extends SyntaxHighlighterBase
{
	private static final Map<IElementType, TextAttributesKey> keys;

	@NotNull
	@Override
	public Lexer getHighlightingLexer()
	{
		return new NapileLexer();
	}

	@NotNull
	@Override
	public TextAttributesKey[] getTokenHighlights(IElementType tokenType)
	{
		return pack(keys.get(tokenType));
	}

	static
	{
		keys = new HashMap<IElementType, TextAttributesKey>();

		SyntaxHighlighterBase.fillMap(keys, NapileTokens.KEYWORDS, NapileHighlightingColors.KEYWORD);

		keys.put(NapileTokens.AS_SAFE, NapileHighlightingColors.KEYWORD);

		keys.put(NapileTokens.INTEGER_LITERAL, NapileHighlightingColors.NUMBER);
		keys.put(NapileTokens.FLOAT_LITERAL, NapileHighlightingColors.NUMBER);

		fillMap(keys, NapileTokens.OPERATIONS.minus(TokenSet.create(NapileTokens.IDENTIFIER)).minus(NapileTokens.KEYWORDS), NapileHighlightingColors.OPERATOR_SIGN);
		keys.put(NapileTokens.LPAR, NapileHighlightingColors.PARENTHESIS);
		keys.put(NapileTokens.RPAR, NapileHighlightingColors.PARENTHESIS);
		keys.put(NapileTokens.LBRACE, NapileHighlightingColors.BRACES);
		keys.put(NapileTokens.RBRACE, NapileHighlightingColors.BRACES);
		keys.put(NapileTokens.LBRACKET, NapileHighlightingColors.BRACKETS);
		keys.put(NapileTokens.RBRACKET, NapileHighlightingColors.BRACKETS);
		keys.put(NapileTokens.COMMA, NapileHighlightingColors.COMMA);
		keys.put(NapileTokens.SEMICOLON, NapileHighlightingColors.SEMICOLON);
		keys.put(NapileTokens.DOT, NapileHighlightingColors.DOT);
		keys.put(NapileTokens.ARROW, NapileHighlightingColors.ARROW);
		keys.put(StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN, NapileHighlightingColors.VALID_STRING_ESCAPE);
		keys.put(StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN, NapileHighlightingColors.INVALID_STRING_ESCAPE);
		keys.put(StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN, NapileHighlightingColors.INVALID_STRING_ESCAPE);

		keys.put(NapileTokens.CHARACTER_LITERAL, NapileHighlightingColors.STRING);
		keys.put(NapileTokens.STRING_LITERAL, NapileHighlightingColors.STRING);

		keys.put(NapileTokens.EOL_COMMENT, NapileHighlightingColors.LINE_COMMENT);
		keys.put(NapileTokens.BLOCK_COMMENT, NapileHighlightingColors.BLOCK_COMMENT);
		keys.put(NapileTokens.DOC_COMMENT, NapileHighlightingColors.DOC_COMMENT);

		keys.put(TokenType.BAD_CHARACTER, NapileHighlightingColors.BAD_CHARACTER);
	}
}
