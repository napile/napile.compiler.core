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

package org.napile.idea.plugin.editor.completion;

import static com.intellij.patterns.StandardPatterns.or;
import static org.napile.idea.plugin.editor.completion.patterns.NapilePsiElementPattern.element;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.PackageDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileBlockExpression;
import org.napile.compiler.lang.psi.NapileCallParameterList;
import org.napile.compiler.lang.psi.NapileClassBody;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.idea.plugin.editor.completion.lookup.DescriptionLookupBuilder;
import org.napile.idea.plugin.module.Analyzer;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;

/**
 * @author VISTALL
 * @date 17:16/24.01.13
 */
public class NapileCompletionContributor extends CompletionContributor
{
	private static final ElementPattern<? extends PsiElement> MEMBERS_IN_CLASS_BODY = element().withSuperParent(2, NapileClassBody.class);
	private static final ElementPattern<? extends PsiElement> MEMBERS_IN_BODY_EXPRESSION_AND_CALL_PARAMETER_LIST = or(element().withSuperParent(2, NapileBlockExpression.class), element().withSuperParent(3, NapileCallParameterList.class));
	private static final ElementPattern<? extends PsiElement> IN_EXPRESSION = or(element().withSuperParent(2, NapileBlockExpression.class));
	private static final ElementPattern<? extends PsiElement> MODIFIER_LIST = or(MEMBERS_IN_CLASS_BODY);

	public NapileCompletionContributor()
	{
		extend(CompletionType.BASIC, MEMBERS_IN_CLASS_BODY, new NapileKeywordCompletionProvider(NapileTokens.CLASS_KEYWORD, NapileTokens.MACRO_KEYWORD, NapileTokens.METH_KEYWORD, NapileTokens.VAR_KEYWORD, NapileTokens.VAL_KEYWORD, NapileTokens.THIS_KEYWORD));
		extend(CompletionType.BASIC, MEMBERS_IN_BODY_EXPRESSION_AND_CALL_PARAMETER_LIST, new NapileKeywordCompletionProvider(NapileTokens.VAR_KEYWORD, NapileTokens.VAL_KEYWORD));

		extend(CompletionType.BASIC, MODIFIER_LIST, new NapileKeywordCompletionProvider(NapileTokens.MODIFIER_KEYWORDS));

		extend(CompletionType.BASIC, IN_EXPRESSION, new CompletionProvider<CompletionParameters>()
		{
			@Override
			protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result)
			{
				PsiElement position = parameters.getOriginalPosition();
				PsiFile containingFile = parameters.getOriginalFile();
				if(!(containingFile instanceof NapileFile))
					return;

				AnalyzeExhaust analyze = Analyzer.analyzeAll((NapileFile) containingFile);

				BodiesResolveContext bodiesResolveContext = analyze.getBodiesResolveContext();

				BindingContext bindingContext = analyze.getBindingContext();

				NapileExpression e = PsiTreeUtil.getParentOfType(position, NapileExpression.class);

				if(e == null)
					return;

				JetScope scope = bindingContext.get(BindingContext.RESOLUTION_SCOPE, e);
				if(scope == null)
				{
					NapileDeclaration declaration = PsiTreeUtil.getParentOfType(position, NapileDeclaration.class);
					if(declaration != null)
					{
						scope = bodiesResolveContext.getDeclaringScopes().get(declaration);
					}
				}
				if(scope == null)
					return;

				for(DeclarationDescriptor declarationDescriptor : scope.getAllDescriptors())
				{
					if(declarationDescriptor instanceof PackageDescriptor)
						continue;

					if(declarationDescriptor instanceof MethodDescriptor)
					{
						LookupElementBuilder item = DescriptionLookupBuilder.buildMethodLookup((MethodDescriptor) declarationDescriptor);
						if(item == null)
							continue;
						result.addElement(item);
					}
					else if(declarationDescriptor instanceof VariableDescriptor)
					{
						LookupElementBuilder item = DescriptionLookupBuilder.buildVariableLookup((VariableDescriptor) declarationDescriptor);
						if(item == null)
							continue;
						result.addElement(item);
					}
				}
			}
		});
	}
}
