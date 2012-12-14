/*
 * Copyright 2010-2012 napile.org
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

package org.napile.idea.plugin.highlighter;

import org.napile.compiler.injection.CodeInjection;
import org.napile.compiler.lang.diagnostics.PositioningStrategies;
import org.napile.compiler.lang.psi.NapileInjectionExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.idea.plugin.IdeaInjectionSupport;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author VISTALL
 * @date 14:32/27.10.12
 */
public class InjectionHighlightingVisitor extends AfterAnalysisHighlightingVisitor
{
	protected InjectionHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext)
	{
		super(holder, bindingContext);
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
				holder.createInfoAnnotation(element, null).setTextAttributes(key[0]);
		}
	}

	@Override
	public void visitInjectionExpression(NapileInjectionExpression injectionExpression)
	{
		super.visitInjectionExpression(injectionExpression);

		CodeInjection codeInjection = injectionExpression.getCodeInjection();

		if(codeInjection != null)
		{
			holder.createInfoAnnotation(PositioningStrategies.INJECTION_NAME.mark(injectionExpression).get(0), null).setTextAttributes(NapileHighlightingColors.KEYWORD);

			IdeaInjectionSupport<?> ideaInjectionSupport = IdeaInjectionSupport.IDEA_SUPPORT.getValue(codeInjection);
			PsiElementVisitor elementVisitor = ideaInjectionSupport.createVisitorForAnnotator(holder);
			if(elementVisitor != null)
				injectionExpression.acceptChildren(elementVisitor);
		}

		PsiElement blockElement = injectionExpression.getBlock();
		if(blockElement != null)
			holder.createInfoAnnotation(blockElement, null).setTextAttributes(NapileHighlightingColors.INJECTION_BLOCK);
	}
}
