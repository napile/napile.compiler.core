/*
 * Copyright 2010-2013 napile.org
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

package org.napile.idea.plugin.editor.highlight.postHighlight;

import java.util.Collection;

import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileAnonymMethod;
import org.napile.compiler.lang.psi.NapileAnonymMethodExpression;
import org.napile.compiler.lang.psi.NapileMethodType;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.idea.plugin.editor.highlight.NapileHighlightingColors;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @since 20:29/26.02.13
 */
public class SoftKeywordPostHighlightVisitor extends PostHighlightVisitor
{
	public SoftKeywordPostHighlightVisitor(BindingTrace context, Collection<HighlightInfo> holder)
	{
		super(context, holder);
	}

	@Override
	public void visitElement(PsiElement element)
	{
		if(element instanceof LeafPsiElement)
		{
			IElementType elementType = ((LeafPsiElement) element).getElementType();
			if(NapileTokens.SOFT_KEYWORDS.contains(elementType))
				highlightInfo(element, null, NapileHighlightingColors.KEYWORD);

			if(NapileTokens.SAFE_ACCESS.equals(elementType))
				highlightInfo(element, null, NapileHighlightingColors.SAFE_ACCESS);
		}
		super.visitElement(element);
	}

	@Override
	public void visitMethodType(NapileMethodType functionLiteral)
	{
		if(ApplicationManager.getApplication().isUnitTestMode())
			return;
		highlightInfo(functionLiteral.getOpenBraceNode(), null, NapileHighlightingColors.METHOD_LITERAL_BRACES_AND_ARROW);
		ASTNode closingBraceNode = functionLiteral.getClosingBraceNode();
		if(closingBraceNode != null)
		{
			highlightInfo(closingBraceNode, null, NapileHighlightingColors.METHOD_LITERAL_BRACES_AND_ARROW);
		}
		ASTNode arrowNode = functionLiteral.getArrowNode();
		if(arrowNode != null)
		{
			highlightInfo(arrowNode, null, NapileHighlightingColors.METHOD_LITERAL_BRACES_AND_ARROW);
		}
	}

	@Override
	public void visitAnonymMethodExpression(NapileAnonymMethodExpression expression)
	{
		if(ApplicationManager.getApplication().isUnitTestMode())
			return;
		NapileAnonymMethod functionLiteral = expression.getAnonymMethod();
		highlightInfo(functionLiteral.getOpenBraceNode(), null, NapileHighlightingColors.METHOD_LITERAL_BRACES_AND_ARROW);
		ASTNode closingBraceNode = functionLiteral.getClosingBraceNode();
		if(closingBraceNode != null)
		{
			highlightInfo(closingBraceNode, null, NapileHighlightingColors.METHOD_LITERAL_BRACES_AND_ARROW);
		}
		ASTNode arrowNode = functionLiteral.getArrowNode();
		if(arrowNode != null)
		{
			highlightInfo(arrowNode, null, NapileHighlightingColors.METHOD_LITERAL_BRACES_AND_ARROW);
		}
	}
}
