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
package org.napile.compiler.lang.lexer;

import org.napile.compiler.injection.CodeInjection;
import org.napile.compiler.lang.psi.NapileInjectionExpression;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import com.intellij.psi.tree.TokenSet;

public interface NapileTokens
{
	NapileToken EOF = new NapileToken("EOF");

	NapileToken BLOCK_COMMENT = new NapileToken("BLOCK_COMMENT");
	NapileToken DOC_COMMENT = new NapileToken("DOC_COMMENT");
	NapileToken EOL_COMMENT = new NapileToken("EOL_COMMENT");

	IElementType WHITE_SPACE = TokenType.WHITE_SPACE;

	NapileToken INTEGER_LITERAL = new NapileToken("INTEGER_LITERAL");
	NapileToken FLOAT_LITERAL = new NapileToken("FLOAT_CONSTANT");
	NapileToken CHARACTER_LITERAL = new NapileToken("CHARACTER_LITERAL");
	NapileToken STRING_LITERAL = new NapileToken("STRING_LITERAL");

	NapileKeywordToken PACKAGE_KEYWORD = NapileKeywordToken.keyword("package");
	NapileKeywordToken AS_KEYWORD = NapileKeywordToken.keyword("as");

	NapileKeywordToken CLASS_KEYWORD = NapileKeywordToken.keyword("class");
	NapileKeywordToken THIS_KEYWORD = NapileKeywordToken.keyword("this");
	NapileKeywordToken SUPER_KEYWORD = NapileKeywordToken.keyword("super");
	NapileKeywordToken VAR_KEYWORD = NapileKeywordToken.keyword("var");
	NapileKeywordToken VAL_KEYWORD = NapileKeywordToken.keyword("val");
	NapileKeywordToken METH_KEYWORD = NapileKeywordToken.keyword("meth");
	NapileKeywordToken MACRO_KEYWORD = NapileKeywordToken.keyword("macro");
	NapileKeywordToken FOR_KEYWORD = NapileKeywordToken.keyword("for");
	NapileKeywordToken NULL_KEYWORD = NapileKeywordToken.keyword("null");
	NapileKeywordToken TRUE_KEYWORD = NapileKeywordToken.keyword("true");
	NapileKeywordToken FALSE_KEYWORD = NapileKeywordToken.keyword("false");
	NapileKeywordToken IS_KEYWORD = NapileKeywordToken.keyword("is");
	NapileKeywordToken IN_KEYWORD = NapileKeywordToken.keyword("in");
	NapileKeywordToken THROW_KEYWORD = NapileKeywordToken.keyword("throw");
	NapileKeywordToken RETURN_KEYWORD = NapileKeywordToken.keyword("return");
	NapileKeywordToken BREAK_KEYWORD = NapileKeywordToken.keyword("break");
	NapileKeywordToken CONTINUE_KEYWORD = NapileKeywordToken.keyword("continue");
	NapileKeywordToken ANONYM_KEYWORD = NapileKeywordToken.keyword("anonym");
	NapileKeywordToken IF_KEYWORD = NapileKeywordToken.keyword("if");
	NapileKeywordToken TRY_KEYWORD = NapileKeywordToken.keyword("try");
	NapileKeywordToken ELSE_KEYWORD = NapileKeywordToken.keyword("else");
	NapileKeywordToken WHILE_KEYWORD = NapileKeywordToken.keyword("while");
	NapileKeywordToken DO_KEYWORD = NapileKeywordToken.keyword("do");
	NapileKeywordToken WHEN_KEYWORD = NapileKeywordToken.keyword("when");

	NapileToken AS_SAFE = NapileKeywordToken.keyword("AS_SAFE");//new NapileToken("as?");

	NapileToken IDENTIFIER = new NapileToken("IDENTIFIER");

	@Deprecated
	NapileToken FIELD_IDENTIFIER = new NapileToken("FIELD_IDENTIFIER");
	NapileToken LBRACKET = new NapileToken("LBRACKET");
	NapileToken RBRACKET = new NapileToken("RBRACKET");
	NapileToken LBRACE = new NapileToken("LBRACE");
	NapileToken RBRACE = new NapileToken("RBRACE");
	NapileToken LPAR = new NapileToken("LPAR");
	NapileToken RPAR = new NapileToken("RPAR");
	NapileToken DOT = new NapileToken("DOT");
	NapileToken PLUSPLUS = new NapileToken("PLUSPLUS");
	NapileToken MINUSMINUS = new NapileToken("MINUSMINUS");
	NapileToken MUL = new NapileToken("MUL");
	NapileToken PLUS = new NapileToken("PLUS");
	NapileToken MINUS = new NapileToken("MINUS");
	NapileToken EXCL = new NapileToken("EXCL");
	NapileToken DIV = new NapileToken("DIV");
	NapileToken PERC = new NapileToken("PERC");
	NapileToken LT = new NapileToken("LT");
	NapileToken GT = new NapileToken("GT");
	NapileToken LTEQ = new NapileToken("LTEQ");
	NapileToken GTEQ = new NapileToken("GTEQ");
	NapileToken ARROW = new NapileToken("ARROW");
	NapileToken EQEQ = new NapileToken("EQEQ");
	NapileToken EXCLEQ = new NapileToken("EXCLEQ");
	NapileToken EXCLEXCL = new NapileToken("EXCLEXCL");
	NapileToken ANDAND = new NapileToken("ANDAND");
	NapileToken OROR = new NapileToken("OROR");
	NapileToken SAFE_ACCESS = new NapileToken("SAFE_ACCESS");
	NapileToken ELVIS = new NapileToken("ELVIS");
	NapileToken QUEST = new NapileToken("QUEST");
	NapileToken COLON = new NapileToken("COLON");
	NapileToken AND = new NapileToken("AND");
	NapileToken SEMICOLON = new NapileToken("SEMICOLON");
	NapileToken RANGE = new NapileToken("RANGE");
	NapileToken EQ = new NapileToken("EQ");
	NapileToken MULTEQ = new NapileToken("MULTEQ");
	NapileToken DIVEQ = new NapileToken("DIVEQ");
	NapileToken PERCEQ = new NapileToken("PERCEQ");
	NapileToken PLUSEQ = new NapileToken("PLUSEQ");
	NapileToken MINUSEQ = new NapileToken("MINUSEQ");
	NapileToken NOT_IN = NapileKeywordToken.keyword("NOT_IN");
	NapileToken NOT_IS = NapileKeywordToken.keyword("NOT_IS");
	NapileToken HASH = new NapileToken("HASH");
	NapileToken AT = new NapileToken("AT");

	IElementType INJECTION_START = new NapileToken("INJECTION_START");

	IElementType INJECTION_BLOCK = new ILazyParseableElementType("INJECTION_BLOCK")
	{
		@Override
		protected Language getLanguageForParser(PsiElement psi)
		{
			if(psi instanceof PsiErrorElement)
				return psi.getLanguage();
			NapileInjectionExpression injectionExpression = (NapileInjectionExpression) psi;
			CodeInjection codeInjection = injectionExpression.getCodeInjection();
			if(codeInjection == null)
				return psi.getLanguage();
			else
				return codeInjection.getLanguage();
		}

		@Override
		public ASTNode createNode(final CharSequence text)
		{
			return new LazyParseablePsiElement(this, text);
		}
	};

	NapileToken IDE_TEMPLATE_START = new NapileToken("IDE_TEMPLATE_START");
	NapileToken IDE_TEMPLATE_END = new NapileToken("IDE_TEMPLATE_END");

	NapileToken COMMA = new NapileToken("COMMA");

	NapileToken EOL_OR_SEMICOLON = new NapileToken("EOL_OR_SEMICOLON");
	NapileKeywordToken IMPORT_KEYWORD = NapileKeywordToken.softKeyword("import");

	NapileKeywordToken LABEL_KEYWORD = NapileKeywordToken.softKeyword("label");

	NapileKeywordToken GET_KEYWORD = NapileKeywordToken.softKeyword("get");
	NapileKeywordToken SET_KEYWORD = NapileKeywordToken.softKeyword("set");

	// modifiers
	NapileKeywordToken LAZY_KEYWORD = NapileKeywordToken.softKeyword("lazy");
	NapileKeywordToken ABSTRACT_KEYWORD = NapileKeywordToken.softKeyword("abstract");
	NapileKeywordToken UTIL_KEYWORD = NapileKeywordToken.softKeyword("util");
	NapileKeywordToken OVERRIDE_KEYWORD = NapileKeywordToken.softKeyword("override");
	NapileKeywordToken STATIC_KEYWORD = NapileKeywordToken.softKeyword("static");
	NapileKeywordToken LOCAL_KEYWORD = NapileKeywordToken.softKeyword("local");
	NapileKeywordToken COVERED_KEYWORD = NapileKeywordToken.softKeyword("covered");
	NapileKeywordToken HERITABLE_KEYWORD = NapileKeywordToken.softKeyword("heritable");
	NapileKeywordToken CATCH_KEYWORD = NapileKeywordToken.softKeyword("catch");
	NapileKeywordToken VARARG_KEYWORD = NapileKeywordToken.softKeyword("vararg");
	NapileKeywordToken REIFIED_KEYWORD = NapileKeywordToken.softKeyword("reified");
	NapileKeywordToken FINAL_KEYWORD = NapileKeywordToken.softKeyword("final");
	NapileKeywordToken NATIVE_KEYWORD = NapileKeywordToken.softKeyword("native");

	NapileKeywordToken FINALLY_KEYWORD = NapileKeywordToken.softKeyword("finally");
	NapileKeywordToken CLASS_OF_KEYWORD = NapileKeywordToken.softKeyword("classOf");
	NapileKeywordToken TYPE_OF_KEYWORD = NapileKeywordToken.softKeyword("typeOf");
	NapileKeywordToken ARRAY_OF_KEYWORD = NapileKeywordToken.softKeyword("arrayOf");

	TokenSet KEYWORDS = TokenSet.create(PACKAGE_KEYWORD, AS_KEYWORD, CLASS_KEYWORD, THIS_KEYWORD, SUPER_KEYWORD, VAR_KEYWORD, VAL_KEYWORD, METH_KEYWORD, MACRO_KEYWORD, FOR_KEYWORD, NULL_KEYWORD, TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD, IN_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD, CONTINUE_KEYWORD, ANONYM_KEYWORD, IF_KEYWORD, ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD, TRY_KEYWORD, WHEN_KEYWORD, NOT_IN, NOT_IS, AS_SAFE);

	TokenSet SOFT_KEYWORDS = TokenSet.create(ARRAY_OF_KEYWORD, UTIL_KEYWORD, CLASS_OF_KEYWORD, TYPE_OF_KEYWORD, STATIC_KEYWORD, HERITABLE_KEYWORD, IMPORT_KEYWORD, GET_KEYWORD, SET_KEYWORD, LAZY_KEYWORD, ABSTRACT_KEYWORD, OVERRIDE_KEYWORD, LOCAL_KEYWORD, COVERED_KEYWORD, CATCH_KEYWORD, FINALLY_KEYWORD, FINAL_KEYWORD, VARARG_KEYWORD, REIFIED_KEYWORD, NATIVE_KEYWORD, LABEL_KEYWORD);

	TokenSet MODIFIER_KEYWORDS = TokenSet.create(ABSTRACT_KEYWORD, UTIL_KEYWORD, OVERRIDE_KEYWORD, LOCAL_KEYWORD, COVERED_KEYWORD, FINAL_KEYWORD, VARARG_KEYWORD, REIFIED_KEYWORD, HERITABLE_KEYWORD, STATIC_KEYWORD, NATIVE_KEYWORD, LAZY_KEYWORD);
	TokenSet VARIABLE_LIKE_KEYWORDS = TokenSet.create(VAR_KEYWORD, VAL_KEYWORD);
	TokenSet VARIABLE_ACCESS_KEYWORDS = TokenSet.create(SET_KEYWORD, GET_KEYWORD);
	TokenSet WHITE_SPACE_OR_COMMENT_BIT_SET = TokenSet.create(WHITE_SPACE, BLOCK_COMMENT, EOL_COMMENT, DOC_COMMENT);
	TokenSet WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE);
	TokenSet COMMENTS = TokenSet.create(EOL_COMMENT, BLOCK_COMMENT, DOC_COMMENT);

	TokenSet STRINGS = TokenSet.create(CHARACTER_LITERAL, STRING_LITERAL);
	TokenSet OPERATIONS = TokenSet.create(AS_KEYWORD, AS_SAFE, IS_KEYWORD, IN_KEYWORD, DOT, PLUSPLUS, MINUSMINUS, EXCLEXCL, MUL, PLUS, MINUS, EXCL, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQ, EXCLEQ, ANDAND, OROR, SAFE_ACCESS, ELVIS,
			COLON, RANGE, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ, NOT_IN, NOT_IS,
			IDENTIFIER);
}
