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

package org.napile.idea.plugin.liveTemplates.macro;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.cli.jvm.compiler.TipsManager;
import org.napile.compiler.di.InjectorForMacros;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileNamedDeclaration;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.psi.NapilePropertyParameter;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.expressions.ExpressionTypingServices;
import org.napile.idea.plugin.project.WholeProjectAnalyzerFacade;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Macro;
import com.intellij.codeInsight.template.Result;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;

/**
 * @author Evgeny Gerashchenko
 * @since 2/8/12
 */
public abstract class BaseJetVariableMacro extends Macro
{
	@Nullable
	private NapileNamedDeclaration[] getVariables(Expression[] params, ExpressionContext context)
	{
		if(params.length != 0)
			return null;

		final Project project = context.getProject();
		PsiDocumentManager.getInstance(project).commitAllDocuments();

		PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(context.getEditor().getDocument());
		if(!(psiFile instanceof NapileFile))
			return null;

		NapileExpression contextExpression = findContextExpression(psiFile, context.getStartOffset());
		if(contextExpression == null)
			return null;

		BindingContext bc = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((NapileFile) psiFile).getBindingContext();
		JetScope scope = bc.get(BindingContext.RESOLUTION_SCOPE, contextExpression);
		if(scope == null)
		{
			return null;
		}

		ExpressionTypingServices callResolverContext = new InjectorForMacros(project).getExpressionTypingServices();

		List<VariableDescriptor> filteredDescriptors = new ArrayList<VariableDescriptor>();
		for(DeclarationDescriptor declarationDescriptor : scope.getAllDescriptors())
		{
			if(declarationDescriptor instanceof VariableDescriptor)
			{
				VariableDescriptor variableDescriptor = (VariableDescriptor) declarationDescriptor;
				if(isSuitable(variableDescriptor, scope, project, callResolverContext))
				{
					filteredDescriptors.add(variableDescriptor);
				}
			}
		}

		List<NapileNamedDeclaration> declarations = new ArrayList<NapileNamedDeclaration>();
		for(DeclarationDescriptor declarationDescriptor : TipsManager.excludeNotCallableExtensions(filteredDescriptors, scope))
		{
			PsiElement declaration = BindingContextUtils.descriptorToDeclaration(bc, declarationDescriptor);
			assert declaration == null || declaration instanceof PsiNamedElement;

			if(declaration instanceof NapileProperty || declaration instanceof NapilePropertyParameter)
			{
				declarations.add((NapileNamedDeclaration) declaration);
			}
		}

		return declarations.toArray(new NapileNamedDeclaration[declarations.size()]);
	}

	protected abstract boolean isSuitable(@NotNull VariableDescriptor variableDescriptor, @NotNull JetScope scope, @NotNull Project project, ExpressionTypingServices callResolverContext);

	@Nullable
	private static NapileExpression findContextExpression(PsiFile psiFile, int startOffset)
	{
		PsiElement e = psiFile.findElementAt(startOffset);
		while(e != null)
		{
			if(e instanceof NapileExpression)
			{
				return (NapileExpression) e;
			}
			e = e.getParent();
		}
		return null;
	}

	@Override
	public Result calculateResult(@NotNull Expression[] params, ExpressionContext context)
	{
		final NapileNamedDeclaration[] vars = getVariables(params, context);
		if(vars == null || vars.length == 0)
			return null;
		return new JetPsiElementResult(vars[0]);
	}

	@Override
	public LookupElement[] calculateLookupItems(@NotNull Expression[] params, ExpressionContext context)
	{
		final PsiNamedElement[] vars = getVariables(params, context);
		if(vars == null || vars.length < 2)
			return null;
		final Set<LookupElement> set = new LinkedHashSet<LookupElement>();
		for(PsiNamedElement var : vars)
		{
			set.add(LookupElementBuilder.create(var));
		}
		return set.toArray(new LookupElement[set.size()]);
	}
}
