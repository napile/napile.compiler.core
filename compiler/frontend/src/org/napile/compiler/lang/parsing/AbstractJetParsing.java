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

import org.napile.compiler.lang.parsing.injection.CodeInjectionManager;
import org.napile.compiler.lexer.NapileKeywordToken;
import org.napile.compiler.lexer.NapileToken;
import org.napile.compiler.lexer.NapileTokens;
import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * @author abreslav
 */
/*package*/ abstract class AbstractJetParsing
{
	private static final Map<String, NapileKeywordToken> SOFT_KEYWORD_TEXTS = new HashMap<String, NapileKeywordToken>();

	static
	{
		for(IElementType type : NapileTokens.SOFT_KEYWORDS.getTypes())
		{
			NapileKeywordToken keywordToken = (NapileKeywordToken) type;
			assert keywordToken.isSoft();
			SOFT_KEYWORD_TEXTS.put(keywordToken.getValue(), keywordToken);
		}

		for(IElementType type : CodeInjectionManager.INSTANCE.getInjectionTokens().getTypes())
		{
			NapileKeywordToken keywordToken = (NapileKeywordToken) type;
			SOFT_KEYWORD_TEXTS.put(keywordToken.getValue(), keywordToken);
		}
	}

	static
	{
		for(IElementType token : NapileTokens.KEYWORDS.getTypes())
		{
			assert token instanceof NapileKeywordToken : "Must be NapileKeywordToken: " + token;
			assert !((NapileKeywordToken) token).isSoft() : "Must not be soft: " + token;
		}
	}

	private final SemanticWhitespaceAwarePsiBuilder myBuilder;

	public AbstractJetParsing(SemanticWhitespaceAwarePsiBuilder builder)
	{
		this.myBuilder = builder;
	}

	protected IElementType getLastToken()
	{
		int i = 1;
		int currentOffset = myBuilder.getCurrentOffset();
		while(i <= currentOffset && NapileTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(myBuilder.rawLookup(-i)))
		{
			i++;
		}
		return myBuilder.rawLookup(-i);
	}

	protected boolean expect(NapileToken expectation, String message)
	{
		return expect(expectation, message, null);
	}

	public PsiBuilder.Marker mark()
	{
		return myBuilder.mark();
	}

	protected void error(String message)
	{
		myBuilder.error(message);
	}

	protected boolean expect(NapileToken expectation, String message, TokenSet recoverySet)
	{
		if(at(expectation))
		{
			advance(); // expectation
			return true;
		}

		errorWithRecovery(message, recoverySet);

		return false;
	}

	protected boolean expectNoAdvance(NapileToken expectation, String message)
	{
		if(at(expectation))
		{
			advance(); // expectation
			return true;
		}

		error(message);

		return false;
	}

	protected void errorWithRecovery(String message, TokenSet recoverySet)
	{
		IElementType tt = tt();
		if(recoverySet == null || recoverySet.contains(tt) || (recoverySet.contains(NapileTokens.EOL_OR_SEMICOLON) && (eof() || tt == NapileTokens.SEMICOLON || myBuilder.newlineBeforeCurrentToken())))
		{
			error(message);
		}
		else
		{
			errorAndAdvance(message);
		}
	}

	protected boolean errorAndAdvance(String message)
	{
		PsiBuilder.Marker err = mark();
		advance(); // erroneous token
		err.error(message);
		return false;
	}

	protected boolean eof()
	{
		return myBuilder.eof();
	}

	protected void advance()
	{
		// TODO: how to report errors on bad characters? (Other than highlighting)
		myBuilder.advanceLexer();
	}

	protected void advanceAt(IElementType current)
	{
		assert _at(current);
		myBuilder.advanceLexer();
	}

	protected void advanceAtSet(IElementType... tokens)
	{
		assert _atSet(tokens);
		myBuilder.advanceLexer();
	}

	protected void advanceAtSet(final TokenSet set)
	{
		assert _atSet(set);
		myBuilder.advanceLexer();
	}

	protected IElementType tt()
	{
		return myBuilder.getTokenType();
	}

	/**
	 * Side-effect-free version of at()
	 */
	protected boolean _at(IElementType expectation)
	{
		IElementType token = tt();
		return tokenMatches(token, expectation);
	}

	private boolean tokenMatches(IElementType token, IElementType expectation)
	{
		if(token == expectation)
			return true;
		if(expectation == NapileTokens.EOL_OR_SEMICOLON)
		{
			if(eof())
				return true;
			if(token == NapileTokens.SEMICOLON)
				return true;
			if(myBuilder.newlineBeforeCurrentToken())
				return true;
		}
		return false;
	}

	protected boolean at(final IElementType expectation)
	{
		if(_at(expectation))
			return true;
		IElementType token = tt();
		if(token == NapileTokens.IDENTIFIER && expectation instanceof NapileKeywordToken)
		{
			NapileKeywordToken expectedKeyword = (NapileKeywordToken) expectation;
			if(expectedKeyword.isSoft() && expectedKeyword.getValue().equals(myBuilder.getTokenText()))
			{
				myBuilder.remapCurrentToken(expectation);
				return true;
			}
		}
		if(expectation == NapileTokens.IDENTIFIER && token instanceof NapileKeywordToken)
		{
			NapileKeywordToken keywordToken = (NapileKeywordToken) token;
			if(keywordToken.isSoft())
			{
				myBuilder.remapCurrentToken(NapileTokens.IDENTIFIER);
				return true;
			}
		}
		return false;
	}

	/**
	 * Side-effect-free version of atSet()
	 */
	protected boolean _atSet(IElementType... tokens)
	{
		return _atSet(TokenSet.create(tokens));
	}

	/**
	 * Side-effect-free version of atSet()
	 */
	protected boolean _atSet(final TokenSet set)
	{
		IElementType token = tt();
		if(set.contains(token))
			return true;
		if(set.contains(NapileTokens.EOL_OR_SEMICOLON))
		{
			if(eof())
				return true;
			if(token == NapileTokens.SEMICOLON)
				return true;
			if(myBuilder.newlineBeforeCurrentToken())
				return true;
		}
		return false;
	}

	protected boolean atSet(IElementType... tokens)
	{
		return atSet(TokenSet.create(tokens));
	}

	protected boolean atSet(final TokenSet set)
	{
		if(_atSet(set))
			return true;
		IElementType token = tt();
		if(token == NapileTokens.IDENTIFIER)
		{
			NapileKeywordToken keywordToken = SOFT_KEYWORD_TEXTS.get(myBuilder.getTokenText());
			if(keywordToken != null && set.contains(keywordToken))
			{
				myBuilder.remapCurrentToken(keywordToken);
				return true;
			}
		}
		else
		{
			// We know at this point that <code>set</code> does not contain <code>token</code>
			if(set.contains(NapileTokens.IDENTIFIER) && token instanceof NapileKeywordToken)
			{
				if(((NapileKeywordToken) token).isSoft())
				{
					myBuilder.remapCurrentToken(NapileTokens.IDENTIFIER);
					return true;
				}
			}
		}
		return false;
	}

	protected IElementType lookahead(int k)
	{
		return myBuilder.lookAhead(k);
	}

	protected void consumeIf(NapileToken token)
	{
		if(at(token))
			advance(); // token
	}

	// TODO: Migrate to predicates
	protected void skipUntil(TokenSet tokenSet)
	{
		boolean stopAtEolOrSemi = tokenSet.contains(NapileTokens.EOL_OR_SEMICOLON);
		while(!eof() && !tokenSet.contains(tt()) && !(stopAtEolOrSemi && at(NapileTokens.EOL_OR_SEMICOLON)))
		{
			advance();
		}
	}

	protected void errorUntil(String message, TokenSet tokenSet)
	{
		PsiBuilder.Marker error = mark();
		skipUntil(tokenSet);
		error.error(message);
	}

	protected void errorUntilOffset(String mesage, int offset)
	{
		PsiBuilder.Marker error = mark();
		while(!eof() && myBuilder.getCurrentOffset() < offset)
		{
			advance();
		}
		error.error(mesage);
	}

	protected int matchTokenStreamPredicate(TokenStreamPattern pattern)
	{
		PsiBuilder.Marker currentPosition = mark();
		Stack<IElementType> opens = new Stack<IElementType>();
		int openAngleBrackets = 0;
		int openBraces = 0;
		int openParentheses = 0;
		int openBrackets = 0;
		while(!eof())
		{
			if(pattern.processToken(myBuilder.getCurrentOffset(), pattern.isTopLevel(openAngleBrackets, openBrackets, openBraces, openParentheses)))
			{
				break;
			}
			if(at(NapileTokens.LPAR))
			{
				openParentheses++;
				opens.push(NapileTokens.LPAR);
			}
			else if(at(NapileTokens.LT))
			{
				openAngleBrackets++;
				opens.push(NapileTokens.LT);
			}
			else if(at(NapileTokens.LBRACE))
			{
				openBraces++;
				opens.push(NapileTokens.LBRACE);
			}
			else if(at(NapileTokens.LBRACKET))
			{
				openBrackets++;
				opens.push(NapileTokens.LBRACKET);
			}
			else if(at(NapileTokens.RPAR))
			{
				openParentheses--;
				if(opens.isEmpty() || opens.pop() != NapileTokens.LPAR)
				{
					if(pattern.handleUnmatchedClosing(NapileTokens.RPAR))
					{
						break;
					}
				}
			}
			else if(at(NapileTokens.GT))
			{
				openAngleBrackets--;
			}
			else if(at(NapileTokens.RBRACE))
			{
				openBraces--;
			}
			else if(at(NapileTokens.RBRACKET))
			{
				openBrackets--;
			}
			advance(); // skip token
		}
		currentPosition.rollbackTo();
		return pattern.result();
	}

	/*
		 * Looks for a the last top-level (not inside any {} [] () <>) '.' occurring before a
		 * top-level occurrence of a token from the <code>stopSet</code>
		 *
		 * Returns -1 if no occurrence is found
		 *
		 */
	protected int findLastBefore(TokenSet lookFor, TokenSet stopAt, boolean dontStopRightAfterOccurrence)
	{
		return matchTokenStreamPredicate(new LastBefore(new AtSet(lookFor), new AtSet(stopAt), dontStopRightAfterOccurrence));
	}

	protected int findLastBefore(IElementType lookFor, TokenSet stopAt, boolean dontStopRightAfterOccurrence)
	{
		return matchTokenStreamPredicate(new LastBefore(new At(lookFor), new AtSet(stopAt), dontStopRightAfterOccurrence));
	}

	protected boolean eol()
	{
		return myBuilder.newlineBeforeCurrentToken() || eof();
	}

	protected abstract JetParsing create(SemanticWhitespaceAwarePsiBuilder builder);

	protected JetParsing createTruncatedBuilder(int eofPosition)
	{
		return create(new TruncatedSemanticWhitespaceAwarePsiBuilder(myBuilder, eofPosition));
	}

	public SemanticWhitespaceAwarePsiBuilder getBuilder()
	{
		return myBuilder;
	}

	protected class AtOffset extends AbstractTokenStreamPredicate
	{

		private final int offset;

		public AtOffset(int offset)
		{
			this.offset = offset;
		}

		@Override
		public boolean matching(boolean topLevel)
		{
			return myBuilder.getCurrentOffset() == offset;
		}
	}

	protected class At extends AbstractTokenStreamPredicate
	{

		private final IElementType lookFor;
		private final boolean topLevelOnly;

		public At(IElementType lookFor, boolean topLevelOnly)
		{
			this.lookFor = lookFor;
			this.topLevelOnly = topLevelOnly;
		}

		public At(IElementType lookFor)
		{
			this(lookFor, true);
		}

		@Override
		public boolean matching(boolean topLevel)
		{
			return (topLevel || !topLevelOnly) && at(lookFor);
		}
	}

	protected class AtSet extends AbstractTokenStreamPredicate
	{
		private final TokenSet lookFor;
		private final TokenSet topLevelOnly;

		public AtSet(TokenSet lookFor, TokenSet topLevelOnly)
		{
			this.lookFor = lookFor;
			this.topLevelOnly = topLevelOnly;
		}

		public AtSet(TokenSet lookFor)
		{
			this(lookFor, lookFor);
		}

		public AtSet(IElementType... lookFor)
		{
			this(TokenSet.create(lookFor), TokenSet.create(lookFor));
		}

		@Override
		public boolean matching(boolean topLevel)
		{
			return (topLevel || !atSet(topLevelOnly)) && atSet(lookFor);
		}
	}

	protected class AtFirstTokenOfTokens extends AbstractTokenStreamPredicate
	{

		private final IElementType[] tokens;

		public AtFirstTokenOfTokens(IElementType... tokens)
		{
			assert tokens.length > 0;
			this.tokens = tokens;
		}

		@Override
		public boolean matching(boolean topLevel)
		{
			int length = tokens.length;
			if(!at(tokens[0]))
				return false;

			for(int i = 1; i < length; i++)
			{
				IElementType lookAhead = myBuilder.lookAhead(i);
				if(lookAhead == null || !tokenMatches(lookAhead, tokens[i]))
				{
					return false;
				}
			}

			return true;
		}
	}
}
