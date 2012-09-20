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

package org.napile.idea.plugin.completion;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.cli.jvm.compiler.TipsManager;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptorWithVisibility;
import org.napile.compiler.lang.descriptors.NamespaceDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.Visibilities;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.NapileQualifiedExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lexer.JetTokens;
import org.napile.idea.plugin.completion.weigher.JetCompletionSorting;
import org.napile.idea.plugin.project.WholeProjectAnalyzerFacade;
import org.napile.idea.plugin.references.JetSimpleNameReference;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;

/**
 * @author Nikolay Krasko
 */
public class JetCompletionContributor extends CompletionContributor
{

	private static class CompletionSession
	{
		public boolean isSomethingAdded = false;
		public int customInvocationCount = 0;

		@Nullable
		public DeclarationDescriptor inDescriptor = null;
	}

	public JetCompletionContributor()
	{
		extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>()
		{
			@Override
			protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result)
			{
				result.restartCompletionWhenNothingMatches();
				result = JetCompletionSorting.addJetSorting(parameters, result);

				CompletionSession session = new CompletionSession();
				session.customInvocationCount = parameters.getInvocationCount();

				PsiElement position = parameters.getPosition();
				if(!(position.getContainingFile() instanceof NapileFile))
				{
					return;
				}

				JetSimpleNameReference jetReference = getJetReference(parameters);
				if(jetReference != null)
				{

					BindingContext jetContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((NapileFile) position.getContainingFile()).getBindingContext();

					JetScope scope = jetContext.get(BindingContext.RESOLUTION_SCOPE, jetReference.getExpression());
					session.inDescriptor = scope != null ? scope.getContainingDeclaration() : null;

					completeForReference(parameters, result, position, jetReference, session);

					if(!session.isSomethingAdded && session.customInvocationCount == 0)
					{
						// Rerun completion if nothing was found
						session.customInvocationCount = 1;
						completeForReference(parameters, result, position, jetReference, session);
					}
				}

				// Prevent from adding reference variants from standard reference contributor
				result.stopHere();
			}
		});
	}

	private static void completeForReference(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result, @NotNull PsiElement position, @NotNull JetSimpleNameReference jetReference, @NotNull CompletionSession session)
	{
		if(isOnlyKeywordCompletion(position))
		{
			return;
		}

		if(shouldRunTypeCompletionOnly(position, jetReference))
		{
			if(session.customInvocationCount > 0)
			{

			}
			else
			{
				for(LookupElement variant : getReferenceVariants(jetReference, result, session))
				{
					if(isTypeDeclaration(variant))
					{
						addCheckedCompletionToResult(result, variant, session);
					}
				}
			}

			return;
		}

		for(LookupElement variant : getReferenceVariants(jetReference, result, session))
			addCheckedCompletionToResult(result, variant, session);
	}

	private static boolean isOnlyKeywordCompletion(PsiElement position)
	{
		return PsiTreeUtil.getParentOfType(position, NapileModifierList.class) != null;
	}


	public static boolean isTypeDeclaration(@NotNull Object variant)
	{
		if(variant instanceof LookupElement)
		{
			Object object = ((LookupElement) variant).getObject();
			if(object instanceof JetLookupObject)
			{
				DeclarationDescriptor descriptor = ((JetLookupObject) object).getDescriptor();
				return (descriptor instanceof ClassDescriptor) ||
						(descriptor instanceof NamespaceDescriptor) ||
						(descriptor instanceof TypeParameterDescriptor);
			}
		}

		return false;
	}



	private static boolean shouldRunTypeCompletionOnly(PsiElement position, JetSimpleNameReference jetReference)
	{
		// Check that completion in the type annotation context and if there's a qualified
		// expression we are at first of it
		NapileTypeReference typeReference = PsiTreeUtil.getParentOfType(position, NapileTypeReference.class);
		if(typeReference != null)
		{
			NapileSimpleNameExpression firstPartReference = PsiTreeUtil.findChildOfType(typeReference, NapileSimpleNameExpression.class);
			return firstPartReference == jetReference.getExpression();
		}

		return false;
	}

	private static boolean shouldRunTopLevelCompletion(@NotNull CompletionParameters parameters, CompletionSession session)
	{
		if(session.customInvocationCount == 0)
		{
			return false;
		}

		PsiElement element = parameters.getPosition();
		if(element.getNode().getElementType() == JetTokens.IDENTIFIER)
		{
			if(element.getParent() instanceof NapileSimpleNameExpression)
			{
				NapileSimpleNameExpression nameExpression = (NapileSimpleNameExpression) element.getParent();

				// Top level completion should be executed for simple which is not in qualified expression
				return (PsiTreeUtil.getParentOfType(nameExpression, NapileQualifiedExpression.class) == null);
			}
		}

		return false;
	}

	private static boolean shouldRunExtensionsCompletion(CompletionParameters parameters, String prefix, CompletionSession session)
	{
		if(session.customInvocationCount == 0 && prefix.length() < 3)
		{
			return false;
		}

		return getJetReference(parameters) != null;
	}


	@Nullable
	private static JetSimpleNameReference getJetReference(@NotNull CompletionParameters parameters)
	{
		PsiElement element = parameters.getPosition();
		if(element.getParent() != null)
		{
			PsiElement parent = element.getParent();
			PsiReference[] references = parent.getReferences();

			if(references.length != 0)
			{
				for(PsiReference reference : references)
				{
					if(reference instanceof JetSimpleNameReference)
					{
						return (JetSimpleNameReference) reference;
					}
				}
			}
		}

		return null;
	}

	private static void addCompletionToResult(@NotNull CompletionResultSet result, @NotNull LookupElement element, @NotNull CompletionSession session)
	{
		if(!result.getPrefixMatcher().prefixMatches(element) || !isVisibleElement(element, session))
		{
			return;
		}

		addCheckedCompletionToResult(result, element, session);
	}

	private static void addCheckedCompletionToResult(@NotNull CompletionResultSet result, @NotNull LookupElement element, @NotNull CompletionSession session)
	{
		result.addElement(element);
		session.isSomethingAdded = true;
	}

	private static boolean isVisibleElement(LookupElement element, CompletionSession session)
	{
		if(session.inDescriptor != null)
		{
			if(element.getObject() instanceof JetLookupObject)
			{
				JetLookupObject jetObject = (JetLookupObject) element.getObject();
				DeclarationDescriptor descriptor = jetObject.getDescriptor();
				return isVisibleDescriptor(descriptor, session);
			}
		}

		return true;
	}

	private static boolean isVisibleDescriptor(DeclarationDescriptor descriptor, CompletionSession session)
	{
		if(session.customInvocationCount >= 2)
		{
			// Show everything if user insist on showing completion list
			return true;
		}

		if(descriptor instanceof DeclarationDescriptorWithVisibility)
		{
			return Visibilities.isVisible((DeclarationDescriptorWithVisibility) descriptor, session.inDescriptor);
		}

		return true;
	}

	@NotNull
	private static LookupElement[] getReferenceVariants(@NotNull JetSimpleNameReference reference, @NotNull final CompletionResultSet result, @NotNull final CompletionSession session)
	{
		BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((NapileFile) reference.getExpression().getContainingFile()).getBindingContext();

		Collection<DeclarationDescriptor> descriptors = TipsManager.getReferenceVariants(reference.getExpression(), bindingContext);

		Collection<DeclarationDescriptor> checkedDescriptors = Collections2.filter(descriptors, new Predicate<DeclarationDescriptor>()
		{
			@Override
			public boolean apply(@Nullable DeclarationDescriptor descriptor)
			{
				if(descriptor == null)
				{
					return false;
				}

				return result.getPrefixMatcher().prefixMatches(descriptor.getName().getName()) && isVisibleDescriptor(descriptor, session);
			}
		});

		return DescriptorLookupConverter.collectLookupElements(bindingContext, checkedDescriptors);
	}
}
