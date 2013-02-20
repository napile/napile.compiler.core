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
package org.napile.idea.plugin.highlighter;

import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileAnonymMethod;
import org.napile.compiler.lang.psi.NapileAnonymMethodExpression;
import org.napile.compiler.lang.psi.NapileMethodType;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;

class SoftKeywordsHighlightingVisitor extends HighlightingVisitor
{
	SoftKeywordsHighlightingVisitor(AnnotationHolder holder)
	{
		super(holder);
	}

	@Override
	public void visitElement(PsiElement element)
	{
		if(element instanceof LeafPsiElement)
		{
			IElementType elementType = ((LeafPsiElement) element).getElementType();
			if(NapileTokens.SOFT_KEYWORDS.contains(elementType))
				holder.createInfoAnnotation(element, null).setTextAttributes(NapileHighlightingColors.KEYWORD);

			if(NapileTokens.SAFE_ACCESS.equals(elementType))
				holder.createInfoAnnotation(element, null).setTextAttributes(NapileHighlightingColors.SAFE_ACCESS);
		}
	}

	@Override
	public void visitMethodType(NapileMethodType functionLiteral)
	{
		if(ApplicationManager.getApplication().isUnitTestMode())
			return;
		holder.createInfoAnnotation(functionLiteral.getOpenBraceNode(), null).setTextAttributes(NapileHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW);
		ASTNode closingBraceNode = functionLiteral.getClosingBraceNode();
		if(closingBraceNode != null)
		{
			holder.createInfoAnnotation(closingBraceNode, null).setTextAttributes(NapileHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW);
		}
		ASTNode arrowNode = functionLiteral.getArrowNode();
		if(arrowNode != null)
		{
			holder.createInfoAnnotation(arrowNode, null).setTextAttributes(NapileHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW);
		}
	}

	@Override
	public void visitAnonymMethodExpression(NapileAnonymMethodExpression expression)
	{
		if(ApplicationManager.getApplication().isUnitTestMode())
			return;
		NapileAnonymMethod functionLiteral = expression.getAnonymMethod();
		holder.createInfoAnnotation(functionLiteral.getOpenBraceNode(), null).setTextAttributes(NapileHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW);
		ASTNode closingBraceNode = functionLiteral.getClosingBraceNode();
		if(closingBraceNode != null)
		{
			holder.createInfoAnnotation(closingBraceNode, null).setTextAttributes(NapileHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW);
		}
		ASTNode arrowNode = functionLiteral.getArrowNode();
		if(arrowNode != null)
		{
			holder.createInfoAnnotation(arrowNode, null).setTextAttributes(NapileHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW);
		}
	}
}
