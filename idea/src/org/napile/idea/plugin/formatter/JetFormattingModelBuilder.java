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

package org.napile.idea.plugin.formatter;

import static org.napile.compiler.NapileNodeTypes.BINARY_EXPRESSION;
import static org.napile.compiler.NapileNodeTypes.BLOCK;
import static org.napile.compiler.NapileNodeTypes.CLASS;
import static org.napile.compiler.NapileNodeTypes.CLASS_BODY;
import static org.napile.compiler.NapileNodeTypes.METHOD;
import static org.napile.compiler.NapileNodeTypes.IMPORT_DIRECTIVE;
import static org.napile.compiler.NapileNodeTypes.NAMESPACE_HEADER;
import static org.napile.compiler.NapileNodeTypes.PROPERTY;
import static org.napile.compiler.NapileNodeTypes.TYPE_CONSTRAINT;
import static org.napile.compiler.NapileNodeTypes.TYPE_PARAMETER;
import static org.napile.compiler.NapileNodeTypes.VALUE_PARAMETER;
import static org.napile.compiler.lexer.JetTokens.ANDAND;
import static org.napile.compiler.lexer.JetTokens.COLON;
import static org.napile.compiler.lexer.JetTokens.COMMA;
import static org.napile.compiler.lexer.JetTokens.DIV;
import static org.napile.compiler.lexer.JetTokens.DIVEQ;
import static org.napile.compiler.lexer.JetTokens.EQ;
import static org.napile.compiler.lexer.JetTokens.EQEQ;
import static org.napile.compiler.lexer.JetTokens.EQEQEQ;
import static org.napile.compiler.lexer.JetTokens.EXCL;
import static org.napile.compiler.lexer.JetTokens.EXCLEQ;
import static org.napile.compiler.lexer.JetTokens.EXCLEQEQEQ;
import static org.napile.compiler.lexer.JetTokens.EXCLEXCL;
import static org.napile.compiler.lexer.JetTokens.GT;
import static org.napile.compiler.lexer.JetTokens.GTEQ;
import static org.napile.compiler.lexer.JetTokens.LBRACE;
import static org.napile.compiler.lexer.JetTokens.LT;
import static org.napile.compiler.lexer.JetTokens.LTEQ;
import static org.napile.compiler.lexer.JetTokens.MINUS;
import static org.napile.compiler.lexer.JetTokens.MINUSEQ;
import static org.napile.compiler.lexer.JetTokens.MINUSMINUS;
import static org.napile.compiler.lexer.JetTokens.MUL;
import static org.napile.compiler.lexer.JetTokens.MULTEQ;
import static org.napile.compiler.lexer.JetTokens.OROR;
import static org.napile.compiler.lexer.JetTokens.PERC;
import static org.napile.compiler.lexer.JetTokens.PERCEQ;
import static org.napile.compiler.lexer.JetTokens.PLUS;
import static org.napile.compiler.lexer.JetTokens.PLUSEQ;
import static org.napile.compiler.lexer.JetTokens.PLUSPLUS;
import static org.napile.compiler.lexer.JetTokens.RANGE;
import static org.napile.compiler.lexer.JetTokens.RBRACE;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.plugin.JetLanguage;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.formatting.FormattingModelProvider;
import com.intellij.formatting.Indent;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.TokenSet;

/**
 * @author yole
 */
public class JetFormattingModelBuilder implements FormattingModelBuilder
{
	@NotNull
	@Override
	public FormattingModel createModel(PsiElement element, CodeStyleSettings settings)
	{
		final JetBlock block = new JetBlock(element.getNode(), null, Indent.getNoneIndent(), null, settings, createSpacingBuilder(settings));

		return FormattingModelProvider.createFormattingModelForPsiFile(element.getContainingFile(), block, settings);
	}

	private static SpacingBuilder createSpacingBuilder(CodeStyleSettings settings)
	{
		JetCodeStyleSettings jetSettings = settings.getCustomSettings(JetCodeStyleSettings.class);
		CommonCodeStyleSettings jetCommonSettings = settings.getCommonSettings(JetLanguage.INSTANCE);

		return new SpacingBuilder(settings)
				// ============ Line breaks ==============
				.after(NAMESPACE_HEADER).blankLines(1)

				.between(IMPORT_DIRECTIVE, IMPORT_DIRECTIVE).lineBreakInCode().after(IMPORT_DIRECTIVE).blankLines(1)

				.before(METHOD).lineBreakInCode().before(PROPERTY).lineBreakInCode().between(METHOD, METHOD).blankLines(1).between(METHOD, PROPERTY).blankLines(1)

				.afterInside(LBRACE, BLOCK).lineBreakInCode().beforeInside(RBRACE, CLASS_BODY).lineBreakInCode().beforeInside(RBRACE, BLOCK).lineBreakInCode()

						// =============== Spacing ================
				.before(COMMA).spaceIf(jetCommonSettings.SPACE_BEFORE_COMMA).after(COMMA).spaceIf(jetCommonSettings.SPACE_AFTER_COMMA)

				.around(TokenSet.create(EQ, MULTEQ, DIVEQ, PLUSEQ, MINUSEQ, PERCEQ)).spaceIf(jetCommonSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS).around(TokenSet.create(ANDAND, OROR)).spaceIf(jetCommonSettings.SPACE_AROUND_LOGICAL_OPERATORS).around(TokenSet.create(EQEQ, EXCLEQ, EQEQEQ, EXCLEQEQEQ)).spaceIf(jetCommonSettings.SPACE_AROUND_EQUALITY_OPERATORS).aroundInside(TokenSet.create(LT, GT, LTEQ, GTEQ), BINARY_EXPRESSION).spaceIf(jetCommonSettings.SPACE_AROUND_RELATIONAL_OPERATORS).aroundInside(TokenSet.create(PLUS, MINUS), BINARY_EXPRESSION).spaceIf(jetCommonSettings.SPACE_AROUND_ADDITIVE_OPERATORS).aroundInside(TokenSet.create(MUL, DIV, PERC), BINARY_EXPRESSION).spaceIf(jetCommonSettings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS).around(TokenSet.create(PLUSPLUS, MINUSMINUS, EXCLEXCL, MINUS, PLUS, EXCL)).spaceIf(jetCommonSettings.SPACE_AROUND_UNARY_OPERATOR).around(RANGE).spaceIf(jetSettings.SPACE_AROUND_RANGE)

				.beforeInside(BLOCK, METHOD).spaceIf(jetCommonSettings.SPACE_BEFORE_METHOD_LBRACE)

						// TODO: Ask for better API
						// Type of the declaration colon
				.beforeInside(COLON, PROPERTY).spaceIf(jetSettings.SPACE_BEFORE_TYPE_COLON).afterInside(COLON, PROPERTY).spaceIf(jetSettings.SPACE_AFTER_TYPE_COLON).beforeInside(COLON, METHOD).spaceIf(jetSettings.SPACE_BEFORE_TYPE_COLON).afterInside(COLON, METHOD).spaceIf(jetSettings.SPACE_AFTER_TYPE_COLON).beforeInside(COLON, VALUE_PARAMETER).spaceIf(jetSettings.SPACE_BEFORE_TYPE_COLON).afterInside(COLON, VALUE_PARAMETER).spaceIf(jetSettings.SPACE_AFTER_TYPE_COLON)

						// Extends or constraint colon
				.beforeInside(COLON, TYPE_CONSTRAINT).spaceIf(jetSettings.SPACE_BEFORE_EXTEND_COLON).afterInside(COLON, TYPE_CONSTRAINT).spaceIf(jetSettings.SPACE_AFTER_EXTEND_COLON).beforeInside(COLON, CLASS).spaceIf(jetSettings.SPACE_BEFORE_EXTEND_COLON).afterInside(COLON, CLASS).spaceIf(jetSettings.SPACE_AFTER_EXTEND_COLON).beforeInside(COLON, TYPE_PARAMETER).spaceIf(jetSettings.SPACE_BEFORE_EXTEND_COLON).afterInside(COLON, TYPE_PARAMETER).spaceIf(jetSettings.SPACE_AFTER_EXTEND_COLON);
	}

	@Override
	public TextRange getRangeAffectingIndent(PsiFile psiFile, int i, ASTNode astNode)
	{
		return null;
	}
}
