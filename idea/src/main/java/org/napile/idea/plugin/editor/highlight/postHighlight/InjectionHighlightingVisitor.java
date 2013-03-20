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

import org.napile.compiler.injection.CodeInjection;
import org.napile.compiler.lang.diagnostics.PositioningStrategies;
import org.napile.compiler.lang.psi.NapileInjectionExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.idea.plugin.IdeaInjectionSupport;
import org.napile.idea.plugin.editor.highlight.NapileHighlightingColors;
import org.napile.idea.plugin.highlighter.InjectionSyntaxHighlighter;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author VISTALL
 * @since 21:47/26.02.13
 */
public class InjectionHighlightingVisitor extends PostHighlightVisitor
{
	public InjectionHighlightingVisitor(BindingContext context, Collection<HighlightInfo> holder)
	{
		super(context, holder);
	}

	@Override
	public void visitElement(PsiElement element)
	{
		super.visitElement(element);

		NapileInjectionExpression exp = PsiTreeUtil.getParentOfType(element, NapileInjectionExpression.class);
		if(exp != null)
		{
			CodeInjection injection = exp.getCodeInjection();
			if(injection == null)
				return;

			InjectionSyntaxHighlighter syntaxHighlighter = IdeaInjectionSupport.SYNTAX_HIGHLIGHTER.getValue(injection);

			TextAttributesKey[] key = syntaxHighlighter.getTokenHighlights(element.getNode().getElementType());
			if(key.length > 0)
				highlightInfo(element, null, key[0]);
		}
	}

	@Override
	public void visitInjectionExpression(NapileInjectionExpression injectionExpression)
	{
		super.visitInjectionExpression(injectionExpression);

		CodeInjection codeInjection = injectionExpression.getCodeInjection();

		if(codeInjection != null)
		{
			highlightInfo(PositioningStrategies.INJECTION_NAME.mark(injectionExpression).get(0), null, NapileHighlightingColors.KEYWORD);

			IdeaInjectionSupport<?> ideaInjectionSupport = IdeaInjectionSupport.IDEA_SUPPORT.getValue(codeInjection);
			PsiElementVisitor elementVisitor = ideaInjectionSupport.createVisitorForHighlight(holder);
			if(elementVisitor != null)
				injectionExpression.acceptChildren(elementVisitor);
		}

		PsiElement blockElement = injectionExpression.getBlock();
		if(blockElement != null)
			highlightInfo(blockElement, null, NapileHighlightingColors.INJECTION_BLOCK);
	}
}
