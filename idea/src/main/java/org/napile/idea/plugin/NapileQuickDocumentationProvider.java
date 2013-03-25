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

package org.napile.idea.plugin;

import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileLabelExpression;
import org.napile.compiler.lang.psi.NapileNamedDeclaration;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTraceUtil;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.render.DescriptorRenderer;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import org.napile.idea.plugin.util.NapileDocUtil;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author abreslav
 * @author Evgeny Gerashchenko
 */
public class NapileQuickDocumentationProvider extends AbstractDocumentationProvider
{
	private static String getText(PsiElement element, PsiElement originalElement, boolean doc)
	{
		NapileReferenceExpression ref;
		if(originalElement instanceof NapileReferenceExpression)
		{
			ref = (NapileReferenceExpression) originalElement;
		}
		else
		{
			ref = PsiTreeUtil.getParentOfType(originalElement, NapileReferenceExpression.class);
		}
		PsiElement declarationPsiElement = PsiTreeUtil.getParentOfType(originalElement, NapileDeclaration.class);
		if(ref != null || declarationPsiElement != null)
		{
			BindingTrace bindingContext = ModuleAnalyzerUtil.lastAnalyze((NapileFile) originalElement.getContainingFile()).getBindingTrace();

			if(ref != null)
			{
				if(declarationPsiElement != null)
				{
					declarationPsiElement = BindingTraceUtil.resolveToDeclarationPsiElement(bindingContext, ref);
				}
			}

			if(declarationPsiElement != null)
			{
				DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingTraceKeys.DECLARATION_TO_DESCRIPTOR, declarationPsiElement);
				if(declarationDescriptor != null)
				{
					return renderDoc(declarationDescriptor, declarationPsiElement, doc);
				}
			}

			if(element instanceof NapileLabelExpression)
				return "<b>label</b> "+ ((NapileLabelExpression) element).getLabelName();

			return null;
		}
		return null;
	}

	private static String renderDoc(DeclarationDescriptor declarationDescriptor, PsiElement element, boolean doc)
	{
		if(doc && element instanceof NapileNamedDeclaration)
		{
			StringBuilder builder = new StringBuilder();
			builder.append(DescriptorRenderer.HTML.render(declarationDescriptor));

			builder.append("<br><br>").append(NapileDocUtil.render((NapileNamedDeclaration) element));
			return builder.toString();
		}
		else
		{
			return DescriptorRenderer.HTML.render(declarationDescriptor);
		}
	}

	@Override
	public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement)
	{
		return getText(element, originalElement, false);
	}

	@Override
	public String generateDoc(PsiElement element, PsiElement originalElement)
	{
		return getText(element, originalElement, true);
	}
}
