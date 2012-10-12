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

import static org.napile.compiler.NapileNodeTypes.*;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.napile.compiler.NapileNodeType;
import org.napile.compiler.lexer.NapileTokens;
import org.napile.compiler.lexer.NapileKeywordToken;
import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * @author max
 * @author abreslav
 */
public class JetParsing extends AbstractJetParsing
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

	public static final TokenSet CLASS_KEYWORDS = TokenSet.create(NapileTokens.CLASS_KEYWORD, NapileTokens.ENUM_KEYWORD, NapileTokens.RETELL_KEYWORD);
	private static final TokenSet ENUM_MEMBER_FIRST = TokenSet.create(NapileTokens.CLASS_KEYWORD, NapileTokens.METH_KEYWORD, NapileTokens.IDENTIFIER);

	private static final TokenSet CLASS_NAME_RECOVERY_SET = TokenSet.orSet(TokenSet.create(NapileTokens.LT, NapileTokens.LPAR, NapileTokens.COLON, NapileTokens.LBRACE), CLASS_KEYWORDS);
	public static final TokenSet TYPE_PARAMETER_GT_RECOVERY_SET = TokenSet.create(NapileTokens.LPAR, NapileTokens.COLON, NapileTokens.LBRACE, NapileTokens.GT);
	private static final TokenSet PARAMETER_NAME_RECOVERY_SET = TokenSet.create(NapileTokens.COLON, NapileTokens.EQ, NapileTokens.COMMA, NapileTokens.RPAR);
	private static final TokenSet NAMESPACE_NAME_RECOVERY_SET = TokenSet.create(NapileTokens.DOT, NapileTokens.EOL_OR_SEMICOLON);
	/*package*/ static final TokenSet TYPE_REF_FIRST = TokenSet.create(NapileTokens.LBRACKET, NapileTokens.IDENTIFIER, NapileTokens.METH_KEYWORD, NapileTokens.LPAR, NapileTokens.THIS_KEYWORD, NapileTokens.HASH);

	static JetParsing createForTopLevel(SemanticWhitespaceAwarePsiBuilder builder)
	{
		JetParsing jetParsing = new JetParsing(builder);
		jetParsing.myExpressionParsing = new JetExpressionParsing(builder, jetParsing);
		return jetParsing;
	}

	private static JetParsing createForByClause(final SemanticWhitespaceAwarePsiBuilder builder)
	{
		final SemanticWhitespaceAwarePsiBuilderForByClause builderForByClause = new SemanticWhitespaceAwarePsiBuilderForByClause(builder);
		JetParsing jetParsing = new JetParsing(builderForByClause);
		jetParsing.myExpressionParsing = new JetExpressionParsing(builderForByClause, jetParsing)
		{
			@Override
			protected boolean parseCallWithClosure()
			{
				if(builderForByClause.getStackSize() > 0)
				{
					return super.parseCallWithClosure();
				}
				return false;
			}

			@Override
			protected JetParsing create(SemanticWhitespaceAwarePsiBuilder builder)
			{
				return createForByClause(builder);
			}
		};
		return jetParsing;
	}

	private JetExpressionParsing myExpressionParsing;

	private JetParsing(SemanticWhitespaceAwarePsiBuilder builder)
	{
		super(builder);
	}

	/*
		 * [start] jetlFile
		 *   : preamble toplevelObject[| import]* [eof]
		 *   ;
		 */
	void parseFile()
	{
		PsiBuilder.Marker fileMarker = mark();

		parsePreamble();

		parseToplevelDeclarations(false);

		fileMarker.done(JET_FILE);
	}


	/*
		 * toplevelObject[| import]*
		 */
	private void parseToplevelDeclarations(boolean insideBlock)
	{
		while(!eof() && (!insideBlock || !at(NapileTokens.RBRACE)))
		{
			if(at(NapileTokens.IMPORT_KEYWORD))
			{
				parseImportDirective();
			}
			else
			{
				parseClassLike();
			}
		}
	}

	/*
		 *preamble
		 *  : namespaceHeader? import*
		 *  ;
		 */
	private void parsePreamble()
	{		/*
		 * namespaceHeader
         *   : modifiers "namespace" SimpleName{"."} SEMI?
         *   ;
         */
		PsiBuilder.Marker namespaceHeader = mark();
		PsiBuilder.Marker firstEntry = mark();
		parseModifierList(MODIFIER_LIST);

		if(at(NapileTokens.PACKAGE_KEYWORD))
		{
			advance(); // PACKAGE_KEYWORD


			parseNamespaceName();

			if(at(NapileTokens.LBRACE))
			{
				// Because it's blocked namespace and it will be parsed as one of top level objects
				firstEntry.rollbackTo();
				namespaceHeader.done(NAMESPACE_HEADER);
				return;
			}

			firstEntry.drop();

			consumeIf(NapileTokens.SEMICOLON);
		}
		else
		{
			firstEntry.rollbackTo();
		}
		namespaceHeader.done(NAMESPACE_HEADER);

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
		if(at(NapileTokens.PACKAGE_KEYWORD))
		{
			advance(); // PACKAGE_KEYWORD
			expect(NapileTokens.DOT, "Expecting '.'", TokenSet.create(NapileTokens.IDENTIFIER, NapileTokens.MUL, NapileTokens.SEMICOLON));
		}

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
		// TODO: Duplicate with parsing imports in parseToplevelDeclarations
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
		 * toplevelObject
		 *   : namespace
		 *   : class
		 *   : extension
		 *   : function
		 *   : property
		 *   : typedef
		 *   : object
		 *   ;
		 */
	private void parseClassLike()
	{
		PsiBuilder.Marker decl = mark();

		parseModifierList(MODIFIER_LIST);

		IElementType keywordToken = tt();
		IElementType declType = null;

		if(CLASS_KEYWORDS.contains(keywordToken))
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


	/**
	 * (modifier | attribute)*
	 * <p/>
	 * Feeds modifiers (not attributes) into the passed consumer, if it is not null
	 */
	boolean parseModifierList(NapileNodeType nodeType)
	{
		PsiBuilder.Marker list = mark();
		boolean empty = true;
		while(!eof())
		{
			if(atSet(NapileTokens.MODIFIER_KEYWORDS))
			{
				advance(); // MODIFIER
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
		if(empty)
		{
			list.drop();
		}
		else
		{
			list.done(nodeType);
		}
		return !empty;
	}


	/*
		 * annotation
		 *   : annotationEntry+
		 *   ;
		 */
	public boolean parseAnnotations()
	{
		PsiBuilder.Marker annotation = mark();
		getBuilder().disableNewlines();

		while(at(NapileTokens.AT))
			parseAnnotationEntry();

		getBuilder().restoreNewlinesState();

		annotation.done(ANNOTATION_LIST);
		return true;
	}

	/*
		 * annotationEntry
		 *   : "@" SimpleName{"."} typeArguments? valueArguments?
		 *   ;
		 */
	private void parseAnnotationEntry()
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
		{
			myExpressionParsing.parseValueArgumentList();
		}
		attribute.done(ANNOTATION_ENTRY);
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
		IElementType lastKeyword = tt();

		advance(); // CLASS_KEYWORD

		if(!parseIdeTemplate())
			expect(NapileTokens.IDENTIFIER, "Class name expected", CLASS_NAME_RECOVERY_SET);
		parseTypeParameterList();

		if(at(NapileTokens.COLON))
		{
			advance(); // COLON
			parseTypeExtendList();
		}

		if(at(NapileTokens.LBRACE))
		{
			if(lastKeyword == NapileTokens.ENUM_KEYWORD)
				parseEnumBody();
			else if(lastKeyword == NapileTokens.RETELL_KEYWORD)
				parseRetellClassBody();
			else
				parseClassBody();
		}

		return CLASS;
	}

	/*
		 * enumClassBody
		 *   : "{" enumEntry* "}"
		 *   ;
		 */
	private void parseEnumBody()
	{
		if(!at(NapileTokens.LBRACE))
			return;

		PsiBuilder.Marker classBody = mark();

		getBuilder().enableNewlines();
		advance(); // LBRACE

		if(!parseIdeTemplate())
		{
			while(!eof() && !at(NapileTokens.RBRACE))
			{
				PsiBuilder.Marker entryOrMember = mark();

				TokenSet constructorNameFollow = TokenSet.create(NapileTokens.SEMICOLON, NapileTokens.COLON, NapileTokens.LPAR, NapileTokens.LT, NapileTokens.LBRACE);
				int lastId = findLastBefore(ENUM_MEMBER_FIRST, constructorNameFollow, false);

				createTruncatedBuilder(lastId).parseModifierList(MODIFIER_LIST);

				IElementType type;
				if(at(NapileTokens.IDENTIFIER))
				{
					parseEnumEntry();
					type = ENUM_ENTRY;
				}
				else
				{
					type = parseMemberDeclarationRest();
				}

				if(type == null)
				{
					errorAndAdvance("Expecting an enum entry or member declaration");
					entryOrMember.drop();
				}
				else
				{
					entryOrMember.done(type);
				}
			}
		}

		expect(NapileTokens.RBRACE, "Expecting '}' to close enum body");
		getBuilder().restoreNewlinesState();

		classBody.done(CLASS_BODY);
	}

	/*
		 * enumEntry
		 *   : typeParameters? valueArguments? typeConstraints classBody?
		 *   ;
		 */
	private void parseEnumEntry()
	{
		assert _at(NapileTokens.IDENTIFIER);

		advance();

		parseTypeArgumentList();
		if(at(NapileTokens.LPAR))
		{
			PsiBuilder.Marker callExpression = mark();

			myExpressionParsing.parseValueArgumentList();

			callExpression.done(CONSTRUCTOR_CALLEE);
		}

		if(at(NapileTokens.LBRACE))
			parseClassBody();

		consumeIf(NapileTokens.SEMICOLON);
	}

	private void parseRetellClassBody()
	{
		if(!at(NapileTokens.LBRACE))
			return;

		PsiBuilder.Marker classBody = mark();

		getBuilder().enableNewlines();
		advance(); // LBRACE

		if(!parseIdeTemplate())
		{
			while(!eof() && !at(NapileTokens.RBRACE))
			{
				PsiBuilder.Marker entryMarker = mark();

				if(at(NapileTokens.IDENTIFIER))
				{
					parseRetellEntry();
					entryMarker.done(RETELL_ENTRY);
				}
				else
				{
					errorAndAdvance("Expecting identifier");
					entryMarker.drop();
				}
			}
		}

		expect(NapileTokens.RBRACE, "Expecting '}' to close retell body");
		getBuilder().restoreNewlinesState();

		classBody.done(CLASS_BODY);
	}

	private void parseRetellEntry()
	{
		assert _at(NapileTokens.IDENTIFIER);

		advance();

		if(at(NapileTokens.EQ))
		{
			advance();

			myExpressionParsing.parseExpression();
		}
		else
			error("'=' Expected");

		consumeIf(NapileTokens.SEMICOLON);
	}

	/*
		 * classBody
		 *   : ("{" memberDeclaration "}")?
		 *   ;
		 */	/*package*/ void parseClassBody()
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
				parseMemberDeclaration();
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
	private void parseMemberDeclaration()
	{
		PsiBuilder.Marker decl = mark();

		IElementType declType = null;
		// ugly
		if(at(NapileTokens.STATIC_KEYWORD) && lookahead(1) == NapileTokens.LBRACE)
			declType = parseStaticConstructor();
		else
		{
			parseModifierList(MODIFIER_LIST);

			declType = parseMemberDeclarationRest();
		}

		if(declType == null)
		{
			errorWithRecovery("Expecting member declaration", TokenSet.create(NapileTokens.RBRACE));
			decl.drop();
		}
		else
			decl.done(declType);
	}

	private IElementType parseMemberDeclarationRest()
	{
		IElementType keywordToken = tt();
		IElementType declType = null;
		if(CLASS_KEYWORDS.contains(keywordToken))
			declType = parseClass();
		else if(keywordToken == NapileTokens.METH_KEYWORD)
			declType = parseMethod();
		else if(keywordToken == NapileTokens.THIS_KEYWORD)
			declType = parseConstructor();
		else if(keywordToken == NapileTokens.VAR_KEYWORD)
			declType = parseProperty();

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

	/*
		 * initializer{","}
		 */
	private void parseInitializerList()
	{
		PsiBuilder.Marker list = mark();
		while(true)
		{
			if(at(NapileTokens.COMMA))
				errorAndAdvance("Expecting a this or super constructor call");
			parseInitializer();
			if(!at(NapileTokens.COMMA))
				break;
			advance(); // COMMA
		}
		list.done(INITIALIZER_LIST);
	}

	/*
		 * initializer
		 *   : attributes "this" valueArguments
		 *   : attributes constructorInvocation // type parameters may (must?) be omitted
		 *   ;
		 */
	private void parseInitializer()
	{
		PsiBuilder.Marker initializer = mark();
		parseAnnotations();

		IElementType type;
		if(at(NapileTokens.THIS_KEYWORD))
		{
			PsiBuilder.Marker mark = mark();
			advance(); // THIS_KEYWORD
			mark.done(THIS_CONSTRUCTOR_REFERENCE);
			type = THIS_CALL;
		}
		else if(atSet(TYPE_REF_FIRST))
		{
			PsiBuilder.Marker reference = mark();
			parseTypeRef();
			reference.done(CONSTRUCTOR_CALLEE);
			type = DELEGATOR_SUPER_CALL;
		}
		else
		{
			errorWithRecovery("Expecting constructor call (this(...)) or supertype initializer", TokenSet.create(NapileTokens.LBRACE, NapileTokens.COMMA));
			initializer.drop();
			return;
		}
		myExpressionParsing.parseValueArgumentList();

		initializer.done(type);
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
	private IElementType parseProperty()
	{
		return parseProperty(false);
	}

	IElementType parseProperty(boolean local)
	{
		if(at(NapileTokens.VAR_KEYWORD))
			advance(); // VAR_KEYWORD
		else
			errorAndAdvance("Expecting 'var'");

		parseTypeParameterList();

		getBuilder().disableJoiningComplexTokens();

		if(!parseIdeTemplate())
			expect(NapileTokens.IDENTIFIER, "Expecting identifier");

		getBuilder().restoreJoiningComplexTokensState();

		if(at(NapileTokens.COLON))
		{
			advance(); // COLON
			if(!parseIdeTemplate())
			{
				parseTypeRef();
			}
		}

		if(at(NapileTokens.EQ))
		{
			advance(); // EQ
			myExpressionParsing.parseExpression();
			consumeIf(NapileTokens.SEMICOLON);
		}

		return PROPERTY;
	}


	/*
		 * function
		 *   : modifiers "meth" typeParameters?
		 *       (type "." | attributes)?
		 *       SimpleName
		 *       typeParameters? functionParameters (":" type)?
		 *       typeConstraints
		 *       functionBody?
		 *   ;
		 */
	IElementType parseMethod()
	{
		assert _at(NapileTokens.METH_KEYWORD);

		advance(); // METH_KEYWORD

		// Recovery for the case of class A { fun| }
		if(at(NapileTokens.RBRACE))
		{
			error("Method body expected");
			return METHOD;
		}

		if(!parseIdeTemplate())
		{
			PsiBuilder.Marker marker = mark();
			if(at(NapileTokens.IDENTIFIER))
				advance();

			if(at(NapileTokens.DOT))
			{
				marker.done(VARIABLE_REFERENCE);

				advance();

				if(at(NapileTokens.SET_KEYWORD) || at(NapileTokens.GET_KEYWORD))
					advance();
				else
					error("Expected 'set' or 'get'");
			}
			else
				marker.drop();
		}

		TokenSet valueParametersFollow = TokenSet.create(NapileTokens.COLON, NapileTokens.EQ, NapileTokens.LBRACE, NapileTokens.SEMICOLON, NapileTokens.RPAR);

		parseTypeParameterList();

		parseValueParameterList(false, valueParametersFollow);

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
			parseFunctionBody();
		}

		return METHOD;
	}

	private NapileNodeType parseStaticConstructor()
	{
		assert _at(NapileTokens.STATIC_KEYWORD);

		advance(); // STATIC_KEYWORD

		parseBlock();

		return STATIC_CONSTRUCTOR;
	}

	/*
		 * constructor
		 *   : modifiers "this" functionParameters (":" initializer{","}) block?
		 *   ;
		 */
	private NapileNodeType parseConstructor()
	{
		assert _at(NapileTokens.THIS_KEYWORD);

		advance(); // THIS_KEYWORD

		parseValueParameterList(false, TokenSet.create(NapileTokens.COLON, NapileTokens.LBRACE, NapileTokens.SEMICOLON));

		if(at(NapileTokens.COLON))
		{
			advance(); // COLON

			parseDelegationSpecifierList();
			//parseInitializerList();
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
	private void parseFunctionBody()
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
		 * delegationSpecifier{","}
		 */
	void parseDelegationSpecifierList()
	{
		PsiBuilder.Marker list = mark();

		while(true)
		{
			if(at(NapileTokens.COMMA))
			{
				errorAndAdvance("Expecting a delegation specifier");
				continue;
			}
			parseDelegationSpecifier();
			if(!at(NapileTokens.COMMA))
				break;
			advance(); // COMMA
		}

		list.done(DELEGATION_SPECIFIER_LIST);
	}

	void parseTypeExtendList()
	{
		PsiBuilder.Marker list = mark();

		while(true)
		{
			if(at(NapileTokens.COMMA))
			{
				errorAndAdvance("Expecting a type");
				continue;
			}
			parseTypeRef();
			if(!at(NapileTokens.COMMA))
				break;
			advance(); // COMMA
		}

		list.done(EXTEND_TYPE_LIST);
	}

	/*
		 * attributes delegationSpecifier
		 *
		 * delegationSpecifier
		 *   : constructorInvocation // type and constructor arguments
		 *   : userType
		 *   : explicitDelegation
		 *   ;
		 *
		 */
	private void parseDelegationSpecifier()
	{
		PsiBuilder.Marker delegator = mark();
		parseAnnotations();

		PsiBuilder.Marker reference = mark();
		parseTypeRef();

		if(at(NapileTokens.LPAR))
		{
			reference.done(CONSTRUCTOR_CALLEE);
			myExpressionParsing.parseValueArgumentList();
			delegator.done(DELEGATOR_SUPER_CALL);
		}
		else
		{
			reference.drop();
			delegator.done(DELEGATOR_SUPER_CLASS);
		}
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

		parseModifierList(MODIFIER_LIST);

		expect(NapileTokens.IDENTIFIER, "Type parameter name expected", TokenSet.EMPTY);

		if(at(NapileTokens.COLON))
		{
			advance(); // COLON

			parseTypeRef();
		}
		else if(at(NapileTokens.LBRACKET))
		{
			advance(); // LBRACKET

			while(true)
			{
				if(at(NapileTokens.COMMA))
					errorAndAdvance("Expecting type declaration");

				parseTypeRef();

				if(!at(NapileTokens.COMMA))
					break;

				advance(); // COMMA
			}

			expect(NapileTokens.RBRACKET, "Missing ']'");
		}

		while(at(NapileTokens.LPAR))
			parseValueParameterList(true, TokenSet.EMPTY);

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
	void parseTypeRef()
	{
		parseTypeRef(TokenSet.EMPTY);
	}

	void parseTypeRef(TokenSet extraRecoverySet)
	{
		PsiBuilder.Marker typeRefMarker = parseTypeRefContents(extraRecoverySet);
		typeRefMarker.done(TYPE_REFERENCE);
	}

	// The extraRecoverySet is needed for the foo(bar<x, 1, y>(z)) case, to tell whether we should stop
	// on expression-indicating symbols or not
	private PsiBuilder.Marker parseTypeRefContents(TokenSet extraRecoverySet)
	{
		// Disabling token merge is required for cases like
		//    Int?.(Foo) -> Bar
		// we don't support this case now
		//        myBuilder.disableJoiningComplexTokens();
		PsiBuilder.Marker typeRefMarker = mark();
		parseAnnotations();

		if(at(NapileTokens.IDENTIFIER) || at(NapileTokens.PACKAGE_KEYWORD))
		{
			parseUserType();
		}
		else if(at(NapileTokens.LPAR))
		{
			PsiBuilder.Marker functionOrParenthesizedType = mark();

			// This may be a function parameter list or just a prenthesized type
			advance(); // LPAR
			parseTypeRefContents(TokenSet.EMPTY).drop(); // parenthesized types, no reference element around it is needed

			if(at(NapileTokens.RPAR))
			{
				advance(); // RPAR
				if(at(NapileTokens.ARROW))
				{
					// It's a function type with one parameter specified
					//    (A) -> B
					functionOrParenthesizedType.rollbackTo();
					parseFunctionType();
				}
				else
				{
					// It's a parenthesized type
					//    (A)
					functionOrParenthesizedType.drop();
				}
			}
			else
			{
				// This must be a function type
				//   (A, B) -> C
				// or
				//   (a : A) -> C
				functionOrParenthesizedType.rollbackTo();
				parseFunctionType();
			}
		}
		else if(at(NapileTokens.THIS_KEYWORD))
		{
			parseSelfType();
		}
		else
		{
			errorWithRecovery("Type expected", TokenSet.orSet(CLASS_KEYWORDS, TokenSet.create(NapileTokens.EQ, NapileTokens.COMMA, NapileTokens.GT, NapileTokens.RBRACKET, NapileTokens.DOT, NapileTokens.RPAR, NapileTokens.RBRACE, NapileTokens.LBRACE, NapileTokens.SEMICOLON), extraRecoverySet));
		}

		while(at(NapileTokens.QUEST))
		{
			PsiBuilder.Marker precede = typeRefMarker.precede();

			advance(); // QUEST
			typeRefMarker.done(NULLABLE_TYPE);

			typeRefMarker = precede;
		}

		if(at(NapileTokens.DOT))
		{
			// This is a receiver for a function type
			//  A.(B) -> C
			//   ^

			PsiBuilder.Marker precede = typeRefMarker.precede();
			typeRefMarker.done(TYPE_REFERENCE);

			advance(); // DOT

			if(at(NapileTokens.LPAR))
			{
				parseFunctionTypeContents().drop();
			}
			else
			{
				error("Expecting function type");
			}
			typeRefMarker = precede.precede();

			precede.done(FUNCTION_TYPE);
		}
		//        myBuilder.restoreJoiningComplexTokensState();
		return typeRefMarker;
	}

	/*
		 * userType
		 *   : ("namespace" ".")? simpleUserType{"."}
		 *   ;
		 */
	private void parseUserType()
	{
		PsiBuilder.Marker userType = mark();

		if(at(NapileTokens.PACKAGE_KEYWORD))
		{
			advance(); // PACKAGE_KEYWORD
			expect(NapileTokens.DOT, "Expecting '.'", TokenSet.create(NapileTokens.IDENTIFIER));
		}

		PsiBuilder.Marker reference = mark();
		while(true)
		{
			if(expect(NapileTokens.IDENTIFIER, "Expecting type name", TokenSet.orSet(JetExpressionParsing.EXPRESSION_FIRST, JetExpressionParsing.EXPRESSION_FOLLOW)))
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
			if(lookahead(1) == NapileTokens.LPAR)
			{
				// This may be a receiver for a function type
				//   Int.(Int) -> Int
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
		 *   : "This"
		 *   ;
		 */
	private void parseSelfType()
	{
		assert _at(NapileTokens.THIS_KEYWORD);

		PsiBuilder.Marker type = mark();
		advance(); // THIS_KEYWORD
		type.done(SELF_TYPE);
	}

	/*
		 *  (optionalProjection type){","}
		 */
	private PsiBuilder.Marker parseTypeArgumentList()
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
		 * functionType
		 *   : (type ".")? "(" (parameter | modifiers type){","}? ")" "->" type?
		 *   ;
		 */
	private void parseFunctionType()
	{
		parseFunctionTypeContents().done(FUNCTION_TYPE);
	}

	private PsiBuilder.Marker parseFunctionTypeContents()
	{
		assert _at(NapileTokens.LPAR) : tt();
		PsiBuilder.Marker functionType = mark();

		//        advance(); // LPAR
		//
		//        int lastLPar = findLastBefore(TokenSet.create(LPAR), TokenSet.create(COLON), false);
		//        if (lastLPar >= 0 && lastLPar > myBuilder.getCurrentOffset()) {
		//            TODO : -1 is a hack?
		//            createTruncatedBuilder(lastLPar - 1).parseTypeRef();
		//            advance(); // DOT
		//        }

		parseValueParameterList(true, TokenSet.EMPTY);

		//        if (at(COLON)) {
		//            advance(); // COLON // expect(COLON, "Expecting ':' followed by a return type", TYPE_REF_FIRST);

		expect(NapileTokens.ARROW, "Expecting '->' to specify return type of a function type", TYPE_REF_FIRST);
		parseTypeRef();
		//        }

		return functionType;//.done(FUNCTION_TYPE);
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
	void parseValueParameterList(boolean isFunctionTypeContents, TokenSet recoverySet)
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
							parseModifierList(MODIFIER_LIST); // lazy, out, ref
							parseTypeRef();
							valueParameter.done(VALUE_PARAMETER);
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

		parameters.done(VALUE_PARAMETER_LIST);
	}

	/*
		 * functionParameter
		 *   : modifiers ("val" | "var")? parameter ("=" element)?
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

		if(at(NapileTokens.IDENTIFIER) && (lookahead(1) == NapileTokens.COMMA || lookahead(1) == NapileTokens.RPAR))
		{
			PsiBuilder.Marker refMark = mark();
			advance();
			refMark.done(REFERENCE_EXPRESSION);

			parameter.done(REFERENCE_PARAMETER);
		}
		else
		{
			parseModifierList(MODIFIER_LIST);

			if(at(NapileTokens.VAR_KEYWORD))
			{
				advance(); // VAR_KEYWORD | VAL_KEYWORD
			}

			if(!parseFunctionParameterRest() && rollbackOnFailure)
			{
				parameter.rollbackTo();
				return false;
			}

			parameter.done(VALUE_PARAMETER);
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

		if(at(NapileTokens.EQ))
		{
			advance(); // EQ
			myExpressionParsing.parseExpression();
		}
		return true;
	}

	/*
		* "<#<" expression ">#>"
		*/
	boolean parseIdeTemplate()
	{
		@Nullable NapileNodeType nodeType = IDE_TEMPLATE_EXPRESSION;
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
	protected JetParsing create(SemanticWhitespaceAwarePsiBuilder builder)
	{
		return createForTopLevel(builder);
	}
}
