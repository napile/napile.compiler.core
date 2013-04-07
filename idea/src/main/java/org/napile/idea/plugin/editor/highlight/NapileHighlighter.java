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
	private static final Map<IElementType, TextAttributesKey> keys = new HashMap<IElementType, TextAttributesKey>();

	static
	{
		safeMap(keys, NapileTokens.KEYWORDS, NapileHighlightingColors.KEYWORD);

		safeMap(keys, NapileTokens.AS_SAFE, NapileHighlightingColors.KEYWORD);

		safeMap(keys, NapileTokens.INTEGER_LITERAL, NapileHighlightingColors.NUMBER);
		safeMap(keys, NapileTokens.FLOAT_LITERAL, NapileHighlightingColors.NUMBER);

		safeMap(keys, NapileTokens.OPERATIONS.minus(TokenSet.create(NapileTokens.IDENTIFIER)).minus(NapileTokens.KEYWORDS), NapileHighlightingColors.OPERATOR_SIGN);
		safeMap(keys, NapileTokens.LPAR, NapileHighlightingColors.PARENTHESIS);
		safeMap(keys, NapileTokens.RPAR, NapileHighlightingColors.PARENTHESIS);
		safeMap(keys, NapileTokens.LBRACE, NapileHighlightingColors.BRACES);
		safeMap(keys, NapileTokens.RBRACE, NapileHighlightingColors.BRACES);
		safeMap(keys, NapileTokens.LBRACKET, NapileHighlightingColors.BRACKETS);
		safeMap(keys, NapileTokens.RBRACKET, NapileHighlightingColors.BRACKETS);
		safeMap(keys, NapileTokens.COMMA, NapileHighlightingColors.COMMA);
		safeMap(keys, NapileTokens.SEMICOLON, NapileHighlightingColors.SEMICOLON);
		//safeMap(keys, NapileTokens.DOT, NapileHighlightingColors.DOT); conflict with OPERATIONS
		safeMap(keys, NapileTokens.ARROW, NapileHighlightingColors.ARROW);
		safeMap(keys, StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN, NapileHighlightingColors.VALID_STRING_ESCAPE);
		safeMap(keys, StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN, NapileHighlightingColors.INVALID_STRING_ESCAPE);
		safeMap(keys, StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN, NapileHighlightingColors.INVALID_STRING_ESCAPE);

		safeMap(keys, NapileTokens.CHARACTER_LITERAL, NapileHighlightingColors.STRING);
		safeMap(keys, NapileTokens.STRING_LITERAL, NapileHighlightingColors.STRING);

		safeMap(keys, NapileTokens.EOL_COMMENT, NapileHighlightingColors.LINE_COMMENT);
		safeMap(keys, NapileTokens.BLOCK_COMMENT, NapileHighlightingColors.BLOCK_COMMENT);
		safeMap(keys, NapileTokens.DOC_COMMENT, NapileHighlightingColors.DOC_COMMENT);

		safeMap(keys, TokenType.BAD_CHARACTER, NapileHighlightingColors.BAD_CHARACTER);
	}

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
}
