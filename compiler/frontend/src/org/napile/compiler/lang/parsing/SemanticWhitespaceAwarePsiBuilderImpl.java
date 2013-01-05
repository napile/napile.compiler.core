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

package org.napile.compiler.lang.parsing;


import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.napile.compiler.lang.lexer.NapileTokens;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.impl.PsiBuilderAdapter;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

/**
 * @author abreslav
 */
public class SemanticWhitespaceAwarePsiBuilderImpl extends PsiBuilderAdapter implements SemanticWhitespaceAwarePsiBuilder
{
	private static final Map<IElementType, Integer> ADDITIONAL_SIZE = new HashMap<IElementType, Integer>()
	{
		{
			put(NapileTokens.SAFE_ACCESS, 1);
			put(NapileTokens.ELVIS, 1);
			put(NapileTokens.EXCLEXCL, 1);
			put(NapileTokens.GTGT, 1);
			put(NapileTokens.GTGTEQ, 1);
			put(NapileTokens.GTGTGT, 2);
			put(NapileTokens.GTGTGTEQ, 2);
		}
	};

	private final Stack<Boolean> joinComplexTokens = new Stack<Boolean>();

	private final Stack<Boolean> newlinesEnabled = new Stack<Boolean>();

	public SemanticWhitespaceAwarePsiBuilderImpl(final PsiBuilder delegate)
	{
		super(delegate);
		newlinesEnabled.push(true);
		joinComplexTokens.push(true);
	}

	@Override
	public boolean newlineBeforeCurrentToken()
	{
		if(!newlinesEnabled.peek())
			return false;

		if(eof())
			return true;

		// TODO: maybe, memoize this somehow?
		for(int i = 1; i <= getCurrentOffset(); i++)
		{
			IElementType previousToken = rawLookup(-i);

			if(previousToken == NapileTokens.BLOCK_COMMENT || previousToken == NapileTokens.DOC_COMMENT || previousToken == NapileTokens.EOL_COMMENT)
			{
				continue;
			}

			if(previousToken != TokenType.WHITE_SPACE)
			{
				break;
			}

			int previousTokenStart = rawTokenTypeStart(-i);
			int previousTokenEnd = rawTokenTypeStart(-i + 1);

			assert previousTokenStart >= 0;
			assert previousTokenEnd < getOriginalText().length();

			for(int j = previousTokenStart; j < previousTokenEnd; j++)
			{
				if(getOriginalText().charAt(j) == '\n')
				{
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void disableNewlines()
	{
		newlinesEnabled.push(false);
	}

	@Override
	public void enableNewlines()
	{
		newlinesEnabled.push(true);
	}

	@Override
	public void restoreNewlinesState()
	{
		assert newlinesEnabled.size() > 1;
		newlinesEnabled.pop();
	}

	private boolean joinComplexTokens()
	{
		return joinComplexTokens.peek();
	}

	@Override
	public void restoreJoiningComplexTokensState()
	{
		joinComplexTokens.pop();
	}

	@Override
	public void enableJoiningComplexTokens()
	{
		joinComplexTokens.push(true);
	}

	@Override
	public void disableJoiningComplexTokens()
	{
		joinComplexTokens.push(false);
	}

	@Override
	public IElementType getTokenType()
	{
		if(!joinComplexTokens())
			return super.getTokenType();
		return getJoinedTokenType(super.getTokenType(), 1);
	}

	private IElementType getJoinedTokenType(IElementType rawTokenType, int rawLookupSteps)
	{
		if(rawTokenType == NapileTokens.QUEST)
		{
			IElementType nextRawToken = rawLookup(rawLookupSteps);
			if(nextRawToken == NapileTokens.DOT)
				return NapileTokens.SAFE_ACCESS;
			if(nextRawToken == NapileTokens.COLON)
				return NapileTokens.ELVIS;
		}
		else if(rawTokenType == NapileTokens.EXCL)
		{
			IElementType nextRawToken = rawLookup(rawLookupSteps);
			if(nextRawToken == NapileTokens.EXCL)
				return NapileTokens.EXCLEXCL;
		}
		else if(rawTokenType == NapileTokens.GT)
		{
			IElementType nextRawToken = rawLookup(rawLookupSteps);
			if(nextRawToken == NapileTokens.GT)
			{
				nextRawToken = rawLookup(1 + rawLookupSteps);
				if(nextRawToken == NapileTokens.GT)
					return NapileTokens.GTGTGT;
				else if(nextRawToken == NapileTokens.GTEQ)
					return NapileTokens.GTGTGTEQ;
				else
					return NapileTokens.GTGT;
			}
			else if(nextRawToken == NapileTokens.GTEQ)
				return NapileTokens.GTGTEQ;
		}
		return rawTokenType;
	}

	@Override
	public void advanceLexer()
	{
		if(!joinComplexTokens())
		{
			super.advanceLexer();
			return;
		}

		IElementType tokenType = getTokenType();
		Integer size = ADDITIONAL_SIZE.get(tokenType);
		if(size != null)
		{
			Marker mark = mark();
			super.advanceLexer();

			for(int i = 0; i < size; i++)
				super.advanceLexer();
			mark.collapse(tokenType);
		}
		else
			super.advanceLexer();
	}

	@Override
	public String getTokenText()
	{
		if(!joinComplexTokens())
			return super.getTokenText();
		IElementType tokenType = getTokenType();
		if(ADDITIONAL_SIZE.containsKey(tokenType))
		{
			if(tokenType == NapileTokens.ELVIS)
				return "?:";
			if(tokenType == NapileTokens.SAFE_ACCESS)
				return "?.";
			if(tokenType == NapileTokens.GTGT)
				return ">>";
			if(tokenType == NapileTokens.GTGTGT)
				return ">>>";
			if(tokenType == NapileTokens.GTGTEQ)
				return ">>=";
			if(tokenType == NapileTokens.GTGTGTEQ)
				return ">>>=";
		}
		return super.getTokenText();
	}

	@Override
	public IElementType lookAhead(int steps)
	{
		if(!joinComplexTokens())
			return super.lookAhead(steps);

		Integer size = ADDITIONAL_SIZE.get(getTokenType());
		if(size != null)
			return super.lookAhead(steps + size);
		return getJoinedTokenType(super.lookAhead(steps), 2);
	}
}
