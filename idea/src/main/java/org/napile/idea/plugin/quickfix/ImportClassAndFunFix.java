/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.napile.idea.plugin.quickfix;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileNamedDeclaration;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.idea.plugin.JetBundle;
import org.napile.idea.plugin.actions.NapileAddImportAction;
import org.napile.idea.plugin.caches.NapileClassResolver;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import com.intellij.codeInsight.daemon.impl.ShowAutoImportPass;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;

/**
 * Check possibility and perform fix for unresolved references.
 *
 * @author Nikolay Krasko
 * @author VISTALL
 */
public class ImportClassAndFunFix extends JetHintAction<NapileSimpleNameExpression> implements HighPriorityAction
{
	@NotNull
	private final List<Pair<DeclarationDescriptor, NapileNamedDeclaration>> suggestions;

	public ImportClassAndFunFix(@NotNull NapileSimpleNameExpression element)
	{
		super(element);
		suggestions = computeSuggestions(element);
	}

	private static List<Pair<DeclarationDescriptor, NapileNamedDeclaration>> computeSuggestions(@NotNull NapileSimpleNameExpression element)
	{
		final PsiFile file = element.getContainingFile();
		if(file == null)
		{
			return Collections.emptyList();
		}

		String referenceName = element.getReferencedName();

		if(!StringUtil.isNotEmpty(referenceName))
		{
			return Collections.emptyList();
		}

		return getDescriptorsForImport(referenceName, (NapileFile) file);
	}

	private static List<Pair<DeclarationDescriptor, NapileNamedDeclaration>> getDescriptorsForImport(@NotNull final String typeName, @NotNull NapileFile file)
	{
		NapileClassResolver cache = NapileClassResolver.getInstance(file.getProject());

		return cache.getDescriptorsForImport(new Condition<String>()
		{
			@Override
			public boolean value(String s)
			{
				return typeName.equals(s);
			}
		}, file);
	}

	@Override
	public boolean showHint(@NotNull Editor editor)
	{
		if(suggestions.isEmpty())
		{
			return false;
		}

		final Project project = editor.getProject();
		if(project == null)
		{
			return false;
		}

		if(HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true))
		{
			return false;
		}

		if(!ApplicationManager.getApplication().isUnitTestMode())
		{
			String hintText = ShowAutoImportPass.getMessage(suggestions.size() > 1, DescriptorUtils.getFQName(suggestions.get(0).getFirst()).getFqName());

			HintManager.getInstance().showQuestionHint(editor, hintText, element.getTextOffset(), element.getTextRange().getEndOffset(), createAction(project, editor));
		}

		return needShowHint(editor);
	}

	public boolean needShowHint(@NotNull final Editor editor)
	{
		final AnalyzeExhaust analyzeExhaust = ModuleAnalyzerUtil.lastAnalyze(element.getContainingFile());

		final DeclarationDescriptor declarationDescriptor = analyzeExhaust.getBindingContext().get(BindingContext.REFERENCE_TARGET, element);
		if(declarationDescriptor != null)
		{
			return false;
		}

		/*if(suggestions.size() == 1 && CodeInsightSettings.getInstance().ADD_UNAMBIGIOUS_IMPORTS_ON_THE_FLY)
		{
			ApplicationManager.getApplication().invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					createAction(element.getProject(), editor).execute();
				}
			});

			return false;
		}*/

		return true;
	}

	@Override
	@NotNull
	public String getText()
	{
		return JetBundle.message("import.fix");
	}

	@Override
	@NotNull
	public String getFamilyName()
	{
		return JetBundle.message("import.fix");
	}

	@Override
	public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file)
	{
		return super.isAvailable(project, editor, file) && !suggestions.isEmpty();
	}

	@Override
	public void invoke(@NotNull final Project project, @NotNull final Editor editor, final PsiFile file) throws IncorrectOperationException
	{
		CommandProcessor.getInstance().runUndoTransparentAction(new Runnable()
		{
			@Override
			public void run()
			{
				createAction(project, editor).execute();
			}
		});
	}

	@Override
	public boolean startInWriteAction()
	{
		return true;
	}

	@NotNull
	private NapileAddImportAction createAction(@NotNull Project project, @NotNull Editor editor)
	{
		return new NapileAddImportAction(project, editor, element, suggestions);
	}

	@Nullable
	public static JetIntentionActionFactory createFactory()
	{
		return new JetIntentionActionFactory()
		{
			@Nullable
			@Override
			public JetIntentionAction<NapileSimpleNameExpression> createAction(@NotNull Diagnostic diagnostic)
			{
				// There could be different psi elements (i.e. NapileArrayAccessExpressionImpl), but we can fix only NapileSimpleNameExpressionImpl case
				if(diagnostic.getPsiElement() instanceof NapileSimpleNameExpression)
				{
					NapileSimpleNameExpression psiElement = (NapileSimpleNameExpression) diagnostic.getPsiElement();
					return new ImportClassAndFunFix(psiElement);
				}

				return null;
			}
		};
	}
}
