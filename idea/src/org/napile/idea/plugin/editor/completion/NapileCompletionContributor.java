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
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.PackageDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileBlockExpression;
import org.napile.compiler.lang.psi.NapileCallParameterList;
import org.napile.compiler.lang.psi.NapileClassBody;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileDotQualifiedExpressionImpl;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.NamespaceType;
import org.napile.idea.plugin.editor.completion.lookup.DescriptionLookupBuilder;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
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

	private static final ElementPattern<? extends PsiElement> MEMBERS_IN_BODY_EXPRESSION_ = element().withSuperParent(2, NapileBlockExpression.class);
	private static final ElementPattern<? extends PsiElement> MEMBERS_IN_CALL_PARAMETER_LIST = element().withSuperParent(3, NapileCallParameterList.class);

	private static final ElementPattern<? extends PsiElement> IN_EXPRESSION = or(element().withSuperParent(2, NapileBlockExpression.class));
	private static final ElementPattern<? extends PsiElement> MODIFIER_LIST = or(MEMBERS_IN_CLASS_BODY);
	private static final ElementPattern<? extends PsiElement> EXPECT_TYPE = or(
																				element().afterLeaf(element().withElementType(NapileTokens.COLON)),
																				element().afterLeaf(element().withElementType(NapileTokens.IS_KEYWORD)),
																				element().afterLeaf(element().withElementType(NapileTokens.NOT_IS)),
																				element().afterLeaf(element().withElementType(NapileTokens.AS_KEYWORD)),
																				element().afterLeaf(element().withElementType(NapileTokens.AS_SAFE))
																				);

	public NapileCompletionContributor()
	{
		extend(CompletionType.BASIC, MEMBERS_IN_CLASS_BODY, new NapileKeywordCompletionProvider(NapileTokens.CLASS_KEYWORD, NapileTokens.MACRO_KEYWORD, NapileTokens.METH_KEYWORD, NapileTokens.VAR_KEYWORD, NapileTokens.VAL_KEYWORD, NapileTokens.THIS_KEYWORD));
		extend(CompletionType.BASIC, MEMBERS_IN_BODY_EXPRESSION_, new NapileKeywordCompletionProvider(NapileTokens.VAR_KEYWORD, NapileTokens.VAL_KEYWORD));
		extend(CompletionType.BASIC, MEMBERS_IN_CALL_PARAMETER_LIST, new NapileKeywordCompletionProvider(NapileTokens.VAR_KEYWORD, NapileTokens.VAL_KEYWORD, NapileTokens.REF_KEYWORD));

		extend(CompletionType.BASIC, MODIFIER_LIST, new NapileKeywordCompletionProvider(NapileTokens.MODIFIER_KEYWORDS));

		extend(CompletionType.BASIC, EXPECT_TYPE, new CompletionProvider<CompletionParameters>()
		{
			@Override
			protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result)
			{
				PsiElement position = parameters.getOriginalPosition();
				PsiFile containingFile = parameters.getOriginalFile();
				if(!(containingFile instanceof NapileFile))
					return;

				AnalyzeExhaust analyze = ModuleAnalyzerUtil.analyzeAll((NapileFile) containingFile);

				NapileDeclaration declaration = PsiTreeUtil.getParentOfType(position, NapileDeclaration.class);
				if(declaration == null)
					return;

				BodiesResolveContext bodiesResolveContext = analyze.getBodiesResolveContext();

				JetScope scope = analyze.getBindingContext().get(BindingContext.RESOLUTION_SCOPE, declaration);
				if(scope == null)
				{
					return;
				}

				for(DeclarationDescriptor declarationDescriptor : scope.getAllDescriptors())
				{
					if(declarationDescriptor instanceof ClassDescriptor || declarationDescriptor instanceof TypeParameterDescriptor)
						DescriptionLookupBuilder.addElement(declarationDescriptor, result);
				}
			}
		});

		extend(CompletionType.BASIC, element().withSuperParent(2, NapileDotQualifiedExpressionImpl.class), new CompletionProvider<CompletionParameters>()
		{
			@Override
			protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result)
			{
				PsiElement position = parameters.getOriginalPosition();
				PsiFile containingFile = parameters.getOriginalFile();
				if(!(containingFile instanceof NapileFile))
					return;

				PsiElement prevElement = position.getPrevSibling();
				if(!(prevElement instanceof NapileDotQualifiedExpressionImpl))
					return;

				NapileDotQualifiedExpressionImpl dotQualifiedExpression = (NapileDotQualifiedExpressionImpl) prevElement;

				AnalyzeExhaust analyze = ModuleAnalyzerUtil.analyzeAll((NapileFile) containingFile);

				//BodiesResolveContext bodiesResolveContext = analyze.getBodiesResolveContext();

				BindingContext bindingContext = analyze.getBindingContext();

				JetType type = bindingContext.get(BindingContext.EXPRESSION_TYPE, dotQualifiedExpression.getReceiverExpression());
				if(type == null)
					return;

				if(type instanceof NamespaceType)
				{

				}
				else
				{
					DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
					if(declarationDescriptor instanceof MutableClassDescriptor)
					{
						MutableClassDescriptor classDescriptor = (MutableClassDescriptor) declarationDescriptor;
						for(CallableMemberDescriptor descriptor : classDescriptor.getAllCallableMembers())
							DescriptionLookupBuilder.addElement(descriptor, result);
					}
				}
			}
		});

		extend(CompletionType.BASIC, IN_EXPRESSION, new CompletionProvider<CompletionParameters>()
		{
			@Override
			protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result)
			{
				PsiElement position = parameters.getOriginalPosition();
				PsiFile containingFile = parameters.getOriginalFile();
				if(!(containingFile instanceof NapileFile))
					return;

				AnalyzeExhaust analyze = ModuleAnalyzerUtil.analyzeAll((NapileFile) containingFile);

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

					if(declarationDescriptor instanceof MethodDescriptor || declarationDescriptor instanceof VariableDescriptor)
						DescriptionLookupBuilder.addElement(declarationDescriptor, result);
				}
			}
		});
	}
}
