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

package org.napile.idea.plugin.quickfix;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.ImportPath;
import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.idea.plugin.JetBundle;
import org.napile.idea.plugin.actions.JetAddImportAction;
import org.napile.idea.plugin.caches.JetShortNamesCache;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.codeInsight.daemon.impl.ShowAutoImportPass;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;

/**
 * Check possibility and perform fix for unresolved references.
 *
 * @author Nikolay Krasko
 */
public class ImportClassAndFunFix extends JetHintAction<NapileSimpleNameExpression> implements HighPriorityAction
{

	@NotNull
	private final Collection<FqName> suggestions;

	public ImportClassAndFunFix(@NotNull NapileSimpleNameExpression element)
	{
		super(element);
		suggestions = computeSuggestions(element);
	}

	private static Collection<FqName> computeSuggestions(@NotNull NapileSimpleNameExpression element)
	{
		final PsiFile file = element.getContainingFile();
		if(!(file instanceof NapileFile))
		{
			return Collections.emptyList();
		}

		final String referenceName = element.getReferencedName();

		if(!StringUtil.isNotEmpty(referenceName))
		{
			return Collections.emptyList();
		}

		assert referenceName != null;

		List<FqName> result = Lists.newArrayList();
		result.addAll(getClassNames(referenceName, (NapileFile) file));

		return Collections2.filter(result, new Predicate<FqName>()
		{
			@Override
			public boolean apply(@Nullable FqName fqName)
			{
				assert fqName != null;
				return ImportInsertHelper.doNeedImport(new ImportPath(fqName, false), null, (NapileFile) file);
			}
		});
	}

	/*
		 * Searches for possible class names in kotlin context and java facade.
		 */
	public static Collection<FqName> getClassNames(@NotNull String referenceName, @NotNull NapileFile file)
	{
		Set<FqName> possibleResolveNames = Sets.newHashSet();

		possibleResolveNames.addAll(getJetClasses(referenceName, file));

		// TODO: Do appropriate sorting
		return Lists.newArrayList(possibleResolveNames);
	}

	private static Collection<FqName> getJetClasses(@NotNull final String typeName, @NotNull NapileFile file)
	{
		JetShortNamesCache cache = JetShortNamesCache.getInstance(file.getProject());
		Collection<ClassDescriptor> descriptors = cache.getJetClassesDescriptors(new Condition<String>()
		{
			@Override
			public boolean value(String s)
			{
				return typeName.equals(s);
			}
		}, file);

		return Collections2.transform(descriptors, new Function<ClassDescriptor, FqName>()
		{
			@Override
			public FqName apply(ClassDescriptor descriptor)
			{
				return DescriptorUtils.getFQName(descriptor).toSafe();
			}
		});
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
			String hintText = ShowAutoImportPass.getMessage(suggestions.size() > 1, suggestions.iterator().next().getFqName());

			HintManager.getInstance().showQuestionHint(editor, hintText, element.getTextOffset(), element.getTextRange().getEndOffset(), createAction(project, editor));
		}

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
	private JetAddImportAction createAction(@NotNull Project project, @NotNull Editor editor)
	{
		return new JetAddImportAction(project, editor, element, suggestions);
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
