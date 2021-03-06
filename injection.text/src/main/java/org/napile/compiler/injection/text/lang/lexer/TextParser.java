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

package org.napile.compiler.injection.text.lang.lexer;

import org.jetbrains.annotations.NotNull;
import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageVersion;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * @author VISTALL
 * @since 21:24/09.11.12
 */
public class TextParser implements PsiParser
{
	private static final TokenSet REMAP = TokenSet.create(TextTokens.HASH, TextTokens.LBRACE, TextTokens.RBRACE);

	@NotNull
	@Override
	public ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder, @NotNull LanguageVersion languageVersion)
	{
		PsiBuilder.Marker rootMarker = builder.mark();
		while(!builder.eof())
		{
			if(REMAP.contains(builder.getTokenType()))
			{
				builder.remapCurrentToken(TextTokens.TEXT_PART);
			}
			builder.advanceLexer();
		}

		rootMarker.done(root);
		return builder.getTreeBuilt();
	}
}
