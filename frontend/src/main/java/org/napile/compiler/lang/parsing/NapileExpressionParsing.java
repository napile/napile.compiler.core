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

import static org.napile.compiler.lang.lexer.NapileNodes.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.lexer.NapileNode;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.lexer.NapileToken;
import org.napile.compiler.lang.lexer.NapileTokens;
import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * @author abreslav
 */
public class NapileExpressionParsing extends AbstractNapileParsing
{
	private static final TokenSet WHEN_CONDITION_RECOVERY_SET = TokenSet.create(NapileTokens.RBRACE, NapileTokens.IN_KEYWORD, NapileTokens.NOT_IN, NapileTokens.IS_KEYWORD, NapileTokens.NOT_IS, NapileTokens.ELSE_KEYWORD);
	private static final TokenSet WHEN_CONDITION_RECOVERY_SET_WITH_ARROW = TokenSet.create(NapileTokens.RBRACE, NapileTokens.IN_KEYWORD, NapileTokens.NOT_IN, NapileTokens.IS_KEYWORD, NapileTokens.NOT_IS, NapileTokens.ELSE_KEYWORD, NapileTokens.ARROW, NapileTokens.DOT);

	private static final TokenSet TYPE_ARGUMENT_LIST_STOPPERS = TokenSet.create
	(
			NapileTokens.INTEGER_LITERAL, NapileTokens.FLOAT_LITERAL, NapileTokens.STRING_LITERAL, NapileTokens.PACKAGE_KEYWORD, NapileTokens.AS_KEYWORD, NapileTokens.CLASS_KEYWORD, NapileTokens.THIS_KEYWORD, NapileTokens.VAR_KEYWORD, NapileTokens.VAL_KEYWORD, NapileTokens.METH_KEYWORD,
			NapileTokens.FOR_KEYWORD, NapileTokens.NULL_KEYWORD, NapileTokens.TRUE_KEYWORD, NapileTokens.FALSE_KEYWORD, NapileTokens.IS_KEYWORD, NapileTokens.THROW_KEYWORD, NapileTokens.RETURN_KEYWORD, NapileTokens.BREAK_KEYWORD, NapileTokens.CONTINUE_KEYWORD,
			NapileTokens.ANONYM_KEYWORD, NapileTokens.IF_KEYWORD, NapileTokens.TRY_KEYWORD, NapileTokens.ELSE_KEYWORD, NapileTokens.WHILE_KEYWORD, NapileTokens.DO_KEYWORD, NapileTokens.WHEN_KEYWORD, NapileTokens.RBRACKET, NapileTokens.RBRACE, NapileTokens.RPAR, NapileTokens.PLUSPLUS, NapileTokens.MINUSMINUS, NapileTokens.EXCLEXCL,

			NapileTokens.PLUS, NapileTokens.MINUS, NapileTokens.EXCL, NapileTokens.DIV, NapileTokens.PERC, NapileTokens.LTEQ, NapileTokens.TILDE, NapileTokens.XOR, NapileTokens.OR, NapileTokens.AND,
			// TODO GTEQ,   foo<bar, baz>=x
			NapileTokens.LTLT, NapileTokens.GTGT, NapileTokens.GTGTGT, NapileTokens.LTLTEQ, NapileTokens.GTGTEQ, NapileTokens.GTGTGTEQ,
			NapileTokens.EQEQ, NapileTokens.EXCLEQ, NapileTokens.ANDAND, NapileTokens.OROR, NapileTokens.SAFE_ACCESS, NapileTokens.ELVIS, NapileTokens.SEMICOLON, NapileTokens.RANGE, NapileTokens.EQ, NapileTokens.MULTEQ, NapileTokens.DIVEQ, NapileTokens.PERCEQ, NapileTokens.PLUSEQ, NapileTokens.MINUSEQ,
			NapileTokens.ANDEQ, NapileTokens.OREQ, NapileTokens.XOREQ, NapileTokens.NOT_IN, NapileTokens.NOT_IS,
			NapileTokens.COLON
	);

	static final TokenSet EXPRESSION_FIRST = TokenSet.create
	(
			// Prefix
			NapileTokens.MINUS, NapileTokens.PLUS, NapileTokens.MINUSMINUS, NapileTokens.PLUSPLUS, NapileTokens.EXCL, NapileTokens.TILDE, NapileTokens.EXCLEXCL, // Joining complex tokens makes it necessary to put EXCLEXCL here
			NapileTokens.LBRACE,
			NapileTokens.LBRACKET,

			NapileTokens.LPAR, // parenthesized
			NapileTokens.HASH, // method targeting
			NapileTokens.DOUBLE_ARROW, // expression targeting

			// literal constant
			NapileTokens.TRUE_KEYWORD, NapileTokens.FALSE_KEYWORD, NapileTokens.INTEGER_LITERAL, NapileTokens.STRING_LITERAL, NapileTokens.FLOAT_LITERAL, NapileTokens.NULL_KEYWORD,

			NapileTokens.LBRACE, // functionLiteral

			NapileTokens.INJECTION_START, // injection

			NapileTokens.THIS_KEYWORD, // this
			NapileTokens.SUPER_KEYWORD, // super

			NapileTokens.IF_KEYWORD, // if
			NapileTokens.WHEN_KEYWORD, // when
			NapileTokens.TRY_KEYWORD, // try
			NapileTokens.ANONYM_KEYWORD, // object
			NapileTokens.LABEL_KEYWORD, // label

			// jump
			NapileTokens.THROW_KEYWORD, NapileTokens.RETURN_KEYWORD, NapileTokens.CONTINUE_KEYWORD, NapileTokens.BREAK_KEYWORD,

			// loop
			NapileTokens.FOR_KEYWORD, NapileTokens.WHILE_KEYWORD, NapileTokens.DO_KEYWORD,

			NapileTokens.IDENTIFIER, // SimpleName

			NapileTokens.IDE_TEMPLATE_START
	);

	private static final TokenSet STATEMENT_FIRST = TokenSet.orSet(EXPRESSION_FIRST, TokenSet.create(
			// declaration
			NapileTokens.METH_KEYWORD, NapileTokens.VAL_KEYWORD, NapileTokens.VAR_KEYWORD,  NapileTokens.CLASS_KEYWORD), NapileTokens.MODIFIER_KEYWORDS);

	/*package*/ static final TokenSet EXPRESSION_FOLLOW = TokenSet.create(NapileTokens.SEMICOLON, NapileTokens.ARROW, NapileTokens.COMMA, NapileTokens.RBRACE, NapileTokens.RPAR, NapileTokens.RBRACKET, NapileTokens.IDE_TEMPLATE_END);

	@SuppressWarnings({"UnusedDeclaration"})
	private enum Precedence
	{
		POSTFIX(NapileTokens.PLUSPLUS, NapileTokens.MINUSMINUS, NapileTokens.EXCLEXCL, NapileTokens.DOT, NapileTokens.SAFE_ACCESS),

		// + - ++ -- !
		PREFIX(NapileTokens.MINUS, NapileTokens.PLUS, NapileTokens.MINUSMINUS, NapileTokens.PLUSPLUS, NapileTokens.EXCL, NapileTokens.TILDE)
				{

					@Override
					public void parseHigherPrecedence(NapileExpressionParsing parser)
					{
						throw new IllegalStateException("Don't call this method");
					}
				},

		COLON_AS(NapileTokens.COLON, NapileTokens.AS_KEYWORD, NapileTokens.AS_SAFE)
				{
					@Override
					public NapileNode parseRightHandSide(IElementType operation, NapileExpressionParsing parser)
					{
						parser.parsing.parseTypeRef();
						return BINARY_WITH_TYPE;
					}

					@Override
					public void parseHigherPrecedence(NapileExpressionParsing parser)
					{
						parser.parsePrefixExpression();
					}
				},

		MULTIPLICATIVE(NapileTokens.MUL, NapileTokens.DIV, NapileTokens.PERC, NapileTokens.XOR, NapileTokens.OR, NapileTokens.AND, NapileTokens.LTLT, NapileTokens.GTGT, NapileTokens.GTGTGT),
		ADDITIVE(NapileTokens.PLUS, NapileTokens.MINUS),
		RANGE(NapileTokens.RANGE),
		SIMPLE_NAME(NapileTokens.IDENTIFIER),
		ELVIS(NapileTokens.ELVIS),
		IN_OR_IS(NapileTokens.IN_KEYWORD, NapileTokens.NOT_IN, NapileTokens.IS_KEYWORD, NapileTokens.NOT_IS)
				{
					@Override
					public NapileNode parseRightHandSide(IElementType operation, NapileExpressionParsing parser)
					{
						if(operation == NapileTokens.IS_KEYWORD || operation == NapileTokens.NOT_IS)
						{
							parser.parsing.parseTypeRef();

							return IS_EXPRESSION;
						}

						return super.parseRightHandSide(operation, parser);
					}
				},
		COMPARISON(NapileTokens.LT, NapileTokens.GT, NapileTokens.LTEQ, NapileTokens.GTEQ),
		EQUALITY(NapileTokens.EQEQ, NapileTokens.EXCLEQ),
		CONJUNCTION(NapileTokens.ANDAND),
		DISJUNCTION(NapileTokens.OROR),
		//        ARROW(NapileTokens.ARROW),
		ASSIGNMENT(NapileTokens.EQ, NapileTokens.PLUSEQ, NapileTokens.MINUSEQ, NapileTokens.MULTEQ, NapileTokens.DIVEQ, NapileTokens.PERCEQ, NapileTokens.ANDEQ, NapileTokens.OREQ, NapileTokens.XOREQ, NapileTokens.LTLTEQ, NapileTokens.GTGTEQ, NapileTokens.GTGTGTEQ);

		static
		{
			Precedence[] values = Precedence.values();
			for(Precedence precedence : values)
			{
				int ordinal = precedence.ordinal();
				precedence.higher = ordinal > 0 ? values[ordinal - 1] : null;
			}
		}

		private Precedence higher;
		private final TokenSet operations;

		Precedence(IElementType... operations)
		{
			this.operations = TokenSet.create(operations);
		}

		public void parseHigherPrecedence(NapileExpressionParsing parser)
		{
			assert higher != null;
			parser.parseBinaryExpression(higher);
		}

		/**
		 * @param operation the operation sign (e.g. PLUS or IS)
		 * @param parser    the parser object
		 * @return node type of the result
		 */
		public NapileNode parseRightHandSide(IElementType operation, NapileExpressionParsing parser)
		{
			parseHigherPrecedence(parser);
			return BINARY_EXPRESSION;
		}

		public final TokenSet getOperations()
		{
			return operations;
		}
	}

	public static final TokenSet ALLOW_NEWLINE_OPERATIONS = TokenSet.create(NapileTokens.DOT, NapileTokens.SAFE_ACCESS);

	public static final TokenSet ALL_OPERATIONS;

	static
	{
		Set<IElementType> operations = new HashSet<IElementType>();
		Precedence[] values = Precedence.values();
		for(Precedence precedence : values)
		{
			operations.addAll(Arrays.asList(precedence.getOperations().getTypes()));
		}
		ALL_OPERATIONS = TokenSet.create(operations.toArray(new IElementType[operations.size()]));
	}

	static
	{
		IElementType[] operations = NapileTokens.OPERATIONS.getTypes();
		Set<IElementType> opSet = new HashSet<IElementType>(Arrays.asList(operations));
		IElementType[] usedOperations = ALL_OPERATIONS.getTypes();
		Set<IElementType> usedSet = new HashSet<IElementType>(Arrays.asList(usedOperations));

		if(opSet.size() > usedSet.size())
		{
			opSet.removeAll(usedSet);
			assert false : opSet;
		}
		assert usedSet.size() == opSet.size() : "Either some ops are unused, or something a non-op is used";

		usedSet.removeAll(opSet);

		assert usedSet.isEmpty() : usedSet.toString();
	}


	private final NapileParsing parsing;
	private final CodeInjectionParser codeInjectionParser;

	public NapileExpressionParsing(SemanticWhitespaceAwarePsiBuilder builder, NapileParsing jetParsing)
	{
		super(builder);
		parsing = jetParsing;
		codeInjectionParser = new CodeInjectionParser(this);
	}

	/*
		 * element
		 *   : attributes element
		 *   : "(" element ")" // see tupleLiteral
		 *   : literalConstant
		 *   : functionLiteral
		 *   : tupleLiteral
		 *   : "null"
		 *   : "this" ("<" type ">")?
		 *   : expressionWithPrecedences
		 *   : if
		 *   : try
		 *   : "typeof" "(" element ")"
		 *   : "new" constructorInvocation
		 *   : objectLiteral
		 *   : declaration
		 *   : jump
		 *   : loop
		 *   // block is syntactically equivalent to a functionLiteral with no parameters
		 *   ;
		 */
	public void parseExpression()
	{
		if(!atSet(EXPRESSION_FIRST))
		{
			error("Expecting an expression");
			return;
		}
		parseBinaryExpression(Precedence.ASSIGNMENT);
	}

	/*
		 * element (operation element)*
		 *
		 * see the precedence table
		 */
	private void parseBinaryExpression(Precedence precedence)
	{
		//        System.out.println(precedence.name() + " at " + myBuilder.getTokenText());

		PsiBuilder.Marker expression = mark();

		precedence.parseHigherPrecedence(this);

		while(!interruptedWithNewLine() && atSet(precedence.getOperations()))
		{
			IElementType operation = tt();

			parseOperationReference();

			NapileNode resultType = precedence.parseRightHandSide(operation, this);
			expression.done(resultType);
			expression = expression.precede();
		}

		expression.drop();
	}

	/*
		 * operation? prefixExpression
		 */
	private void parsePrefixExpression()
	{
		//        System.out.println("pre at "  + myBuilder.getTokenText());

		getBuilder().disableJoiningComplexTokens();
		if(atSet(Precedence.PREFIX.getOperations()))
		{
			PsiBuilder.Marker expression = mark();

			parseOperationReference();

			getBuilder().restoreJoiningComplexTokensState();

			parsePrefixExpression();
			expression.done(PREFIX_EXPRESSION);
		}
		else
		{
			getBuilder().restoreJoiningComplexTokensState();
			parsePostfixExpression();
		}
	}

	/*
		 * atomicExpression postfixUnaryOperation?
		 *
		 * postfixUnaryOperation
		 *   : "++" : "--" : "!!"
		 *   : typeArguments? valueArguments (getEntryPoint? functionLiteral)
		 *   : typeArguments (getEntryPoint? functionLiteral)
		 *   : arrayAccess
		 *   : memberAccessOperation postfixUnaryExpression // TODO: Review
		 *   ;
		 */
	private void parsePostfixExpression()
	{
		//        System.out.println("post at "  + myBuilder.getTokenText());

		PsiBuilder.Marker expressionMarker = mark();
		parseAtomicExpression();
		while(true)
		{
			if(interruptedWithNewLine())
			{
				break;
			}
			else if(at(NapileTokens.LBRACKET))
			{
				parseArrayAccess();
				expressionMarker.done(ARRAY_ACCESS_EXPRESSION);
			}
			else if(parseCallSuffix())
			{
				expressionMarker.done(CALL_EXPRESSION);
			}
			else if(at(NapileTokens.DOT))
			{
				advance(); // DOT

				parseCallExpression();

				expressionMarker.done(DOT_QUALIFIED_EXPRESSION);
			}
			else if(at(NapileTokens.HASH))
			{
				parseLinkMethodExpression(expressionMarker);
			}
			else if(at(NapileTokens.DOUBLE_ARROW))
			{
				parseDoubleArrowExpression();
			}
			else if(at(NapileTokens.SAFE_ACCESS))
			{
				advance(); // SAFE_ACCESS

				parseCallExpression();

				expressionMarker.done(SAFE_ACCESS_EXPRESSION);
			}
			else if(atSet(Precedence.POSTFIX.getOperations()))
			{
				parseOperationReference();
				expressionMarker.done(POSTFIX_EXPRESSION);
			}
			else
			{
				break;
			}
			expressionMarker = expressionMarker.precede();
		}
		expressionMarker.drop();
	}

	private void parseDoubleArrowExpression()
	{
		PsiBuilder.Marker marker = mark();

		PsiBuilder.Marker temp = mark();
		advance();
		temp.done(REFERENCE_EXPRESSION);

		parseExpression();

		marker.done(DOUBLE_ARROW_EXPRESSION);
	}

	/*
		 * callSuffix
		 *   : typeArguments? valueArguments (getEntryPoint? functionLiteral*)
		 *   : typeArguments (getEntryPoint? functionLiteral*)
		 *   ;
		 */
	private boolean parseCallSuffix()
	{
		if(parseCallWithClosure())
		{
			parseCallWithClosure();
		}
		else if(at(NapileTokens.LPAR))
		{
			parseValueArgumentList();
			parseCallWithClosure();
		}
		else if(at(NapileTokens.LT))
		{
			PsiBuilder.Marker typeArgumentList = mark();
			if(parsing.tryParseTypeArgumentList(TYPE_ARGUMENT_LIST_STOPPERS))
			{
				typeArgumentList.done(TYPE_ARGUMENT_LIST);
				if(!getBuilder().newlineBeforeCurrentToken() && at(NapileTokens.LPAR))
					parseValueArgumentList();
				parseCallWithClosure();
			}
			else
			{
				typeArgumentList.rollbackTo();
				return false;
			}
		}
		else
		{
			return false;
		}

		return true;
	}

	/*
		 * atomicExpression typeParameters? valueParameters? functionLiteral*
		 */
	private void parseCallExpression()
	{
		PsiBuilder.Marker mark = mark();
		parseAtomicExpression();
		if(!getBuilder().newlineBeforeCurrentToken() && parseCallSuffix())
		{
			mark.done(CALL_EXPRESSION);
		}
		else
		{
			mark.drop();
		}
	}

	private void parseOperationReference()
	{
		PsiBuilder.Marker operationReference = mark();
		advance(); // operation
		operationReference.done(OPERATION_REFERENCE);
	}

	/*
		 * element (getEntryPoint? functionLiteral)?
		 */
	protected boolean parseCallWithClosure()
	{
		boolean success = false;
		//        while (!myBuilder.newlineBeforeCurrentToken()
		//                && (at(LBRACE)
		while(at(NapileTokens.LBRACE))
		{
			parseFunctionLiteral();

			success = true;
		}
		return success;
	}

	/*
		 * atomicExpression
		 *   : "this" label?
		 *   : "super" ("<" type ">")? label?
		 *   : objectLiteral
		 *   : jump
		 *   : if
		 *   : when
		 *   : try
		 *   : loop
		 *   : literalConstant
		 *   : functionLiteral
		 *   : declaration
		 *   : SimpleName
		 *   ;
		 */
	private void parseAtomicExpression()
	{
		//System.out.println("atom at "  + tt());

		if(at(NapileTokens.INJECTION_START))
		{
			codeInjectionParser.parse();
		}
		else if(at(NapileTokens.LPAR))
		{
			parseParenthesizedExpression();
		}
		else if(at(NapileTokens.IDE_TEMPLATE_START))
		{
			parsing.parseIdeTemplate();
		}
		else if(at(NapileTokens.THIS_KEYWORD))
		{
			parseThisExpression();
		}
		else if(at(NapileTokens.SUPER_KEYWORD))
		{
			parseSuperExpression();
		}
		else if(at(NapileTokens.ANONYM_KEYWORD))
		{
			parseObjectLiteral();
		}
		else if(at(NapileTokens.THROW_KEYWORD))
		{
			parseThrow();
		}
		else if(at(NapileTokens.RETURN_KEYWORD))
		{
			parseReturn();
		}
		else if(at(NapileTokens.CONTINUE_KEYWORD))
		{
			parseJump(CONTINUE);
		}
		else if(at(NapileTokens.BREAK_KEYWORD))
		{
			parseJump(BREAK);
		}
		else if(at(NapileTokens.IF_KEYWORD))
		{
			parseIf();
		}
		else if(at(NapileTokens.WHEN_KEYWORD))
		{
			parseWhen();
		}
		else if(at(NapileTokens.TRY_KEYWORD))
		{
			parseTry();
		}
		else if(at(NapileTokens.FOR_KEYWORD))
		{
			parseFor();
		}
		else if(at(NapileTokens.LABEL_KEYWORD))
		{
			parseLabel0();
		}
		else if(at(NapileTokens.WHILE_KEYWORD))
		{
			parseWhile();
		}
		else if(at(NapileTokens.DO_KEYWORD))
		{
			parseDoWhile();
		}
		else if(at(NapileTokens.CLASS_OF_KEYWORD))
			parseClassOrTypeOf(CLASS_OF);
		else if(at(NapileTokens.TYPE_OF_KEYWORD))
			parseClassOrTypeOf(TYPE_OF);
		else if(at(NapileTokens.ARRAY_OF_KEYWORD))
			parseArrayExpression();
		else if(at(NapileTokens.HASH))
			parseLinkMethodExpression(null);
		else if(at(NapileTokens.DOUBLE_ARROW))
		{
			parseDoubleArrowExpression();
		}
		else if(at(NapileTokens.IDENTIFIER))
			parseOneTokenExpression(REFERENCE_EXPRESSION);
		else if(at(NapileTokens.LBRACE))
			parseFunctionLiteral();
		else if(at(NapileTokens.LBRACKET))
			parseMultiTypeExpression();
		else if(!parseLiteralConstant())
		{
			// TODO: better recovery if FIRST(element) did not match
			errorWithRecovery("Expecting an element", EXPRESSION_FOLLOW);
		}
	}

	private void parseClassOrTypeOf(IElementType done)
	{
		PsiBuilder.Marker marker = mark();

		advance();

		if(expect(NapileTokens.LPAR, "'(' expected"))
		{
			parsing.parseTypeRef(NapileParsing.TYPE_PARAMETER_GT_RECOVERY_SET);

			expect(NapileTokens.RPAR, "') expected");
		}

		marker.done(done);
	}

	/*
		 * literalConstant
		 *   : "true" | "false"
		 *   : StringWithTemplates
		 *   : NoEscapeString
		 *   : IntegerLiteral
		 *   : LongLiteral
		 *   : CharacterLiteral
		 *   : FloatLiteral
		 *   : "null"
		 *   ;
		 */
	private boolean parseLiteralConstant()
	{
		if(at(NapileTokens.TRUE_KEYWORD) || at(NapileTokens.FALSE_KEYWORD))
			parseOneTokenExpression(BOOLEAN_CONSTANT);
		else if(at(NapileTokens.INTEGER_LITERAL))
			parseOneTokenExpression(INTEGER_CONSTANT);
		else if(at(NapileTokens.FLOAT_LITERAL))
			parseOneTokenExpression(FLOAT_CONSTANT);
		else if(at(NapileTokens.STRING_LITERAL))
			parseOneTokenExpression(STRING_CONSTANT);
		else if(at(NapileTokens.NULL_KEYWORD))
			parseOneTokenExpression(NULL);
		else
			return false;
		return true;
	}

	/*
		 * when
		 *   : "when" ("(" (modifiers "val" SimpleName "=")? element ")")? "{"
		 *         whenEntry*
		 *     "}"
		 *   ;
		 */
	private void parseWhen()
	{
		assert _at(NapileTokens.WHEN_KEYWORD);

		PsiBuilder.Marker when = mark();

		advance(); // WHEN_KEYWORD

		// Parse condition
		getBuilder().disableNewlines();
		if(at(NapileTokens.LPAR))
		{
			advanceAt(NapileTokens.LPAR);

			int valPos = matchTokenStreamPredicate(new FirstBefore(new At(NapileTokens.VAR_KEYWORD), new AtSet(NapileTokens.RPAR, NapileTokens.LBRACE, NapileTokens.RBRACE, NapileTokens.SEMICOLON, NapileTokens.EQ)));
			if(valPos >= 0)
			{
				PsiBuilder.Marker property = mark();
				parsing.parseModifierList(false);
				parsing.parseVariableOrValue(false);
				property.done(VARIABLE);
			}
			else
			{
				parseExpression();
			}

			expect(NapileTokens.RPAR, "Expecting ')'");
		}
		getBuilder().restoreNewlinesState();

		// Parse when block
		getBuilder().enableNewlines();
		expect(NapileTokens.LBRACE, "Expecting '{'");

		while(!eof() && !at(NapileTokens.RBRACE))
		{
			parseWhenEntry();
		}

		expect(NapileTokens.RBRACE, "Expecting '}'");
		getBuilder().restoreNewlinesState();

		when.done(WHEN);
	}

	/*
		 * whenEntry
		 *   // TODO : consider empty after ->
		 *   : whenCondition{","} "->" element SEMI
		 *   : "else" "->" element SEMI
		 *   ;
		 */
	private void parseWhenEntry()
	{
		PsiBuilder.Marker entry = mark();

		if(at(NapileTokens.ELSE_KEYWORD))
		{
			advance(); // ELSE_KEYWORD

			if(!at(NapileTokens.ARROW))
			{
				errorUntil("Expecting '->'", TokenSet.create(NapileTokens.ARROW, NapileTokens.RBRACE, NapileTokens.EOL_OR_SEMICOLON));
			}

			if(at(NapileTokens.ARROW))
			{
				advance(); // ARROW

				if(atSet(WHEN_CONDITION_RECOVERY_SET))
				{
					error("Expecting an element");
				}
				else
				{
					parseExpressionPreferringBlocks();
				}
			}
			else if(!atSet(WHEN_CONDITION_RECOVERY_SET))
			{
				errorAndAdvance("Expecting '->'");
			}
		}
		else
		{
			parseWhenEntryNotElse();
		}

		entry.done(WHEN_ENTRY);
		consumeIf(NapileTokens.SEMICOLON);
	}

	/*
		 * : whenCondition{","} "->" element SEMI
		 */
	private void parseWhenEntryNotElse()
	{
		if(!parsing.parseIdeTemplate())
		{
			while(true)
			{
				while(at(NapileTokens.COMMA))
					errorAndAdvance("Expecting a when-condition");
				parseWhenCondition();
				if(!at(NapileTokens.COMMA))
					break;
				advance(); // COMMA
			}
		}
		expect(NapileTokens.ARROW, "Expecting '->' or 'when'", WHEN_CONDITION_RECOVERY_SET);
		if(atSet(WHEN_CONDITION_RECOVERY_SET))
		{
			error("Expecting an element");
		}
		else
		{
			parseExpressionPreferringBlocks();
		}
		// SEMI is consumed in parseWhenEntry
	}

	/*
		 * whenCondition
		 *   : expression
		 *   : ("in" | "!in") expression
		 *   : ("is" | "!is") isRHS
		 *   ;
		 */
	private void parseWhenCondition()
	{
		PsiBuilder.Marker condition = mark();
		getBuilder().disableNewlines();
		if(at(NapileTokens.IN_KEYWORD) || at(NapileTokens.NOT_IN))
		{
			PsiBuilder.Marker mark = mark();
			advance(); // IN_KEYWORD or NOT_IN
			mark.done(OPERATION_REFERENCE);


			if(atSet(WHEN_CONDITION_RECOVERY_SET_WITH_ARROW))
			{
				error("Expecting an element");
			}
			else
			{
				parseExpression();
			}
			condition.done(WHEN_CONDITION_IN_RANGE);
		}
		else if(at(NapileTokens.IS_KEYWORD) || at(NapileTokens.NOT_IS))
		{
			advance(); // IS_KEYWORD or NOT_IS

			if(atSet(WHEN_CONDITION_RECOVERY_SET_WITH_ARROW))
			{
				error("Expecting a type");
			}
			else
			{
				parsing.parseTypeRef();
			}
			condition.done(WHEN_CONDITION_IS_PATTERN);
		}
		else
		{
			if(atSet(WHEN_CONDITION_RECOVERY_SET_WITH_ARROW))
			{
				error("Expecting an expression, is-condition or in-condition");
			}
			else
			{
				parseExpression();
			}
			condition.done(WHEN_CONDITION_EXPRESSION);
		}
		getBuilder().restoreNewlinesState();
	}

	/*
		 * arrayAccess
		 *   : "[" element{","} "]"
		 *   ;
		 */
	private void parseArrayAccess()
	{
		assert _at(NapileTokens.LBRACKET);

		PsiBuilder.Marker indices = mark();

		getBuilder().disableNewlines();
		advance(); // LBRACKET

		while(true)
		{
			if(at(NapileTokens.COMMA))
				errorAndAdvance("Expecting an index element");
			if(at(NapileTokens.RBRACKET))
			{
				error("Expecting an index element");
				break;
			}
			parseExpression();
			if(!at(NapileTokens.COMMA))
				break;
			advance(); // COMMA
		}

		expect(NapileTokens.RBRACKET, "Expecting ']'");
		getBuilder().restoreNewlinesState();

		indices.done(INDICES);
	}

	public void parseLinkMethodExpression(@Nullable final PsiBuilder.Marker currentMarker)
	{
		PsiBuilder.Marker marker = currentMarker == null ? mark() : currentMarker;

		advance();

		if(at(NapileTokens.IDENTIFIER))
			parseOneTokenExpression(REFERENCE_EXPRESSION);
		else
			error("Identifier expected");

		parsing.parseTypeArgumentList();

		if(at(NapileTokens.LPAR))
		{
			PsiBuilder.Marker list = mark();

			advance();

			while(true)
			{
				if(at(NapileTokens.COMMA))
				{
					errorAndAdvance("Expecting a type");
					continue;
				}

				parsing.parseTypeRef();
				if(!at(NapileTokens.COMMA))
					break;

				advance();
			}

			expect(NapileTokens.RPAR, "')' expected");

			list.done(LINK_METHOD_TYPE_LIST);
		}

		marker.done(LINK_METHOD_EXPRESSION);
	}

	private void parseMultiTypeExpression()
	{
		PsiBuilder.Marker marker = mark();

		advance();

		while(true)
		{
			parseExpression();

			if(at(NapileTokens.COMMA))
				advance();
			else
				break;
		}

		expect(NapileTokens.RBRACKET, "']' expected");

		marker.done(NapileNodes.MULTI_TYPE_EXPRESSION);
	}

	/*
		 * functionLiteral
		 *   : "{" expressions "}"
		 *   : "{" (modifiers SimpleName){","} "->" statements "}"
		 *   : "{" (type ".")? "(" (modifiers SimpleName (":" type)?){","} ")" (":" type)? "->" expressions "}"
		 *   ;
		 */
	private void parseFunctionLiteral()
	{
		parseFunctionLiteral(false);
	}

	private void parseFunctionLiteral(boolean preferBlock)
	{
		assert _at(NapileTokens.LBRACE);

		PsiBuilder.Marker literalExpression = mark();

		PsiBuilder.Marker literal = mark();

		getBuilder().enableNewlines();
		advance(); // LBRACE

		boolean paramsFound = false;

		if(at(NapileTokens.ARROW))
		{
			//   { -> ...}
			advance(); // ARROW
			mark().done(CALL_PARAMETER_LIST);
			paramsFound = true;
		}
		else if(at(NapileTokens.LPAR))
		{
			// Look for ARROW after matching RPAR
			//   {(a, b) -> ...}

			{
				boolean preferParamsToExpressions = isConfirmedParametersByComma();

				PsiBuilder.Marker rollbackMarker = mark();
				parseFunctionLiteralParametersAndType();

				paramsFound = preferParamsToExpressions ? rollbackOrDrop(rollbackMarker, NapileTokens.ARROW, "An -> is expected", NapileTokens.RBRACE) : rollbackOrDropAt(rollbackMarker, NapileTokens.ARROW);
			}

			if(!paramsFound)
			{
				// If not found, try a typeRef DOT and then LPAR .. RPAR ARROW
				//   {((A) -> B).(x) -> ... }
				paramsFound = parseFunctionTypeDotParametersAndType();
			}
		}
		else
		{
			if(at(NapileTokens.IDENTIFIER))
			{
				// Try to parse a simple name list followed by an ARROW
				//   {a -> ...}
				//   {a, b -> ...}
				PsiBuilder.Marker rollbackMarker = mark();
				boolean preferParamsToExpressions = (lookahead(1) == NapileTokens.COMMA);
				parseFunctionLiteralShorthandParameterList();
				parseOptionalFunctionLiteralType();

				paramsFound = preferParamsToExpressions ? rollbackOrDrop(rollbackMarker, NapileTokens.ARROW, "An -> is expected", NapileTokens.RBRACE) : rollbackOrDropAt(rollbackMarker, NapileTokens.ARROW);
			}
			if(!paramsFound && atSet(NapileParsing.TYPE_REF_FIRST))
			{
				// Try to parse a type DOT valueParameterList ARROW
				//   {A.(b) -> ...}
				paramsFound = parseFunctionTypeDotParametersAndType();
			}
		}

		if(!paramsFound)
		{
			if(preferBlock)
			{
				literal.drop();
				parseStatements();
				expect(NapileTokens.RBRACE, "Expecting '}'");
				literalExpression.done(BLOCK);
				getBuilder().restoreNewlinesState();

				return;
			}
		}

		PsiBuilder.Marker body = mark();
		parseStatements();
		body.done(BLOCK);

		expect(NapileTokens.RBRACE, "Expecting '}'");
		getBuilder().restoreNewlinesState();

		literal.done(ANONYM_METHOD);
		literalExpression.done(ANONYM_METHOD_EXPRESSION);
	}

	private boolean rollbackOrDropAt(PsiBuilder.Marker rollbackMarker, IElementType dropAt)
	{
		if(at(dropAt))
		{
			advance(); // dropAt
			rollbackMarker.drop();
			return true;
		}
		rollbackMarker.rollbackTo();
		return false;
	}

	private boolean rollbackOrDrop(PsiBuilder.Marker rollbackMarker, NapileToken expected, String expectMessage, IElementType validForDrop)
	{
		if(at(expected))
		{
			advance(); // dropAt
			rollbackMarker.drop();
			return true;
		}
		else if(at(validForDrop))
		{
			rollbackMarker.drop();
			expect(expected, expectMessage);
			return true;
		}

		rollbackMarker.rollbackTo();
		return false;
	}


	/*
		 * SimpleName{,}
		 */
	private void parseFunctionLiteralShorthandParameterList()
	{
		PsiBuilder.Marker parameterList = mark();

		while(!eof())
		{
			PsiBuilder.Marker parameter = mark();

			//            int parameterNamePos = matchTokenStreamPredicate(new LastBefore(new At(IDENTIFIER), new AtOffset(doubleArrowPos)));
			//            createTruncatedBuilder(parameterNamePos).parseModifierList(MODIFIER_LIST, false);

			expect(NapileTokens.IDENTIFIER, "Expecting parameter name", TokenSet.create(NapileTokens.ARROW));

			parameter.done(CALL_PARAMETER_AS_VARIABLE);

			if(at(NapileTokens.COLON))
			{
				PsiBuilder.Marker errorMarker = mark();
				advance(); // COLON
				parsing.parseTypeRef();
				errorMarker.error("To specify a type of a parameter or a return type, use the full notation: {(parameter : Type) : ReturnType -> ...}");
			}
			else if(at(NapileTokens.ARROW))
			{
				break;
			}
			else if(at(NapileTokens.COMMA))
			{
				advance(); // COMMA
			}
			else
			{
				error("Expecting '->' or ','");
				break;
			}
		}

		parameterList.done(CALL_PARAMETER_LIST);
	}

	// Check that position is followed by top level comma. It can't be expression and we want it be
	// parsed as parameters in function literal
	private boolean isConfirmedParametersByComma()
	{
		assert _at(NapileTokens.LPAR);
		PsiBuilder.Marker lparMarker = mark();
		advance(); // LPAR
		int comma = matchTokenStreamPredicate(new FirstBefore(new At(NapileTokens.COMMA), new AtSet(NapileTokens.ARROW, NapileTokens.RPAR)));
		lparMarker.rollbackTo();
		return comma > 0;
	}

	private boolean parseFunctionTypeDotParametersAndType()
	{
		PsiBuilder.Marker rollbackMarker = mark();

		// True when it's confirmed that body of literal can't be simple expressions and we prefer to parse
		// it to function params if possible.
		boolean preferParamsToExpressions = false;

		int lastDot = matchTokenStreamPredicate(new LastBefore(new At(NapileTokens.DOT), new AtSet(NapileTokens.ARROW, NapileTokens.RPAR)));
		if(lastDot >= 0)
		{
			createTruncatedBuilder(lastDot).parseTypeRef();
			if(at(NapileTokens.DOT))
			{
				advance(); // DOT

				if(at(NapileTokens.LPAR))
				{
					preferParamsToExpressions = isConfirmedParametersByComma();
				}

				parseFunctionLiteralParametersAndType();
			}
		}

		return preferParamsToExpressions ? rollbackOrDrop(rollbackMarker, NapileTokens.ARROW, "An -> is expected", NapileTokens.RBRACE) : rollbackOrDropAt(rollbackMarker, NapileTokens.ARROW);
	}

	private void parseFunctionLiteralParametersAndType()
	{
		parseFunctionLiteralParameterList();
		parseOptionalFunctionLiteralType();
	}

	/*
		 * (":" type)?
		 */
	private void parseOptionalFunctionLiteralType()
	{
		if(at(NapileTokens.COLON))
		{
			advance(); // COLON
			if(at(NapileTokens.ARROW))
			{
				error("Expecting a type");
			}
			else
			{
				parsing.parseTypeRef();
			}
		}
	}

	/*
		 * "(" (modifiers SimpleName (":" type)?){","} ")"
		 */
	private void parseFunctionLiteralParameterList()
	{
		PsiBuilder.Marker list = mark();
		expect(NapileTokens.LPAR, "Expecting a parameter list in parentheses (...)", TokenSet.create(NapileTokens.ARROW, NapileTokens.COLON));

		getBuilder().disableNewlines();

		if(!at(NapileTokens.RPAR))
		{
			while(true)
			{
				if(at(NapileTokens.COMMA))
					errorAndAdvance("Expecting a parameter declaration");

				PsiBuilder.Marker parameter = mark();
				int parameterNamePos = matchTokenStreamPredicate(new LastBefore(new At(NapileTokens.IDENTIFIER), new AtSet(NapileTokens.COMMA, NapileTokens.RPAR, NapileTokens.COLON, NapileTokens.ARROW)));
				createTruncatedBuilder(parameterNamePos).parseModifierList(false);

				expect(NapileTokens.IDENTIFIER, "Expecting parameter declaration");

				if(at(NapileTokens.COLON))
				{
					advance(); // COLON
					parsing.parseTypeRef();
				}
				parameter.done(CALL_PARAMETER_AS_VARIABLE);
				if(!at(NapileTokens.COMMA))
					break;
				advance(); // COMMA

				if(at(NapileTokens.RPAR))
				{
					error("Expecting a parameter declaration");
					break;
				}
			}
		}

		getBuilder().restoreNewlinesState();

		expect(NapileTokens.RPAR, "Expecting ')", TokenSet.create(NapileTokens.ARROW, NapileTokens.COLON));
		list.done(CALL_PARAMETER_LIST);
	}

	/*
		 * expressions
		 *   : SEMI* statement{SEMI+} SEMI*
		 */
	public void parseStatements()
	{
		while(at(NapileTokens.SEMICOLON))
			advance(); // SEMICOLON
		while(!eof() && !at(NapileTokens.RBRACE))
		{
			if(!atSet(STATEMENT_FIRST))
			{
				errorAndAdvance("Expecting an element");
			}
			if(atSet(STATEMENT_FIRST))
			{
				parseStatement();
			}
			if(at(NapileTokens.SEMICOLON))
			{
				while(at(NapileTokens.SEMICOLON))
					advance(); // SEMICOLON
			}
			else if(at(NapileTokens.RBRACE))
			{
				break;
			}
			else if(!getBuilder().newlineBeforeCurrentToken())
			{
				errorUntil("Unexpected tokens (use ';' to separate expressions on the same line)", TokenSet.create(NapileTokens.EOL_OR_SEMICOLON));
			}
		}
	}

	/*
		 * statement
		 *  : expression
		 *  : declaration
		 *  ;
		 */
	private void parseStatement()
	{
		if(!parsing.parseMemberDeclaration(true))
		{
			if(!atSet(EXPRESSION_FIRST))
			{
				errorAndAdvance("Expecting a statement");
			}
			else
			{
				parseExpression();
			}
		}
	}


	/*
		 * doWhile
		 *   : "do" element "while" "(" element ")"
		 *   ;
		 */
	private void parseDoWhile()
	{
		assert _at(NapileTokens.DO_KEYWORD);

		PsiBuilder.Marker loop = mark();

		advance(); // DO_KEYWORD

		if(!at(NapileTokens.WHILE_KEYWORD))
		{
			parseControlStructureBody();
		}

		if(expect(NapileTokens.WHILE_KEYWORD, "Expecting 'while' followed by a post-condition"))
		{
			parseCondition();
		}

		loop.done(DO_WHILE);
	}

	private void parseLabel0()
	{
		assert _at(NapileTokens.LABEL_KEYWORD);

		PsiBuilder.Marker marker = mark();

		advance();

		if(!at(NapileTokens.IDENTIFIER))
			error("Identifier expected");
		else
			advance();

		parsing.parseBlock();

		marker.done(LABEL_EXPRESSION);
	}
	/*
		 * while
		 *   : "while" "(" element ")" element
		 *   ;
		 */
	private void parseWhile()
	{
		assert _at(NapileTokens.WHILE_KEYWORD);

		PsiBuilder.Marker loop = mark();

		advance(); // WHILE_KEYWORD

		parseCondition();

		parseControlStructureBody();

		loop.done(WHILE);
	}

	/*
		 * for
		 *   : "for" "(" attributes valOrVar? SimpleName (":" type)? "in" element ")" element
		 *   ;
		 *
		 *   TODO: empty loop body (at the end of the block)?
		 */
	private void parseFor()
	{
		assert _at(NapileTokens.FOR_KEYWORD);

		PsiBuilder.Marker loop = mark();

		advance(); // FOR_KEYWORD

		getBuilder().disableNewlines();
		expect(NapileTokens.LPAR, "Expecting '(' to open a loop range", TokenSet.create(NapileTokens.RPAR,  NapileTokens.VAR_KEYWORD, NapileTokens.IDENTIFIER));

		PsiBuilder.Marker parameter = mark();

		parsing.parseModifierList(false);

		if(atSet(NapileTokens.VARIABLE_LIKE_KEYWORDS))
			advance();

		if(!parsing.parseIdeTemplate())
			expect(NapileTokens.IDENTIFIER, "Expecting a variable name", TokenSet.create(NapileTokens.COLON));

		if(at(NapileTokens.COLON))
		{
			advance(); // COLON
			parsing.parseTypeRef();
		}
		parameter.done(CALL_PARAMETER_AS_VARIABLE);

		expect(NapileTokens.IN_KEYWORD, "Expecting 'in'");

		PsiBuilder.Marker range = mark();
		parseExpression();
		range.done(LOOP_RANGE);

		expectNoAdvance(NapileTokens.RPAR, "Expecting ')'");
		getBuilder().restoreNewlinesState();

		parseControlStructureBody();

		loop.done(FOR);
	}

	/**
	 * If it has no ->, it's a block, otherwise a function literal
	 */
	private void parseExpressionPreferringBlocks()
	{
		if(at(NapileTokens.LBRACE))
		{
			parseFunctionLiteral(true);
		}
		else
		{
			parseExpression();
		}
	}

	/*
		 * element
		 */
	private void parseControlStructureBody()
	{
		PsiBuilder.Marker body = mark();
		if(!at(NapileTokens.SEMICOLON))
		{
			parseExpressionPreferringBlocks();
		}
		body.done(BODY);
	}

	/*
		 * try
		 *   : "try" block catchBlock* finallyBlock?
		 *   ;
		 * catchBlock
		 *   : "catch" "(" attributes SimpleName ":" userType ")" block
		 *   ;
		 *
		 * finallyBlock
		 *   : "finally" block
		 *   ;
		 */
	private void parseTry()
	{
		assert _at(NapileTokens.TRY_KEYWORD);

		PsiBuilder.Marker tryExpression = mark();

		advance(); // TRY_KEYWORD

		parsing.parseBlock();

		boolean catchOrFinally = false;
		while(at(NapileTokens.CATCH_KEYWORD))
		{
			catchOrFinally = true;
			PsiBuilder.Marker catchBlock = mark();
			advance(); // CATCH_KEYWORD

			parsing.parseCallParameterList(false, TokenSet.create(NapileTokens.LBRACE, NapileTokens.FINALLY_KEYWORD, NapileTokens.CATCH_KEYWORD));

			parsing.parseBlock();
			catchBlock.done(CATCH);
		}

		if(at(NapileTokens.FINALLY_KEYWORD))
		{
			catchOrFinally = true;
			PsiBuilder.Marker finallyBlock = mark();

			advance(); // FINALLY_KEYWORD

			parsing.parseBlock();

			finallyBlock.done(FINALLY);
		}

		if(!catchOrFinally)
		{
			error("Expecting 'catch' or 'finally'");
		}

		tryExpression.done(TRY);
	}

	/*
		 * if
		 *   : "if" "(" element ")" element SEMI? ("else" element)?
		 *   ;
		 */
	private void parseIf()
	{
		assert _at(NapileTokens.IF_KEYWORD);

		PsiBuilder.Marker marker = mark();

		advance(); //IF_KEYWORD

		parseCondition();

		PsiBuilder.Marker thenBranch = mark();
		if(!at(NapileTokens.ELSE_KEYWORD) && !at(NapileTokens.SEMICOLON))
		{
			parseExpressionPreferringBlocks();
		}
		if(at(NapileTokens.SEMICOLON) && lookahead(1) == NapileTokens.ELSE_KEYWORD)
		{
			advance(); // SEMICOLON
		}
		thenBranch.done(THEN);

		if(at(NapileTokens.ELSE_KEYWORD))
		{
			advance(); // ELSE_KEYWORD

			PsiBuilder.Marker elseBranch = mark();
			if(!at(NapileTokens.SEMICOLON))
			{
				parseExpressionPreferringBlocks();
			}
			elseBranch.done(ELSE);
		}

		marker.done(IF);
	}

	/*
		 * "(" element ")"
		 */
	private void parseCondition()
	{
		getBuilder().disableNewlines();
		expect(NapileTokens.LPAR, "Expecting a condition in parentheses '(...)'");

		PsiBuilder.Marker condition = mark();
		parseExpression();
		condition.done(CONDITION);

		expect(NapileTokens.RPAR, "Expecting ')");
		getBuilder().restoreNewlinesState();
	}

	/*
		 * : "continue"
		 * : "break" getEntryPoint?
		 */
	private void parseJump(NapileNode type)
	{
		assert _at(NapileTokens.BREAK_KEYWORD) || _at(NapileTokens.CONTINUE_KEYWORD);

		PsiBuilder.Marker marker = mark();

		advance(); // BREAK_KEYWORD or CONTINUE_KEYWORD

		//TODO [VISTALL] rework it
		if(type == BREAK && at(NapileTokens.IDENTIFIER))
			parseOneTokenExpression(LABEL_REFERENCE);

		marker.done(type);
	}

	/*
		 * "return" getEntryPoint? element?
		 */
	private void parseReturn()
	{
		assert _at(NapileTokens.RETURN_KEYWORD);

		PsiBuilder.Marker returnExpression = mark();

		advance(); // RETURN_KEYWORD


		if(atSet(EXPRESSION_FIRST) && !at(NapileTokens.EOL_OR_SEMICOLON))
			parseExpression();

		returnExpression.done(RETURN);
	}

	/*
		 * : "throw" element
		 */
	private void parseThrow()
	{
		assert _at(NapileTokens.THROW_KEYWORD);

		PsiBuilder.Marker marker = mark();

		advance(); // THROW_KEYWORD

		parseExpression();

		marker.done(THROW);
	}

	/*
	 * "arrayOf" "(" expression ("," expression)* ")"
	 */
	private void parseArrayExpression()
	{
		PsiBuilder.Marker mark = mark();

		advance();

		if(expect(NapileTokens.LPAR, "'(' expected"))
		{
			while(!at(NapileTokens.RPAR) && !eof())
			{
				parseExpression();

				if(at(NapileTokens.COMMA))
					advance();
				else
					break;
			}

			expect(NapileTokens.RPAR, "')' expected");
		}

		mark.done(ARRAY);
	}

	/*
		 * "(" expression ")"
		 */
	private void parseParenthesizedExpression()
	{
		assert _at(NapileTokens.LPAR);

		PsiBuilder.Marker mark = mark();

		getBuilder().disableNewlines();
		advance(); // LPAR
		if(at(NapileTokens.RPAR))
		{
			error("Expecting an expression");
		}
		else
		{
			parseExpression();
		}

		expect(NapileTokens.RPAR, "Expecting ')'");
		getBuilder().restoreNewlinesState();

		mark.done(PARENTHESIZED);
	}


	/*
		 * "this" label?
		 */
	private void parseThisExpression()
	{
		assert _at(NapileTokens.THIS_KEYWORD);
		PsiBuilder.Marker mark = mark();

		PsiBuilder.Marker thisReference = mark();
		advance(); // THIS_KEYWORD
		thisReference.done(REFERENCE_EXPRESSION);
		if(at(NapileTokens.LT))
		{
			// This may be "this < foo" or "this<foo>", thus the backtracking
			PsiBuilder.Marker supertype = mark();

			getBuilder().disableNewlines();
			advance(); // LT

			parsing.parseTypeRef();

			if(at(NapileTokens.GT))
			{
				advance(); // GT
				supertype.drop();
			}
			else
			{
				supertype.rollbackTo();
			}
			getBuilder().restoreNewlinesState();
		}
		mark.done(THIS_EXPRESSION);
	}

	/*
		 * "this" ("<" type ">")? label?
		 */
	private void parseSuperExpression()
	{
		assert _at(NapileTokens.SUPER_KEYWORD);
		PsiBuilder.Marker mark = mark();

		PsiBuilder.Marker superReference = mark();
		advance(); // SUPER_KEYWORD
		superReference.done(REFERENCE_EXPRESSION);

		if(at(NapileTokens.LT))
		{
			// This may be "super < foo" or "super<foo>", thus the backtracking
			PsiBuilder.Marker supertype = mark();

			getBuilder().disableNewlines();
			advance(); // LT

			parsing.parseTypeRef();

			if(at(NapileTokens.GT))
			{
				advance(); // GT
				supertype.drop();
			}
			else
			{
				supertype.rollbackTo();
			}
			getBuilder().restoreNewlinesState();
		}
		mark.done(SUPER_EXPRESSION);
	}

	/*
		 * valueArguments
		 *   : "(" (SimpleName "=")? "*"? element{","} ")"
		 *   ;
		 */
	public void parseValueArgumentList()
	{
		PsiBuilder.Marker list = mark();

		getBuilder().disableNewlines();
		expect(NapileTokens.LPAR, "Expecting an argument list", EXPRESSION_FOLLOW);

		if(!at(NapileTokens.RPAR))
		{
			while(true)
			{
				while(at(NapileTokens.COMMA))
					errorAndAdvance("Expecting an argument");
				parseValueArgument();
				if(!at(NapileTokens.COMMA))
					break;
				advance(); // COMMA
				if(at(NapileTokens.RPAR))
				{
					error("Expecting an argument");
					break;
				}
			}
		}

		expect(NapileTokens.RPAR, "Expecting ')'", EXPRESSION_FOLLOW);
		getBuilder().restoreNewlinesState();

		list.done(VALUE_ARGUMENT_LIST);
	}

	/*
		 * (SimpleName "=")? "*"? element
		 */
	private void parseValueArgument()
	{
		PsiBuilder.Marker argument = mark();
		if(at(NapileTokens.IDENTIFIER) && lookahead(1) == NapileTokens.EQ)
		{
			PsiBuilder.Marker argName = mark();
			PsiBuilder.Marker reference = mark();
			advance(); // IDENTIFIER
			reference.done(REFERENCE_EXPRESSION);
			argName.done(VALUE_ARGUMENT_NAME);
			advance(); // EQ
		}
		if(at(NapileTokens.MUL))
		{
			advance(); // MUL
		}
		parseExpression();
		argument.done(VALUE_ARGUMENT);
	}

	/*
		 * "object" (":" delegationSpecifier{","})? classBody // Cannot make class body optional: foo(object : F, A)
		 */
	public void parseObjectLiteral()
	{
		PsiBuilder.Marker literal = mark();
		PsiBuilder.Marker declaration = mark();
		parsing.parseObject(); // Body is not optional because of foo(object : A, B)
		declaration.done(ANONYM_CLASS);
		literal.done(ANONYM_CLASS_EXPRESSION);
	}

	private void parseOneTokenExpression(NapileNode type)
	{
		PsiBuilder.Marker mark = mark();
		advance();
		mark.done(type);
	}

	@Override
	protected NapileParsing create(SemanticWhitespaceAwarePsiBuilder builder)
	{
		return parsing.create(builder);
	}

	private boolean interruptedWithNewLine()
	{
		return !ALLOW_NEWLINE_OPERATIONS.contains(tt()) && getBuilder().newlineBeforeCurrentToken();
	}
}
