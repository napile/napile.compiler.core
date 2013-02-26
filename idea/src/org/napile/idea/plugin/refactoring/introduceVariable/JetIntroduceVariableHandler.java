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

package org.napile.idea.plugin.refactoring.introduceVariable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.di.InjectorForTopDownAnalyzerBasic;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.calls.autocasts.DataFlowInfo;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.NamespaceType;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.checker.JetTypeChecker;
import org.napile.compiler.render.DescriptorRenderer;
import org.napile.idea.plugin.codeInsight.ReferenceToClassesShortening;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import org.napile.idea.plugin.refactoring.JetIntroduceHandlerBase;
import org.napile.idea.plugin.refactoring.JetNameSuggester;
import org.napile.idea.plugin.refactoring.JetNameValidatorImpl;
import org.napile.idea.plugin.refactoring.JetRefactoringBundle;
import org.napile.idea.plugin.refactoring.JetRefactoringUtil;
import com.intellij.codeInsight.PsiEquivalenceUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser;
import com.intellij.refactoring.util.CommonRefactoringUtil;

/**
 * User: Alefas
 * Date: 25.01.12
 */
public class JetIntroduceVariableHandler extends JetIntroduceHandlerBase
{

	private static final String INTRODUCE_VARIABLE = JetRefactoringBundle.message("introduce.variable");

	@Override
	public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file, DataContext dataContext)
	{
		JetRefactoringUtil.SelectExpressionCallback callback = new JetRefactoringUtil.SelectExpressionCallback()
		{
			@Override
			public void run(@Nullable NapileExpression expression)
			{
				doRefactoring(project, editor, expression);
			}
		};
		try
		{
			JetRefactoringUtil.selectExpression(editor, file, callback);
		}
		catch(JetRefactoringUtil.IntroduceRefactoringException e)
		{
			showErrorHint(project, editor, e.getMessage());
		}
	}

	private static void doRefactoring(@NotNull final Project project, final Editor editor, @Nullable NapileExpression _expression)
	{
		if(_expression == null)
		{
			showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.no.expression"));
			return;
		}
		if(_expression.getParent() instanceof NapileParenthesizedExpression)
		{
			_expression = (NapileExpression) _expression.getParent();
		}
		final NapileExpression expression = _expression;
		boolean noTypeInference = false;
		boolean needParentheses = false;
		if(expression.getParent() instanceof NapileQualifiedExpressionImpl)
		{
			NapileQualifiedExpressionImpl qualifiedExpression = (NapileQualifiedExpressionImpl) expression.getParent();
			if(qualifiedExpression.getReceiverExpression() != expression)
			{
				showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.no.expression"));
				return;
			}
		}
		else if(expression instanceof NapileStatementExpression)
		{
			showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.no.expression"));
			return;
		}
		else if(expression.getParent() instanceof NapileCallElement)
		{
			if(expression instanceof NapileAnonymMethodExpression)
			{
				needParentheses = true;
			}
			else
			{
				showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.no.expression"));
				return;
			}
		}
		else if(expression.getParent() instanceof NapileOperationExpression)
		{
			NapileOperationExpression operationExpression = (NapileOperationExpression) expression.getParent();
			if(operationExpression.getOperationReference() == expression)
			{
				showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.no.expression"));
				return;
			}
		}
		BindingContext bindingContext = ModuleAnalyzerUtil.analyze((NapileFile) expression.getContainingFile()).getBindingContext();
		final JetType expressionType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression); //can be null or error type
		JetScope scope = bindingContext.get(BindingContext.RESOLUTION_SCOPE, expression);
		if(scope != null)
		{
			DataFlowInfo dataFlowInfo = bindingContext.get(BindingContext.NON_DEFAULT_EXPRESSION_DATA_FLOW, expression);
			if(dataFlowInfo == null)
			{
				dataFlowInfo = DataFlowInfo.EMPTY;
			}

			AnalyzeExhaust analyzeExhaust = ModuleAnalyzerUtil.analyze(expression.getContainingFile());
			InjectorForTopDownAnalyzerBasic injector = analyzeExhaust.getInjector();
			if(injector == null)
				return;

			JetType typeNoExpectedType = injector.getExpressionTypingServices().getType(scope, expression, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo, injector.getBindingTrace());
			if(expressionType != null && typeNoExpectedType != null && !JetTypeChecker.INSTANCE.equalTypes(expressionType, typeNoExpectedType))
			{
				noTypeInference = true;
			}
		}
		if(expressionType instanceof NamespaceType)
		{
			showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.namespace.expression"));
			return;
		}
		if(expressionType == null && noTypeInference)
		{
			showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.expression.should.have.inferred.type"));
			return;
		}
		final PsiElement container = getContainer(expression);
		final PsiElement occurrenceContainer = getOccurrenceContainer(expression);
		if(container == null)
		{
			showErrorHint(project, editor, JetRefactoringBundle.message("cannot.refactor.no.container"));
			return;
		}
		final boolean isInplaceAvailableOnDataContext = editor.getSettings().isVariableInplaceRenameEnabled() && !ApplicationManager.getApplication().isUnitTestMode();
		final List<NapileExpression> allOccurrences = findOccurrences(occurrenceContainer, expression);
		final boolean finalNoTypeInference = noTypeInference;
		final boolean finalNeedParentheses = needParentheses;
		Pass<OccurrencesChooser.ReplaceChoice> callback = new Pass<OccurrencesChooser.ReplaceChoice>()
		{
			@Override
			public void pass(OccurrencesChooser.ReplaceChoice replaceChoice)
			{
				boolean replaceOccurrence = container != expression.getParent();
				final List<NapileExpression> allReplaces;
				if(OccurrencesChooser.ReplaceChoice.ALL == replaceChoice)
				{
					if(allOccurrences.size() > 1)
						replaceOccurrence = true;
					allReplaces = allOccurrences;
				}
				else
				{
					allReplaces = Collections.singletonList(expression);
				}

				PsiElement commonParent = PsiTreeUtil.findCommonParent(allReplaces);
				PsiElement commonContainer = getContainer(commonParent);
				JetNameValidatorImpl validator = new JetNameValidatorImpl(commonContainer, calculateAnchor(commonParent, commonContainer, allReplaces));
				String[] suggestedNames = JetNameSuggester.suggestNames(expression, validator);
				final LinkedHashSet<String> suggestedNamesSet = new LinkedHashSet<String>();
				Collections.addAll(suggestedNamesSet, suggestedNames);
				final Ref<NapileVariable> propertyRef = new Ref<NapileVariable>();
				final ArrayList<NapileExpression> references = new ArrayList<NapileExpression>();
				final Ref<NapileExpression> reference = new Ref<NapileExpression>();
				final Runnable introduceRunnable = introduceVariable(project, expression, suggestedNames, allReplaces, commonContainer, commonParent, replaceOccurrence, propertyRef, references, reference, finalNoTypeInference, finalNeedParentheses, expressionType);
				final boolean finalReplaceOccurrence = replaceOccurrence;
				CommandProcessor.getInstance().executeCommand(project, new Runnable()
				{
					@Override
					public void run()
					{
						ApplicationManager.getApplication().runWriteAction(introduceRunnable);
						NapileVariable property = propertyRef.get();
						if(property != null)
						{
							editor.getCaretModel().moveToOffset(property.getTextOffset());
							editor.getSelectionModel().removeSelection();
							if(isInplaceAvailableOnDataContext)
							{
								PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
								PsiDocumentManager.getInstance(project).
										doPostponedOperationsAndUnblockDocument(editor.getDocument());
								JetInplaceVariableIntroducer variableIntroducer = new JetInplaceVariableIntroducer(property, editor, project, INTRODUCE_VARIABLE, references.toArray(new NapileExpression[references.size()]), reference.get(), finalReplaceOccurrence, property, /*todo*/false, /*todo*/false, expressionType, finalNoTypeInference);
								variableIntroducer.performInplaceRefactoring(suggestedNamesSet);
							}
						}
					}
				}, INTRODUCE_VARIABLE, null);
			}
		};
		if(isInplaceAvailableOnDataContext)
		{
			OccurrencesChooser.<NapileExpression>simpleChooser(editor).
					showChooser(expression, allOccurrences, callback);
		}
		else
		{
			callback.pass(OccurrencesChooser.ReplaceChoice.ALL);
		}
	}

	private static Runnable introduceVariable(final @NotNull Project project, final NapileExpression expression, final String[] suggestedNames, final List<NapileExpression> allReplaces, final PsiElement commonContainer, final PsiElement commonParent, final boolean replaceOccurrence, final Ref<NapileVariable> propertyRef, final ArrayList<NapileExpression> references, final Ref<NapileExpression> reference, final boolean noTypeInference, final boolean needParentheses, final JetType expressionType)
	{
		return new Runnable()
		{
			@Override
			public void run()
			{
				String variableText = "val " + suggestedNames[0];
				if(noTypeInference)
				{
					variableText += ": " + DescriptorRenderer.TEXT.renderType(expressionType);
				}
				variableText += " = ";
				if(expression instanceof NapileParenthesizedExpression)
				{
					NapileParenthesizedExpression parenthesizedExpression = (NapileParenthesizedExpression) expression;
					NapileExpression innerExpression = parenthesizedExpression.getExpression();
					if(innerExpression != null)
					{
						variableText += innerExpression.getText();
					}
					else
					{
						variableText += expression.getText();
					}
				}
				else
				{
					variableText += expression.getText();
				}
				NapileVariable property = NapilePsiFactory.createProperty(project, variableText);
				if(property == null)
					return;
				PsiElement anchor = calculateAnchor(commonParent, commonContainer, allReplaces);
				if(anchor == null)
					return;
				boolean needBraces = !(commonContainer instanceof NapileBlockExpression ||
						commonContainer instanceof NapileClassBody);
				if(!needBraces)
				{
					property = (NapileVariable) commonContainer.addBefore(property, anchor);
					commonContainer.addBefore(NapilePsiFactory.createWhiteSpace(project, "\n"), anchor);
				}
				else
				{
					NapileExpression emptyBody = NapilePsiFactory.createEmptyBody(project);
					PsiElement firstChild = emptyBody.getFirstChild();
					emptyBody.addAfter(NapilePsiFactory.createWhiteSpace(project, "\n"), firstChild);
					if(replaceOccurrence && commonContainer != null)
					{
						for(NapileExpression replace : allReplaces)
						{
							boolean isActualExpression = expression == replace;
							if(!needParentheses && !(replace.getParent() instanceof NapileCallExpression))
							{
								NapileExpression element = (NapileExpression) replace.replace(NapilePsiFactory.createExpression(project, suggestedNames[0]));
								if(isActualExpression)
									reference.set(element);
							}
							else
							{
								NapileValueArgumentList argumentList = NapilePsiFactory.createCallArguments(project, "(" + suggestedNames[0] + ")");
								NapileValueArgumentList element = (NapileValueArgumentList) replace.replace(argumentList);
								if(isActualExpression)
									reference.set(element.getArguments().get(0).getArgumentExpression());
							}
						}
						PsiElement oldElement = commonContainer;
						if(commonContainer instanceof NapileWhenEntry)
						{
							NapileExpression body = ((NapileWhenEntry) commonContainer).getExpression();
							if(body != null)
							{
								oldElement = body;
							}
						}
						else if(commonContainer instanceof NapileNamedMethodOrMacro)
						{
							NapileExpression body = ((NapileNamedMethodOrMacro) commonContainer).getBodyExpression();
							if(body != null)
							{
								oldElement = body;
							}
						}
						else if(commonContainer instanceof NapileContainerNode)
						{
							NapileContainerNode container = (NapileContainerNode) commonContainer;
							PsiElement[] children = container.getChildren();
							for(PsiElement child : children)
							{
								if(child instanceof NapileExpression)
								{
									oldElement = child;
								}
							}
						}
						//ugly logic to make sure we are working with right actual expression
						NapileExpression actualExpression = reference.get();
						int diff = actualExpression.getTextRange().getStartOffset() - oldElement.getTextRange().getStartOffset();
						String actualExpressionText = actualExpression.getText();
						PsiElement newElement = emptyBody.addAfter(oldElement, firstChild);
						PsiElement elem = newElement.findElementAt(diff);
						while(elem != null && !(elem instanceof NapileExpression && actualExpressionText.equals(elem.getText())))
						{
							elem = elem.getParent();
						}
						if(elem != null)
						{
							reference.set((NapileExpression) elem);
						}
						emptyBody.addAfter(NapilePsiFactory.createWhiteSpace(project, "\n"), firstChild);
						property = (NapileVariable) emptyBody.addAfter(property, firstChild);
						emptyBody.addAfter(NapilePsiFactory.createWhiteSpace(project, "\n"), firstChild);
						actualExpression = reference.get();
						diff = actualExpression.getTextRange().getStartOffset() - emptyBody.getTextRange().getStartOffset();
						actualExpressionText = actualExpression.getText();
						emptyBody = (NapileExpression) anchor.replace(emptyBody);
						elem = emptyBody.findElementAt(diff);
						while(elem != null && !(elem instanceof NapileExpression && actualExpressionText.equals(elem.getText())))
						{
							elem = elem.getParent();
						}
						if(elem != null)
						{
							reference.set((NapileExpression) elem);
						}
					}
					else
					{
						property = (NapileVariable) emptyBody.addAfter(property, firstChild);
						emptyBody.addAfter(NapilePsiFactory.createWhiteSpace(project, "\n"), firstChild);
						emptyBody = (NapileExpression) anchor.replace(emptyBody);
					}
					for(PsiElement child : emptyBody.getChildren())
					{
						if(child instanceof NapileVariable)
						{
							property = (NapileVariable) child;
						}
					}
					if(commonContainer instanceof NapileNamedMethodOrMacro)
					{
						//we should remove equals sign
						NapileNamedMethodOrMacro function = (NapileNamedMethodOrMacro) commonContainer;
						if(!function.hasDeclaredReturnType())
						{
							//todo: add return type
						}
						function.getEqualsToken().delete();
					}
					else if(commonContainer instanceof NapileContainerNode)
					{
						NapileContainerNode node = (NapileContainerNode) commonContainer;
						if(node.getParent() instanceof NapileIfExpression)
						{
							PsiElement next = node.getNextSibling();
							if(next != null)
							{
								PsiElement nextnext = next.getNextSibling();
								if(nextnext != null && nextnext.getNode().getElementType() == NapileTokens.ELSE_KEYWORD)
								{
									if(next instanceof PsiWhiteSpace)
									{
										next.replace(NapilePsiFactory.createWhiteSpace(project, " "));
									}
								}
							}
						}
					}
				}
				for(NapileExpression replace : allReplaces)
				{
					if(replaceOccurrence && !needBraces)
					{
						boolean isActualExpression = expression == replace;

						if(!needParentheses && !(replace.getParent() instanceof NapileCallExpression))
						{
							NapileExpression element = (NapileExpression) replace.replace(NapilePsiFactory.createExpression(project, suggestedNames[0]));
							references.add(element);
							if(isActualExpression)
								reference.set(element);
						}
						else
						{
							NapileValueArgumentList argumentList = NapilePsiFactory.createCallArguments(project, "(" + suggestedNames[0] + ")");
							NapileValueArgumentList element = (NapileValueArgumentList) replace.replace(argumentList);
							NapileExpression argumentExpression = element.getArguments().get(0).getArgumentExpression();
							references.add(argumentExpression);
							if(isActualExpression)
							{
								reference.set(argumentExpression);
							}
						}
					}
					else if(!needBraces)
					{
						replace.delete();
					}
				}
				propertyRef.set(property);
				if(noTypeInference)
				{
					ReferenceToClassesShortening.compactReferenceToClasses(Collections.singletonList(property));
				}
			}
		};
	}

	private static PsiElement calculateAnchor(PsiElement commonParent, PsiElement commonContainer, List<NapileExpression> allReplaces)
	{
		PsiElement anchor = commonParent;
		if(anchor != commonContainer)
		{
			while(anchor.getParent() != commonContainer)
			{
				anchor = anchor.getParent();
			}
		}
		else
		{
			anchor = commonContainer.getFirstChild();
			int startOffset = commonContainer.getTextRange().getEndOffset();
			for(NapileExpression expr : allReplaces)
			{
				int offset = expr.getTextRange().getStartOffset();
				if(offset < startOffset)
					startOffset = offset;
			}
			while(anchor != null && !anchor.getTextRange().contains(startOffset))
			{
				anchor = anchor.getNextSibling();
			}
			if(anchor == null)
				return null;
		}
		return anchor;
	}

	private static ArrayList<NapileExpression> findOccurrences(PsiElement occurrenceContainer, @NotNull NapileExpression expression)
	{
		if(expression instanceof NapileParenthesizedExpression)
		{
			NapileParenthesizedExpression parenthesizedExpression = (NapileParenthesizedExpression) expression;
			NapileExpression innerExpression = parenthesizedExpression.getExpression();
			if(innerExpression != null)
			{
				expression = innerExpression;
			}
		}
		final NapileExpression actualExpression = expression;

		final ArrayList<NapileExpression> result = new ArrayList<NapileExpression>();

		final BindingContext bindingContext = ModuleAnalyzerUtil.analyze((NapileFile) expression.getContainingFile()).getBindingContext();

		NapileVisitorVoid visitor = new NapileVisitorVoid()
		{
			@Override
			public void visitJetElement(NapileElement element)
			{
				element.acceptChildren(this);
				super.visitJetElement(element);
			}

			@Override
			public void visitExpression(final NapileExpression expression)
			{
				if(PsiEquivalenceUtil.areElementsEquivalent(expression, actualExpression, null, new Comparator<PsiElement>()
				{
					@Override
					public int compare(PsiElement element1, PsiElement element2)
					{
						if(element1.getNode().getElementType() == NapileTokens.IDENTIFIER && element2.getNode().getElementType() == NapileTokens.IDENTIFIER)
						{
							if(element1.getParent() instanceof NapileSimpleNameExpression && element2.getParent() instanceof NapileSimpleNameExpression)
							{
								NapileSimpleNameExpression expr1 = (NapileSimpleNameExpression) element1.getParent();
								NapileSimpleNameExpression expr2 = (NapileSimpleNameExpression) element2.getParent();
								DeclarationDescriptor descr1 = bindingContext.get(BindingContext.REFERENCE_TARGET, expr1);
								DeclarationDescriptor descr2 = bindingContext.get(BindingContext.REFERENCE_TARGET, expr2);
								if(descr1 != descr2)
								{
									return 1;
								}
								else
								{
									return 0;
								}
							}
						}
						if(!element1.textMatches(element2))
						{
							return 1;
						}
						else
						{
							return 0;
						}
					}
				}, null, false))
				{
					PsiElement parent = expression.getParent();
					if(parent instanceof NapileParenthesizedExpression)
					{
						result.add((NapileParenthesizedExpression) parent);
					}
					else
					{
						result.add(expression);
					}
				}
				else
				{
					super.visitExpression(expression);
				}
			}
		};
		occurrenceContainer.accept(visitor);
		return result;
	}

	@Nullable
	private static PsiElement getContainer(PsiElement place)
	{
		if(place instanceof NapileBlockExpression || place instanceof NapileClassBody)
		{
			return place;
		}
		while(place != null)
		{
			PsiElement parent = place.getParent();
			if(parent instanceof NapileContainerNode)
			{
				if(!isBadContainerNode((NapileContainerNode) parent, place))
				{
					return parent;
				}
			}
			if(parent instanceof NapileBlockExpression || parent instanceof NapileWhenEntry ||
					parent instanceof NapileClassBody)
			{
				return parent;
			}
			else if(parent instanceof NapileNamedMethodOrMacro)
			{
				NapileNamedMethodOrMacro function = (NapileNamedMethodOrMacro) parent;
				if(function.getBodyExpression() == place)
				{
					return parent;
				}
			}
			place = parent;
		}
		return null;
	}

	private static boolean isBadContainerNode(NapileContainerNode parent, PsiElement place)
	{
		if(parent.getParent() instanceof NapileIfExpression && ((NapileIfExpression) parent.getParent()).getCondition() == place)
		{
			return true;
		}
		else if(parent.getParent() instanceof NapileLoopExpression && ((NapileLoopExpression) parent.getParent()).getBody() != place)
		{
			return true;
		}
		return false;
	}

	@Nullable
	private static PsiElement getOccurrenceContainer(PsiElement place)
	{
		PsiElement result = null;
		while(place != null)
		{
			PsiElement parent = place.getParent();
			if(parent instanceof NapileContainerNode)
			{
				if(!(place instanceof NapileBlockExpression) && !isBadContainerNode((NapileContainerNode) parent, place))
				{
					result = parent;
				}
			}
			else if(parent instanceof NapileClassBody || parent instanceof NapileFile)
			{
				if(result == null)
				{
					return parent;
				}
				else
				{
					return result;
				}
			}
			else if(parent instanceof NapileBlockExpression)
			{
				result = parent;
			}
			else if(parent instanceof NapileWhenEntry)
			{
				if(!(place instanceof NapileBlockExpression))
				{
					result = parent;
				}
			}
			else if(parent instanceof NapileNamedMethodOrMacro)
			{
				NapileNamedMethodOrMacro function = (NapileNamedMethodOrMacro) parent;
				if(function.getBodyExpression() == place)
				{
					if(!(place instanceof NapileBlockExpression))
					{
						result = parent;
					}
				}
			}
			place = parent;
		}
		return null;
	}

	private static void showErrorHint(Project project, Editor editor, String message)
	{
		if(ApplicationManager.getApplication().isUnitTestMode())
			throw new RuntimeException(message);
		CommonRefactoringUtil.showErrorHint(project, editor, message, INTRODUCE_VARIABLE, HelpID.INTRODUCE_VARIABLE);
	}

	@Override
	public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext)
	{
		//do nothing
	}
}
