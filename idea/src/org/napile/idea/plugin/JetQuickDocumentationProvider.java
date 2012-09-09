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

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileLabelExpression;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.plugin.JetLanguage;
import org.napile.idea.plugin.project.WholeProjectAnalyzerFacade;
import org.napile.compiler.resolve.DescriptorRenderer;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.lang.java.JavaDocumentationProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author abreslav
 * @author Evgeny Gerashchenko
 */
public class JetQuickDocumentationProvider extends AbstractDocumentationProvider
{
	private static String getText(PsiElement element, PsiElement originalElement, boolean mergeKotlinAndJava)
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
			BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((NapileFile) originalElement.getContainingFile()).getBindingContext();

			if(ref != null)
			{
				DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, ref);
				if(declarationDescriptor != null)
				{
					return render(declarationDescriptor, bindingContext, element, originalElement, mergeKotlinAndJava);
				}
				if(declarationPsiElement != null)
				{
					declarationPsiElement = BindingContextUtils.resolveToDeclarationPsiElement(bindingContext, ref);
				}
			}

			if(declarationPsiElement != null)
			{
				DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declarationPsiElement);
				if(declarationDescriptor != null)
				{
					return render(declarationDescriptor, bindingContext, element, originalElement, mergeKotlinAndJava);
				}
			}

			if(element instanceof NapileLabelExpression)
				return "<b>label</b> "+ ((NapileLabelExpression) element).getLabelName();

			return JetQuickDocumentationProvider.class.getName();
		}
		return null;
	}

	@Override
	public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement)
	{
		return getText(element, originalElement, true);
	}

	@Override
	public String generateDoc(PsiElement element, PsiElement originalElement)
	{
		return getText(element, originalElement, false);
	}

	private static String render(@NotNull DeclarationDescriptor declarationDescriptor, @NotNull BindingContext bindingContext, PsiElement element, PsiElement originalElement, boolean mergeKotlinAndJava)
	{
		String renderedDecl = DescriptorRenderer.HTML.render(declarationDescriptor);
		if(isKotlinDeclaration(declarationDescriptor, bindingContext))
		{
			return renderedDecl;
		}
		else
		{
			if(mergeKotlinAndJava)
			{
				return renderedDecl + "\nOriginal: " + new JavaDocumentationProvider().getQuickNavigateInfo(element, originalElement);
			}
			else
			{
				return null;
			}
		}
	}

	private static boolean isKotlinDeclaration(DeclarationDescriptor descriptor, BindingContext bindingContext)
	{
		PsiElement declaration = BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
		if(declaration == null)
			return false;
		if(JetLanguage.INSTANCE == declaration.getLanguage())
			return true;
		return false;
	}
}
