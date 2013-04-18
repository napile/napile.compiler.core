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
package org.napile.compiler.lang.parsing;

import static org.napile.compiler.lang.lexer.NapileNodes.*;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.lexer.NapileKeywordToken;
import org.napile.compiler.lang.lexer.NapileNode;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.lexer.NapileTokens;
import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.Consumer;

/**
 * @author max
 * @author abreslav
 */
public class NapileParsing extends AbstractNapileParsing
{
	// TODO: token sets to constants, including derived methods
	public static final Map<String, IElementType> MODIFIER_KEYWORD_MAP = new HashMap<String, IElementType>();

	static
	{
		for(IElementType softKeyword : NapileTokens.MODIFIER_KEYWORDS.getTypes())
		{
			MODIFIER_KEYWORD_MAP.put(((NapileKeywordToken) softKeyword).getValue(), softKeyword);
		}
	}

	static class TokenDetector implements Consumer<IElementType>
	{
		boolean detected;
		final IElementType elementType;

		TokenDetector(IElementType elementType)
		{
			this.elementType = elementType;
		}

		@Override
		public void consume(IElementType elementType)
		{
			if(this.elementType == elementType)
				detected = true;
		}
	}

	private static final TokenSet CLASS_NAME_RECOVERY_SET = TokenSet.orSet(TokenSet.create(NapileTokens.LT, NapileTokens.LPAR, NapileTokens.COLON, NapileTokens.LBRACE));
	public static final TokenSet TYPE_PARAMETER_GT_RECOVERY_SET = TokenSet.create(NapileTokens.LPAR, NapileTokens.COLON, NapileTokens.LBRACE, NapileTokens.GT);
	private static final TokenSet PARAMETER_NAME_RECOVERY_SET = TokenSet.create(NapileTokens.COLON, NapileTokens.EQ, NapileTokens.COMMA, NapileTokens.RPAR);
	private static final TokenSet NAMESPACE_NAME_RECOVERY_SET = TokenSet.create(NapileTokens.DOT, NapileTokens.EOL_OR_SEMICOLON);
	/*package*/ static final TokenSet TYPE_REF_FIRST = TokenSet.create(NapileTokens.LBRACKET, NapileTokens.IDENTIFIER, NapileTokens.METH_KEYWORD, NapileTokens.LPAR, NapileTokens.THIS_KEYWORD, NapileTokens.HASH);

	public static NapileParsing createForTopLevel(SemanticWhitespaceAwarePsiBuilder builder)
	{
		NapileParsing jetParsing = new NapileParsing(builder);
		jetParsing.myExpressionParsing = new NapileExpressionParsing(builder, jetParsing);
		return jetParsing;
	}

	private NapileExpressionParsing myExpressionParsing;

	private NapileParsing(SemanticWhitespaceAwarePsiBuilder builder)
	{
		super(builder);
	}

	/*
		 * [start] jetlFile
		 *   : preamble class* [eof]
		 *   ;
		 */
	void parseFile()
	{
		PsiBuilder.Marker fileMarker = mark();

		parsePreamble();

		while(!eof())
			parseClass0();

		fileMarker.done(NAPILE_FILE);
	}


	/*
		 *preamble
		 *  : namespaceHeader? import*
		 *  ;
		 */
	private void parsePreamble()
	{		/*
		 * namespaceHeader
         *   : modifiers "package" SimpleName{"."} SEMI?
         *   ;
         */
		PsiBuilder.Marker namespaceHeader = mark();
		PsiBuilder.Marker firstEntry = mark();

		if(at(NapileTokens.PACKAGE_KEYWORD))
		{
			advance(); // PACKAGE_KEYWORD


			parseNamespaceName();

			if(at(NapileTokens.LBRACE))
			{
				// Because it's blocked namespace and it will be parsed as one of top level objects
				firstEntry.rollbackTo();
				namespaceHeader.done(PACKAGE);
				return;
			}

			firstEntry.drop();

			consumeIf(NapileTokens.SEMICOLON);
		}
		else
		{
			firstEntry.rollbackTo();
		}
		namespaceHeader.done(PACKAGE);

		parseImportDirectives();
	}

	/* SimpleName{"."} */
	private void parseNamespaceName()
	{
		while(true)
		{
			if(getBuilder().newlineBeforeCurrentToken())
			{
				errorWithRecovery("Package name must be a '.'-separated identifier list placed on a single line", NAMESPACE_NAME_RECOVERY_SET);
				break;
			}

			PsiBuilder.Marker nsName = mark();
			if(expect(NapileTokens.IDENTIFIER, "Package name must be a '.'-separated identifier list", NAMESPACE_NAME_RECOVERY_SET))
			{
				nsName.done(REFERENCE_EXPRESSION);
			}
			else
			{
				nsName.drop();
			}

			if(at(NapileTokens.DOT))
			{
				advance(); // DOT
			}
			else
			{
				break;
			}
		}
	}

	/*
		 * import
		 *   : "import" ("namespace" ".")? SimpleName{"."} ("." "*" | "as" SimpleName)? SEMI?
		 *   ;
		 */
	private void parseImportDirective()
	{
		assert _at(NapileTokens.IMPORT_KEYWORD);
		PsiBuilder.Marker importDirective = mark();
		advance(); // IMPORT_KEYWORD

		PsiBuilder.Marker qualifiedName = mark();

		PsiBuilder.Marker reference = mark();
		expect(NapileTokens.IDENTIFIER, "Expecting qualified name");
		reference.done(REFERENCE_EXPRESSION);
		while(at(NapileTokens.DOT) && lookahead(1) != NapileTokens.MUL)
		{
			advance(); // DOT

			reference = mark();
			if(expect(NapileTokens.IDENTIFIER, "Qualified name must be a '.'-separated identifier list", TokenSet.create(NapileTokens.AS_KEYWORD, NapileTokens.DOT, NapileTokens.SEMICOLON)))
			{
				reference.done(REFERENCE_EXPRESSION);
			}
			else
			{
				reference.drop();
			}

			PsiBuilder.Marker precede = qualifiedName.precede();
			qualifiedName.done(DOT_QUALIFIED_EXPRESSION);
			qualifiedName = precede;
		}
		qualifiedName.drop();

		if(at(NapileTokens.DOT))
		{
			advance(); // DOT
			assert _at(NapileTokens.MUL);
			advance(); // MUL
			handleUselessRename();
		}
		if(at(NapileTokens.AS_KEYWORD))
		{
			advance(); // AS_KEYWORD
			expect(NapileTokens.IDENTIFIER, "Expecting identifier", TokenSet.create(NapileTokens.SEMICOLON));
		}
		consumeIf(NapileTokens.SEMICOLON);
		importDirective.done(IMPORT_DIRECTIVE);
	}

	private void parseImportDirectives()
	{
		while(at(NapileTokens.IMPORT_KEYWORD))
		{
			parseImportDirective();
		}
	}

	private void handleUselessRename()
	{
		if(at(NapileTokens.AS_KEYWORD))
		{
			PsiBuilder.Marker as = mark();
			advance(); // AS_KEYWORD
			consumeIf(NapileTokens.IDENTIFIER);
			as.error("Cannot rename a all imported items to one identifier");
		}
	}

	/*
		 * parseClass0
		 *   : class
		 *   ;
		 */
	private void parseClass0()
	{
		PsiBuilder.Marker decl = mark();

		parseModifierList(true);

		IElementType keywordToken = tt();
		IElementType declType = null;

		if(keywordToken == NapileTokens.CLASS_KEYWORD)
			declType = parseClass();

		if(declType == null)
		{
			errorAndAdvance("Expecting package directive or top level declaration");
			decl.drop();
		}
		else
		{
			decl.done(declType);
		}
	}

	boolean parseModifierList(boolean napileDoc)
	{
		return parseModifierList(Consumer.EMPTY_CONSUMER, napileDoc);
	}

	/**
	 * (modifier | attribute)*
	 * <p/>
	 * Feeds modifiers (not attributes) into the passed consumer, if it is not null
	 */
	boolean parseModifierList(Consumer<IElementType> detector, boolean napileDoc)
	{
		if(tt() == NapileTokens.DOC_COMMENT)
		{
			if(!napileDoc)
				error("Wrong doc position");
			advance();
		}

		PsiBuilder.Marker list = mark();
		boolean empty = true;
		while(!eof())
		{
			if(atSet(NapileTokens.MODIFIER_KEYWORDS))
			{
				detector.consume(tt());

				advance();
			}
			else if(at(NapileTokens.AT))
			{
				parseAnnotations();
			}
			else
			{
				break;
			}
			empty = false;
		}
		/*if(empty)
		{
			list.drop();
		}
		else
		{  */
			list.done(MODIFIER_LIST);
		//}
		return !empty;
	}


	/*
		 * annotation
		 *   : annotationEntry+
		 *   ;
		 */
	public boolean parseAnnotations()
	{
		getBuilder().disableNewlines();

		while(at(NapileTokens.AT))
			parseAnnotation();

		getBuilder().restoreNewlinesState();
		return true;
	}

	/*
		 * annotationEntry
		 *   : "@" SimpleName{"."} typeArguments? valueArguments?
		 *   ;
		 */
	private void parseAnnotation()
	{
		assert _at(NapileTokens.AT);

		PsiBuilder.Marker attribute = mark();

		advance();

		PsiBuilder.Marker reference = mark();
		PsiBuilder.Marker typeReference = mark();
		parseUserType();
		typeReference.done(TYPE_REFERENCE);
		reference.done(CONSTRUCTOR_CALLEE);

		parseTypeArgumentList();

		if(at(NapileTokens.LPAR))
			myExpressionParsing.parseValueArgumentList();

		attribute.done(ANNOTATION);
	}

	/*
		 * class
		 *   : modifiers "class" SimpleName
		 *       typeParameters?
		 *         modifiers ("(" primaryConstructorParameter{","} ")")?
		 *       (":" attributes delegationSpecifier{","})?
		 *       typeConstraints
		 *       (classBody? | enumClassBody)
		 *   ;
		 */
	IElementType parseClass()
	{
		advance(); // CLASS_KEYWORD

		if(!parseIdeTemplate())
			expect(NapileTokens.IDENTIFIER, "Class name expected", CLASS_NAME_RECOVERY_SET);
		parseTypeParameterList();

		if(at(NapileTokens.COLON))
		{
			advance(); // COLON
			parseTypeList(EXTEND_TYPE_LIST, NapileTokens.AND);
		}

		if(at(NapileTokens.LBRACE))
			parseClassBody();

		return CLASS;
	}

	/*
		 * classBody
		 *   : ("{" memberDeclaration "}")?
		 *   ;
		 */
	void parseClassBody()
	{
		PsiBuilder.Marker body = mark();

		getBuilder().enableNewlines();
		expect(NapileTokens.LBRACE, "Expecting a class body", TokenSet.create(NapileTokens.LBRACE));

		if(!parseIdeTemplate())
		{
			while(!eof())
			{
				if(at(NapileTokens.RBRACE))
				{
					break;
				}
				parseMemberDeclaration(false);
			}
		}
		expect(NapileTokens.RBRACE, "Missing '}");
		getBuilder().restoreNewlinesState();

		body.done(CLASS_BODY);
	}

	/*
		 * memberDeclaration
		 *   : modifiers memberDeclaration'
		 *   ;
		 *
		 * memberDeclaration'
		 *   : classObject
		 *   : constructor
		 *   : function
		 *   : property
		 *   : class
		 *   : extension
		 *   : typedef
		 *   : anonymousInitializer
		 *   : object
		 *   ;
		 */
	protected boolean parseMemberDeclaration(boolean silent)
	{
		PsiBuilder.Marker decl = mark();

		IElementType declType = null;
		TokenDetector tokenDetector = new TokenDetector(NapileTokens.ENUM_KEYWORD);

		parseModifierList(tokenDetector, !silent);

		declType = parseMemberDeclarationRest(tokenDetector.detected);

		if(declType == null)
		{
			if(!silent)
				errorWithRecovery("Expecting member declaration", TokenSet.create(NapileTokens.RBRACE));
			decl.drop();
			return false;
		}
		else
			decl.done(declType);
		return true;
	}

	private IElementType parseMemberDeclarationRest(boolean enumModifier)
	{
		IElementType keywordToken = tt();
		IElementType declType = null;
		if(keywordToken == NapileTokens.CLASS_KEYWORD)
			declType = parseClass();
		else if(keywordToken == NapileTokens.METH_KEYWORD)
			declType = parseMethodOrMacro(METHOD);
		else if(keywordToken == NapileTokens.MACRO_KEYWORD)
			declType = parseMethodOrMacro(MACRO);
		else if(keywordToken == NapileTokens.THIS_KEYWORD)
			declType = parseConstructor();
		else if(keywordToken == NapileTokens.VAL_KEYWORD && enumModifier)
			declType = parseEnumVal();
		else if(NapileTokens.VARIABLE_LIKE_KEYWORDS.contains(keywordToken))
			declType = parseVariableOrValue(false);

		return declType;
	}

	/*
		 * object
		 *   : "object" SimpleName? ":" delegationSpecifier{","}? classBody?
		 *   ;
		 */
	void parseObject()
	{
		assert _at(NapileTokens.ANONYM_KEYWORD);

		advance(); // OBJECT_KEYWORD

		if(at(NapileTokens.COLON))
		{
			advance(); // COLON
			parseDelegationSpecifierList();
		}

		if(at(NapileTokens.LBRACE))
			parseClassBody();
	}

	IElementType parseEnumVal()
	{
		if(at(NapileTokens.VAL_KEYWORD))
			advance();
		else
			errorAndAdvance("Expecting 'val'");

		if(!parseIdeTemplate())
			expect(NapileTokens.IDENTIFIER, "Expecting identifier");

		if(expect(NapileTokens.COLON, "':' expected"))
			parseDelegationSpecifierList();

		return NapileNodes.ENUM_VALUE;
	}

	/*
		 * property
		 *   : modifiers ("val" | "var")
		 *       typeParameters? (type "." | attributes)?
		 *       SimpleName (":" type)?
		 *       typeConstraints
		 *       ("=" element SEMI?)?
		 *       (getter? setter? | setter? getter?) SEMI?
		 *   ;
		 */
	IElementType parseVariableOrValue(boolean typeReference)
	{
		if(atSet(NapileTokens.VARIABLE_LIKE_KEYWORDS))
			advance();
		else
			errorAndAdvance("Expecting 'var' or 'val'");

		parseTypeParameterList();

		getBuilder().disableJoiningComplexTokens();

		if(!parseIdeTemplate())
			expect(NapileTokens.IDENTIFIER, "Expecting identifier");

		getBuilder().restoreJoiningComplexTokensState();

		if(at(NapileTokens.COLON))
		{
			advance(); // COLON
			if(!parseIdeTemplate())
				parseTypeRef();
		}

		if(!typeReference)
		{
			if(at(NapileTokens.LBRACE))
			{
				advance();

				while(true)
				{
					PsiBuilder.Marker marker = mark();

					parseModifierList(false);

					if(atSet(NapileTokens.VARIABLE_ACCESS_KEYWORDS))
					{
						advance();

						if(at(NapileTokens.SEMICOLON))
						{
							advance();
						}
						else if(at(NapileTokens.EQ) || at(NapileTokens.LBRACE))
						{
							parseMethodOrMacroBody();
						}

						marker.done(VARIABLE_ACCESSOR);
					}
					else
					{
						marker.drop();
						break;
					}
				}

				expect(NapileTokens.RBRACE, "'}' expected");
			}

			if(at(NapileTokens.EQ))
			{
				advance(); // EQ
				myExpressionParsing.parseExpression();
			}

			consumeIf(NapileTokens.SEMICOLON);
		}

		return VARIABLE;
	}


	/*
		 * function
		 *   : modifiers ("meth" | "macro") typeParameters?
		 *       (type "." | attributes)?
		 *       SimpleName
		 *       typeParameters? functionParameters (":" type)?
		 *       typeConstraints
		 *       functionBody?
		 *   ;
		 */
	IElementType parseMethodOrMacro(@NotNull IElementType doneElement)
	{
		advance();

		if(at(NapileTokens.RBRACE))
		{
			error("Method body expected");
			return doneElement;
		}

		if(!parseIdeTemplate())
			expect(NapileTokens.IDENTIFIER, "Identifier expected");

		TokenSet valueParametersFollow = TokenSet.create(NapileTokens.COLON, NapileTokens.EQ, NapileTokens.LBRACE, NapileTokens.SEMICOLON, NapileTokens.RPAR);

		parseTypeParameterList();

		parseCallParameterList(false, valueParametersFollow);

		if(at(NapileTokens.COLON))
		{
			advance(); // COLON

			if(!parseIdeTemplate())
				parseTypeRef();
		}

		if(at(NapileTokens.SEMICOLON))
		{
			advance(); // SEMICOLON
		}
		else if(at(NapileTokens.EQ) || at(NapileTokens.LBRACE))
		{
			parseMethodOrMacroBody();
		}

		return doneElement;
	}

	/*
		 * constructor
		 *   : modifiers "this" functionParameters (":" initializer{","}) block?
		 *   ;
		 */
	private IElementType parseConstructor()
	{
		assert _at(NapileTokens.THIS_KEYWORD);

		advance(); // THIS_KEYWORD

		parseCallParameterList(false, TokenSet.create(NapileTokens.COLON, NapileTokens.LBRACE, NapileTokens.SEMICOLON));

		if(at(NapileTokens.COLON))
		{
			advance(); // COLON

			parseDelegationSpecifierList();
		}

		if(at(NapileTokens.LBRACE))
		{
			parseBlock();
		}
		else
		{
			consumeIf(NapileTokens.SEMICOLON);
		}

		return CONSTRUCTOR;
	}


	/*
		 * functionBody
		 *   : block
		 *   : "=" element
		 *   ;
		 */
	private void parseMethodOrMacroBody()
	{
		if(at(NapileTokens.LBRACE))
		{
			parseBlock();
		}
		else if(at(NapileTokens.EQ))
		{
			advance(); // EQ
			myExpressionParsing.parseExpression();
			consumeIf(NapileTokens.SEMICOLON);
		}
		else
		{
			errorAndAdvance("Expecting function body");
		}
	}

	/*
		 * block
		 *   : "{" (expressions)* "}"
		 *   ;
		 */
	void parseBlock()
	{
		PsiBuilder.Marker block = mark();

		getBuilder().enableNewlines();
		expect(NapileTokens.LBRACE, "Expecting '{' to open a block");

		myExpressionParsing.parseStatements();

		expect(NapileTokens.RBRACE, "Expecting '}");
		getBuilder().restoreNewlinesState();

		block.done(BLOCK);
	}

	/*
		 * delegationSpecifier{"&"}
		 */
	void parseDelegationSpecifierList()
	{
		PsiBuilder.Marker list = mark();

		while(true)
		{
			if(at(NapileTokens.AND))
			{
				errorAndAdvance("Expecting a delegation specifier");
				continue;
			}
			parseDelegationSpecifier();
			if(!at(NapileTokens.AND))
				break;
			advance(); // COMMA
		}

		list.done(DELEGATION_SPECIFIER_LIST);
	}

	void parseTypeList(@NotNull IElementType doneElement, @NotNull IElementType split)
	{
		PsiBuilder.Marker list = mark();

		while(true)
		{
			parseTypeRef();

			if(at(split))
				advance();
			else
				break;
		}

		list.done(doneElement);
	}

	private void parseDelegationSpecifier()
	{
		PsiBuilder.Marker marker = mark();

		PsiBuilder.Marker reference = mark();
		parseTypeRef();
		reference.done(CONSTRUCTOR_CALLEE);

		if(at(NapileTokens.LPAR))
			myExpressionParsing.parseValueArgumentList();

		marker.done(DELEGATOR_SUPER_CALL);
	}

	/*
		 * typeParameters
		 *   : ("<" typeParameter{","} ">"
		 *   ;
		 */
	private boolean parseTypeParameterList()
	{
		PsiBuilder.Marker list = mark();
		boolean result = false;
		if(at(NapileTokens.LT))
		{
			getBuilder().disableNewlines();
			advance(); // LT

			while(true)
			{
				if(at(NapileTokens.COMMA))
					errorAndAdvance("Expecting type parameter declaration");

				parseTypeParameter();

				if(!at(NapileTokens.COMMA))
					break;
				advance(); // COMMA
			}

			expect(NapileTokens.GT, "Missing '>'");
			getBuilder().restoreNewlinesState();
			result = true;
		}
		list.done(TYPE_PARAMETER_LIST);
		return result;
	}


	/*
		 * typeParameter
		 *   : modifiers SimpleName (("[" typeList? "]") | (":" userType))?
		 *   ;
		 */
	private void parseTypeParameter()
	{
		if(atSet(TYPE_PARAMETER_GT_RECOVERY_SET))
		{
			error("Type parameter declaration expected");
			return;
		}

		PsiBuilder.Marker mark = mark();

		parseModifierList(false);

		expect(NapileTokens.IDENTIFIER, "Type parameter name expected", TokenSet.EMPTY);

		while(at(NapileTokens.LPAR))
			parseCallParameterList(true, TokenSet.EMPTY);

		if(at(NapileTokens.COLON))
		{
			advance(); // COLON

			while(true)
			{
				parseTypeRef();

				if(at(NapileTokens.AND))
					advance();
				else
					break;
			}
		}

		mark.done(TYPE_PARAMETER);
	}

	/*
		 * type
		 *   : attributes typeDescriptor
		 *
		 * typeDescriptor
		 *   : selfType
		 *   : functionType
		 *   : userType
		 *   : tupleType
		 *   : nullableType
		 *   ;
		 *
		 * nullableType
		 *   : typeDescriptor "?"
		 */
	PsiBuilder.Marker parseTypeRef()
	{
		return parseTypeRef(TokenSet.EMPTY);
	}

	PsiBuilder.Marker parseTypeRef(TokenSet extraRecoverySet)
	{
		PsiBuilder.Marker typeRefMarker = parseTypeRefContents(extraRecoverySet);
		typeRefMarker.done(TYPE_REFERENCE);
		return typeRefMarker;
	}

	// The extraRecoverySet is needed for the foo(bar<x, 1, y>(z)) case, to tell whether we should stop
	// on expression-indicating symbols or not
	private PsiBuilder.Marker parseTypeRefContents(TokenSet extraRecoverySet)
	{
		getBuilder().disableJoiningComplexTokens();

		PsiBuilder.Marker typeRefMarker = mark();

		parseAnnotations();

		// identifier
		if(at(NapileTokens.IDENTIFIER))
			parseUserType();
		// {() -> String}
		else if(at(NapileTokens.LBRACE))
			parseAnonymMethodType();
		// [val a : String]
		else if(at(NapileTokens.LBRACKET))
			parseMultiType();
		// this
		else if(at(NapileTokens.THIS_KEYWORD))
			parseSelfType();
		else
			errorWithRecovery("Type expected", TokenSet.orSet(TokenSet.create(NapileTokens.EQ, NapileTokens.COMMA, NapileTokens.GT, NapileTokens.RBRACKET, NapileTokens.DOT, NapileTokens.RPAR, NapileTokens.RBRACE, NapileTokens.LBRACE, NapileTokens.SEMICOLON), extraRecoverySet));

		if(at(NapileTokens.QUEST))
		{
			PsiBuilder.Marker precede = typeRefMarker.precede();

			advance(); // QUEST
			typeRefMarker.done(NULLABLE_TYPE);

			typeRefMarker = precede;
		}

		getBuilder().restoreJoiningComplexTokensState();

		return typeRefMarker;
	}

	/*
		 * userType
		 *   : simpleUserType{"."}
		 *   ;
		 */
	private void parseUserType()
	{
		PsiBuilder.Marker userType = mark();

		PsiBuilder.Marker reference = mark();
		while(true)
		{
			if(expect(NapileTokens.IDENTIFIER, "Expecting type name", TokenSet.orSet(NapileExpressionParsing.EXPRESSION_FIRST, NapileExpressionParsing.EXPRESSION_FOLLOW)))
			{
				reference.done(REFERENCE_EXPRESSION);
			}
			else
			{
				reference.drop();
				break;
			}

			parseTypeArgumentList();
			if(!at(NapileTokens.DOT))
			{
				break;
			}

			PsiBuilder.Marker precede = userType.precede();
			userType.done(USER_TYPE);
			userType = precede;

			advance(); // DOT
			reference = mark();
		}

		userType.done(USER_TYPE);
	}

	/*
		 * selfType
		 *   : "this"
		 *   ;
		 */
	private void parseSelfType()
	{
		assert _at(NapileTokens.THIS_KEYWORD);

		PsiBuilder.Marker type = mark();

		PsiBuilder.Marker referenceMarker = mark();
		advance(); // THIS_KEYWORD
		referenceMarker.done(NapileNodes.REFERENCE_EXPRESSION);

		type.done(SELF_TYPE);
	}

	private void parseAnonymMethodType()
	{
		PsiBuilder.Marker anonymMethodType = mark();

		advance(); // LBRACE

		if(at(NapileTokens.IDENTIFIER))
		{
			advance();
		}

		if(at(NapileTokens.LPAR))
		{
			parseCallParameterList(true, TokenSet.EMPTY);

			expect(NapileTokens.ARROW, "'->' expecting", TYPE_REF_FIRST);

			parseTypeRef();
		}
		else
		{
			if(!at(NapileTokens.RBRACE))
				parseTypeRef();
		}

		expect(NapileTokens.RBRACE, "'}' expecting");

		anonymMethodType.done(METHOD_TYPE);
	}

	private void parseMultiType()
	{
		PsiBuilder.Marker mark = mark();

		advance();

		if(!at(NapileTokens.RBRACKET))
		{
			while(true)
			{
				PsiBuilder.Marker varMark = mark();

				varMark.done(parseVariableOrValue(true));

				if(at(NapileTokens.COMMA))
					advance();
				else
					break;
			}
		}

		expect(NapileTokens.RBRACKET, "']' expected");

		mark.done(MULTI_TYPE);
	}
	/*
		 *  (optionalProjection type){","}
		 */
	protected PsiBuilder.Marker parseTypeArgumentList()
	{
		if(!at(NapileTokens.LT))
			return null;

		PsiBuilder.Marker list = mark();

		tryParseTypeArgumentList(TokenSet.EMPTY);

		list.done(TYPE_ARGUMENT_LIST);
		return list;
	}

	boolean tryParseTypeArgumentList(TokenSet extraRecoverySet)
	{
		getBuilder().disableNewlines();
		advance(); // LT

		while(true)
		{
			parseTypeRef(extraRecoverySet);

			if(!at(NapileTokens.COMMA))
				break;
			advance(); // COMMA
		}

		boolean atGT = at(NapileTokens.GT);
		if(!atGT)
		{
			error("Expecting a '>'");
		}
		else
		{
			advance(); // GT
		}
		getBuilder().restoreNewlinesState();
		return atGT;
	}

	/*
		 * functionParameters
		 *   : "(" functionParameter{","}? ")" // default values
		 *   ;
		 *
		 * functionParameter
		 *   : modifiers functionParameterRest
		 *   ;
		 *
		 * functionParameterRest
		 *   : parameter ("=" element)?
		 *   ;
		 */
	void parseCallParameterList(boolean isFunctionTypeContents, TokenSet recoverySet)
	{
		PsiBuilder.Marker parameters = mark();

		getBuilder().disableNewlines();
		expect(NapileTokens.LPAR, "Expecting '(", recoverySet);

		if(!parseIdeTemplate())
		{
			if(!at(NapileTokens.RPAR) && !atSet(recoverySet))
			{
				while(true)
				{
					if(at(NapileTokens.COMMA))
					{
						errorAndAdvance("Expecting a parameter declaration");
					}
					else if(at(NapileTokens.RPAR))
					{
						error("Expecting a parameter declaration");
						break;
					}

					if(isFunctionTypeContents)
					{
						if(!tryParseValueParameter())
						{
							PsiBuilder.Marker valueParameter = mark();
							parseModifierList(false); // lazy, out, ref
							parseTypeRef();
							valueParameter.done(CALL_PARAMETER_AS_VARIABLE);
						}
					}
					else
						parseValueParameter();

					if(!at(NapileTokens.COMMA))
						break;
					advance(); // COMMA
				}
			}
		}

		expect(NapileTokens.RPAR, "Expecting ')'", recoverySet);
		getBuilder().restoreNewlinesState();

		parameters.done(CALL_PARAMETER_LIST);
	}

	/*
		 * functionParameter
		 *   : modifiers "var"? parameter ("=" element)?
		 *   ;
		 */
	private boolean tryParseValueParameter()
	{
		return parseValueParameter(true);
	}

	private void parseValueParameter()
	{
		parseValueParameter(false);
	}

	private boolean parseValueParameter(boolean rollbackOnFailure)
	{
		PsiBuilder.Marker parameter = mark();

		boolean modifierList = parseModifierList(false);
		if(atSet(NapileTokens.VARIABLE_LIKE_KEYWORDS))
		{
			advance();

			if(!parseFunctionParameterRest() && rollbackOnFailure)
			{
				parameter.rollbackTo();
				return false;
			}

			parameter.done(CALL_PARAMETER_AS_VARIABLE);
		}
		else
		{
			if(modifierList)
			{
				error("'var' or 'val' expected");
				if(!parseFunctionParameterRest() && rollbackOnFailure)
				{
					parameter.rollbackTo();
					return false;
				}

				parameter.done(CALL_PARAMETER_AS_VARIABLE);
			}
			else if(at(NapileTokens.IDENTIFIER))
			{
				PsiBuilder.Marker refMark = mark();
				advance();
				refMark.done(REFERENCE_EXPRESSION);

				parseDefaultValueForCallParameter();

				parameter.done(CALL_PARAMETER_AS_REFERENCE);
			}
			else
			{
				error("'var', 'val' or identifier expected");
				parameter.rollbackTo();
				return false;
			}
		}

		return true;
	}

	/*
		 * functionParameterRest
		 *   : parameter ("=" element)?
		 *   ;
		 */
	private boolean parseFunctionParameterRest()
	{
		expect(NapileTokens.IDENTIFIER, "Parameter name expected", PARAMETER_NAME_RECOVERY_SET);

		if(at(NapileTokens.COLON))
		{
			advance(); // COLON
			parseTypeRef();
		}
		else
		{
			error("Parameters must have type annotation");
			return false;
		}

		parseDefaultValueForCallParameter();

		return true;
	}

	private void parseDefaultValueForCallParameter()
	{
		if(at(NapileTokens.EQ))
		{
			PsiBuilder.Marker mark = mark();

			advance();

			myExpressionParsing.parseExpression();

			mark.done(DEFAULT_VALUE_NODE);
		}
	}

	/*
		* "<#<" expression ">#>"
		*/
	boolean parseIdeTemplate()
	{
		@Nullable NapileNode nodeType = IDE_TEMPLATE_EXPRESSION;
		if(at(NapileTokens.IDE_TEMPLATE_START))
		{
			PsiBuilder.Marker mark = null;
			mark = mark();
			advance();
			expect(NapileTokens.IDENTIFIER, "Expecting identifier inside template");
			expect(NapileTokens.IDE_TEMPLATE_END, "Expecting IDE template end after identifier");
			mark.done(nodeType);
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	protected NapileParsing create(SemanticWhitespaceAwarePsiBuilder builder)
	{
		return createForTopLevel(builder);
	}

	public NapileExpressionParsing getExpressionParser()
	{
		return myExpressionParsing;
	}
}
