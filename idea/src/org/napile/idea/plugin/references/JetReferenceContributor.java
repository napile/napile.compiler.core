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

package org.napile.idea.plugin.references;

import static com.intellij.patterns.PlatformPatterns.psiElement;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileArrayAccessExpressionImpl;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;

/**
 * @author yole
 */
public class JetReferenceContributor extends PsiReferenceContributor
{
	@Override
	public void registerReferenceProviders(PsiReferenceRegistrar registrar)
	{
		registrar.registerReferenceProvider(psiElement(NapileSimpleNameExpression.class), new PsiReferenceProvider()
		{
			@NotNull
			@Override
			public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext processingContext)
			{
				return new PsiReference[]{
						new JetSimpleNameReference((NapileSimpleNameExpression) element)
				};
			}
		});

		registrar.registerReferenceProvider(psiElement(NapileArrayAccessExpressionImpl.class), new PsiReferenceProvider()
		{
			@NotNull
			@Override
			public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext processingContext)
			{
				return JetArrayAccessReference.create((NapileArrayAccessExpressionImpl) element);
			}
		});
	}
}
