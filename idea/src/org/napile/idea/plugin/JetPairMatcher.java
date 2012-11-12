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
package org.napile.idea.plugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.lexer.NapileTokens;
import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;

public class JetPairMatcher implements PairedBraceMatcher
{
	private final BracePair[] pairs = new BracePair[]{
			new BracePair(NapileTokens.LPAR, NapileTokens.RPAR, false),
			new BracePair(NapileTokens.LBRACE, NapileTokens.RBRACE, true),
			new BracePair(NapileTokens.LBRACKET, NapileTokens.RBRACKET, false)
	};

	public BracePair[] getPairs()
	{
		return pairs;
	}

	public boolean isPairedBracesAllowedBeforeType(@NotNull final IElementType lbraceType, @Nullable final IElementType contextType)
	{
		return NapileTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(contextType) || contextType == NapileTokens.SEMICOLON || contextType == NapileTokens.COMMA || contextType == NapileTokens.RPAR || contextType == NapileTokens.RBRACKET || contextType == NapileTokens.RBRACE || contextType == NapileTokens.LBRACE;
	}

	public int getCodeConstructStart(final PsiFile file, final int openingBraceOffset)
	{
		return openingBraceOffset;
	}
}
