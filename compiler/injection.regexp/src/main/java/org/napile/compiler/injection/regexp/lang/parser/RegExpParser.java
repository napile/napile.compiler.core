/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.napile.compiler.injection.regexp.lang.parser;

import java.util.EnumSet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;

@SuppressWarnings({"RedundantIfStatement"})
public class RegExpParser implements PsiParser
{
	private final EnumSet<RegExpCapability> myCapabilities;

	public RegExpParser()
	{
		myCapabilities = EnumSet.noneOf(RegExpCapability.class);
	}

	public RegExpParser(EnumSet<RegExpCapability> capabilities)
	{
		myCapabilities = capabilities;
	}

	@NotNull
	public ASTNode parse(IElementType root, PsiBuilder builder)
	{
		//        builder.setDebugMode(true);
		final PsiBuilder.Marker rootMarker = builder.mark();

		parsePattern(builder);

		while(!builder.eof())
		{
			patternExpected(builder);
			builder.advanceLexer();
		}

		rootMarker.done(root);
		return builder.getTreeBuilt();
	}


	/**
	 * PATTERN ::= BRANCH "|" PATTERN | BRANCH
	 */
	private boolean parsePattern(PsiBuilder builder)
	{
		final PsiBuilder.Marker marker = builder.mark();

		if(!parseBranch(builder))
		{
			marker.drop();
			return false;
		}

		while(builder.getTokenType() == RegExpTokens.UNION)
		{
			builder.advanceLexer();
			if(!parseBranch(builder))
			{
				// TODO: no test coverage
				patternExpected(builder);
				break;
			}
		}

		marker.done(RegExpElementTypes.PATTERN);

		return true;
	}

	/**
	 * BRANCH  ::= ATOM BRANCH | ""
	 */
	@SuppressWarnings({"StatementWithEmptyBody"})
	private boolean parseBranch(PsiBuilder builder)
	{
		PsiBuilder.Marker marker = builder.mark();

		if(!parseAtom(builder))
		{
			final IElementType token = builder.getTokenType();
			if(token == RegExpTokens.GROUP_END || token == RegExpTokens.UNION || token == null)
			{
				// empty branches are allowed
				marker.done(RegExpElementTypes.BRANCH);
				return true;
			}
			marker.drop();
			return false;
		}

		for(; parseAtom(builder); )
			;

		marker.done(RegExpElementTypes.BRANCH);
		return true;
	}

	/**
	 * ATOM        ::= CLOSURE | GROUP
	 * CLOSURE     ::= GROUP QUANTIFIER
	 */
	private boolean parseAtom(PsiBuilder builder)
	{
		PsiBuilder.Marker marker = parseGroup(builder);

		if(marker == null)
		{
			return false;
		}
		marker = marker.precede();

		if(parseQuantifier(builder))
		{
			marker.done(RegExpElementTypes.CLOSURE);
		}
		else
		{
			marker.drop();
		}

		return true;
	}

	/**
	 * QUANTIFIER   ::= Q TYPE | ""
	 * Q            ::= "{" BOUND "}" | "*" | "?" | "+"
	 * BOUND        ::= NUM | NUM "," | NUM "," NUM
	 * TYPE         ::= "?" | "+" | ""
	 */
	private boolean parseQuantifier(PsiBuilder builder)
	{
		final PsiBuilder.Marker marker = builder.mark();

		if(builder.getTokenType() == RegExpTokens.LBRACE)
		{
			builder.advanceLexer();
			boolean minOmitted = false;
			if(builder.getTokenType() == RegExpTokens.COMMA && myCapabilities.contains(RegExpCapability.OMIT_NUMBERS_IN_QUANTIFIERS))
			{
				minOmitted = true;
				builder.advanceLexer();
			}
			else if(builder.getTokenType() != RegExpTokens.NUMBER && myCapabilities.contains(RegExpCapability.DANGLING_METACHARACTERS))
			{
				marker.done(RegExpTokens.CHARACTER);
				return true;
			}
			else
			{
				checkMatches(builder, RegExpTokens.NUMBER, "Number expected");
			}
			if(builder.getTokenType() == RegExpTokens.RBRACE)
			{
				builder.advanceLexer();
				parseQuantifierType(builder);
				marker.done(RegExpElementTypes.QUANTIFIER);
			}
			else
			{
				if(!minOmitted)
				{
					checkMatches(builder, RegExpTokens.COMMA, "',' expected");
				}
				if(builder.getTokenType() == RegExpTokens.RBRACE)
				{
					builder.advanceLexer();
					parseQuantifierType(builder);
					marker.done(RegExpElementTypes.QUANTIFIER);
				}
				else if(builder.getTokenType() == RegExpTokens.NUMBER)
				{
					builder.advanceLexer();
					checkMatches(builder, RegExpTokens.RBRACE, "'}' expected");
					parseQuantifierType(builder);
					marker.done(RegExpElementTypes.QUANTIFIER);
				}
				else
				{
					builder.error("'}' or number expected");
					marker.done(RegExpElementTypes.QUANTIFIER);
					return true;
				}
			}
		}
		else if(RegExpTokens.QUANTIFIERS.contains(builder.getTokenType()))
		{
			builder.advanceLexer();
			parseQuantifierType(builder);
			marker.done(RegExpElementTypes.QUANTIFIER);
		}
		else
		{
			marker.drop();
			return false;
		}

		return true;
	}

	private static void parseQuantifierType(PsiBuilder builder)
	{
		if(builder.getTokenType() == RegExpTokens.PLUS)
		{
			builder.advanceLexer();
		}
		else if(builder.getTokenType() == RegExpTokens.QUEST)
		{
			builder.advanceLexer();
		}
		else
		{
			if(RegExpTokens.QUANTIFIERS.contains(builder.getTokenType()))
			{
				builder.error("Dangling metacharacter");
			}
		}
	}

	/**
	 * CLASS            ::= "[" NEGATION DEFLIST "]"
	 * NEGATION         ::= "^" | ""
	 * DEFLIST          ::= INTERSECTION DEFLIST
	 * INTERSECTION     ::= INTERSECTION "&&" CLASSDEF | CLASSDEF
	 * CLASSDEF         ::= CLASS | SIMPLE_CLASSDEF | ""
	 * SIMPLE_CLASSDEF  ::= CHARACTER | CHARACTER "-" CLASSDEF
	 */
	private PsiBuilder.Marker parseClass(PsiBuilder builder)
	{
		final PsiBuilder.Marker marker = builder.mark();
		builder.advanceLexer();

		if(builder.getTokenType() == RegExpTokens.CARET)
		{
			builder.advanceLexer();
		}

		// DEFLIST
		if(parseClassIntersection(builder))
		{
			while(RegExpTokens.CHARACTERS2.contains(builder.getTokenType()) ||
					builder.getTokenType() == RegExpTokens.CLASS_BEGIN ||
					builder.getTokenType() == RegExpTokens.PROPERTY)
			{
				parseClassIntersection(builder);
			}
		}

		checkMatches(builder, RegExpTokens.CLASS_END, "Unclosed character class");
		marker.done(RegExpElementTypes.CLASS);
		return marker;
	}

	private boolean parseClassIntersection(PsiBuilder builder)
	{
		PsiBuilder.Marker marker = builder.mark();

		if(!parseClassdef(builder, false))
		{
			marker.drop();
			return false;
		}
		while(RegExpTokens.ANDAND == builder.getTokenType())
		{
			builder.advanceLexer();
			parseClassdef(builder, true);
			marker.done(RegExpElementTypes.INTERSECTION);
			marker = marker.precede();
		}

		marker.drop();
		return true;
	}

	private boolean parseClassdef(PsiBuilder builder, boolean mayBeEmpty)
	{
		final IElementType token = builder.getTokenType();
		if(token == RegExpTokens.CLASS_BEGIN)
		{
			parseClass(builder);
		}
		else if(RegExpTokens.CHARACTERS2.contains(token))
		{
			parseSimpleClassdef(builder);
		}
		else if(token == RegExpTokens.PROPERTY)
		{
			parseProperty(builder);
		}
		else if(mayBeEmpty)
		{
			// TODO: no test coverage
			return true;
		}
		else
		{
			return false;
		}
		return true;
	}

	private void parseSimpleClassdef(PsiBuilder builder)
	{
		assert RegExpTokens.CHARACTERS2.contains(builder.getTokenType());

		final PsiBuilder.Marker marker = builder.mark();
		makeChar(builder);

		IElementType t = builder.getTokenType();
		if(t == RegExpTokens.MINUS)
		{
			final PsiBuilder.Marker m = builder.mark();
			builder.advanceLexer();

			t = builder.getTokenType();
			if(RegExpTokens.CHARACTERS2.contains(t))
			{
				m.drop();
				makeChar(builder);
				marker.done(RegExpElementTypes.CHAR_RANGE);
			}
			else
			{
				marker.drop();
				m.done(t == RegExpTokens.CHAR_CLASS ? RegExpElementTypes.SIMPLE_CLASS : RegExpElementTypes.CHAR);

				if(t == RegExpTokens.CLASS_END)
				{ // [a-]
					return;
				}
				else if(t == RegExpTokens.CLASS_BEGIN)
				{ // [a-[b]]
					if(parseClassdef(builder, false))
					{
						return;
					}
				}
				builder.error("Illegal character range");
			}
		}
		else
		{
			marker.drop();
		}
	}

	private static void makeChar(PsiBuilder builder)
	{
		final IElementType t = builder.getTokenType();
		PsiBuilder.Marker m = builder.mark();
		builder.advanceLexer();
		m.done(t == RegExpTokens.CHAR_CLASS ? RegExpElementTypes.SIMPLE_CLASS : RegExpElementTypes.CHAR);
	}

	/**
	 * GROUP  ::= "(" PATTERN ")" | TERM
	 * TERM   ::= "." | "$" | "^" | CHAR | CLASS | BACKREF
	 */
	@Nullable
	private PsiBuilder.Marker parseGroup(PsiBuilder builder)
	{
		final IElementType type = builder.getTokenType();

		final PsiBuilder.Marker marker = builder.mark();

		if(RegExpTokens.GROUPS.contains(type))
		{
			builder.advanceLexer();
			if(!parsePattern(builder))
			{
				patternExpected(builder);
			}
			else
			{
				checkMatches(builder, RegExpTokens.GROUP_END, "Unclosed group");
			}
			marker.done(RegExpElementTypes.GROUP);
		}
		else if(type == RegExpTokens.SET_OPTIONS)
		{
			builder.advanceLexer();

			final PsiBuilder.Marker o = builder.mark();
			if(builder.getTokenType() == RegExpTokens.OPTIONS_ON)
			{
				builder.advanceLexer();
			}
			if(builder.getTokenType() == RegExpTokens.OPTIONS_OFF)
			{
				builder.advanceLexer();
			}
			o.done(RegExpElementTypes.OPTIONS);

			if(builder.getTokenType() == RegExpTokens.COLON)
			{
				builder.advanceLexer();
				if(!parsePattern(builder))
				{
					// TODO: no test coverage
					patternExpected(builder);
				}
				else
				{
					checkMatches(builder, RegExpTokens.GROUP_END, "Unclosed group");
				}
				marker.done(RegExpElementTypes.GROUP);
			}
			else
			{
				checkMatches(builder, RegExpTokens.GROUP_END, "Unclosed options group");
				marker.done(RegExpElementTypes.SET_OPTIONS);
			}
		}
		else if(type == StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN)
		{
			builder.error("Illegal/unsupported escape sequence");
			builder.advanceLexer();
			marker.done(RegExpElementTypes.CHAR);
		}
		else if(RegExpTokens.CHARACTERS.contains(type))
		{
			builder.advanceLexer();
			marker.done(RegExpElementTypes.CHAR);
		}
		else if(RegExpTokens.BOUNDARIES.contains(type))
		{
			builder.advanceLexer();
			marker.done(RegExpElementTypes.BOUNDARY);
		}
		else if(type == RegExpTokens.BACKREF)
		{
			builder.advanceLexer();
			marker.done(RegExpElementTypes.BACKREF);
		}
		else if(type == RegExpTokens.PYTHON_NAMED_GROUP || type == RegExpTokens.RUBY_NAMED_GROUP || type == RegExpTokens.RUBY_QUOTED_NAMED_GROUP)
		{
			builder.advanceLexer();
			checkMatches(builder, RegExpTokens.NAME, "Group name expected");
			checkMatches(builder, type == RegExpTokens.RUBY_QUOTED_NAMED_GROUP ? RegExpTokens.QUOTE : RegExpTokens.GT, "Unclosed group name");
			if(!parsePattern(builder))
			{
				patternExpected(builder);
			}
			else
			{
				checkMatches(builder, RegExpTokens.GROUP_END, "Unclosed group");
			}
			marker.done(RegExpElementTypes.GROUP);
		}
		else if(type == RegExpTokens.PYTHON_NAMED_GROUP_REF)
		{
			builder.advanceLexer();
			checkMatches(builder, RegExpTokens.NAME, "Group name expected");
			checkMatches(builder, RegExpTokens.GROUP_END, "Unclosed group reference");
			marker.done(RegExpElementTypes.PY_NAMED_GROUP_REF);
		}
		else if(type == RegExpTokens.PYTHON_COND_REF)
		{
			builder.advanceLexer();
			if(builder.getTokenType() == RegExpTokens.NAME || builder.getTokenType() == RegExpTokens.NUMBER)
			{
				builder.advanceLexer();
			}
			else
			{
				builder.error("Group name or number expected");
			}
			checkMatches(builder, RegExpTokens.GROUP_END, "Unclosed group reference");
			if(!parseBranch(builder))
			{
				patternExpected(builder);
			}
			else
			{
				if(builder.getTokenType() == RegExpTokens.UNION)
				{
					builder.advanceLexer();
					if(!parseBranch(builder))
					{
						patternExpected(builder);
					}
				}
				checkMatches(builder, RegExpTokens.GROUP_END, "Unclosed group");
			}
			marker.done(RegExpElementTypes.PY_COND_REF);
		}
		else if(type == RegExpTokens.PROPERTY)
		{
			parseProperty(builder);
			marker.done(RegExpElementTypes.PROPERTY);
		}
		else if(RegExpTokens.SIMPLE_CLASSES.contains(type))
		{
			builder.advanceLexer();
			marker.done(RegExpElementTypes.SIMPLE_CLASS);
		}
		else if(type == RegExpTokens.CLASS_BEGIN)
		{
			marker.drop();
			return parseClass(builder);
		}
		else if(type == RegExpTokens.LBRACE && myCapabilities.contains(RegExpCapability.DANGLING_METACHARACTERS))
		{
			builder.advanceLexer();
			marker.done(RegExpElementTypes.CHAR);
		}
		else
		{
			marker.drop();
			return null;
		}
		return marker;
	}

	private static void parseProperty(PsiBuilder builder)
	{
		checkMatches(builder, RegExpTokens.PROPERTY, "'\\p' expected");

		checkMatches(builder, RegExpTokens.LBRACE, "Character category expected");
		if(builder.getTokenType() == RegExpTokens.NAME)
		{
			builder.advanceLexer();
		}
		else if(builder.getTokenType() == RegExpTokens.RBRACE)
		{
			builder.error("Empty character family");
		}
		else
		{
			builder.error("Character family name expected");
			builder.advanceLexer();
		}
		checkMatches(builder, RegExpTokens.RBRACE, "Unclosed character family");
	}

	private static void patternExpected(PsiBuilder builder)
	{
		final IElementType token = builder.getTokenType();
		if(token == RegExpTokens.GROUP_END)
		{
			builder.error("Unmatched closing ')'");
		}
		else if(RegExpTokens.QUANTIFIERS.contains(token))
		{
			builder.error("Dangling metacharacter");
		}
		else
		{
			builder.error("Pattern expected");
		}
	}

	protected static void checkMatches(final PsiBuilder builder, final IElementType token, final String message)
	{
		if(builder.getTokenType() == token)
		{
			builder.advanceLexer();
		}
		else
		{
			builder.error(message);
		}
	}
}
