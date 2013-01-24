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

package org.napile.idea.plugin.actions;

import java.util.List;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileNamedDeclaration;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.idea.plugin.JetBundle;
import org.napile.idea.plugin.NapileIconProvider;
import org.napile.idea.plugin.quickfix.ImportInsertHelper;
import com.intellij.codeInsight.daemon.QuickFixBundle;
import com.intellij.codeInsight.daemon.impl.actions.AddImportAction;
import com.intellij.codeInsight.hint.QuestionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

/**
 * Automatically adds import directive to the file for resolving reference.
 * Based on {@link AddImportAction}
 *
 * @author Nikolay Krasko
 */
public class NapileAddImportAction implements QuestionAction
{
	private final Project myProject;
	private final Editor myEditor;
	private final PsiElement myElement;
	private final List<Pair<DeclarationDescriptor, NapileNamedDeclaration>> possibleImports;

	/**
	 * @param project Project where action takes place.
	 * @param editor  Editor where modification should be done.
	 * @param element Element with unresolved reference.
	 * @param imports Variants for resolution.
	 */
	public NapileAddImportAction(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element, @NotNull List<Pair<DeclarationDescriptor, NapileNamedDeclaration>> imports)
	{
		myProject = project;
		myEditor = editor;
		myElement = element;
		possibleImports = imports;
	}

	@Override
	public boolean execute()
	{
		PsiDocumentManager.getInstance(myProject).commitAllDocuments();

		if(!myElement.isValid())
		{
			return false;
		}

		if(possibleImports.size() == 1)
		{
			addImport(myElement, myProject, possibleImports.iterator().next());
		}
		else
		{
			chooseClassAndImport();
		}

		return true;
	}

	protected BaseListPopupStep getImportSelectionPopup()
	{
		return new BaseListPopupStep<Pair<DeclarationDescriptor, NapileNamedDeclaration>>(JetBundle.message("imports.chooser.title"), possibleImports)
		{
			@Override
			public boolean isAutoSelectionEnabled()
			{
				return false;
			}

			@Override
			public PopupStep onChosen(Pair<DeclarationDescriptor, NapileNamedDeclaration> selectedValue, boolean finalChoice)
			{
				if(selectedValue == null)
				{
					return FINAL_CHOICE;
				}

				if(finalChoice)
				{
					addImport(myElement, myProject, selectedValue);
					return FINAL_CHOICE;
				}

				List<String> toExclude = AddImportAction.getAllExcludableStrings(DescriptorUtils.getFQName(selectedValue.getFirst()).getFqName());

				return new BaseListPopupStep<String>(null, toExclude)
				{
					@NotNull
					@Override
					public String getTextFor(String value)
					{
						return "Exclude '" + value + "' from auto-import";
					}

					@Override
					public PopupStep onChosen(String selectedValue, boolean finalChoice)
					{
						if(finalChoice)
						{
							AddImportAction.excludeFromImport(myProject, selectedValue);
						}

						return super.onChosen(selectedValue, finalChoice);
					}
				};
			}

			@Override
			public boolean hasSubstep(Pair<DeclarationDescriptor, NapileNamedDeclaration> selectedValue)
			{
				return true;
			}

			@NotNull
			@Override
			public String getTextFor(Pair<DeclarationDescriptor, NapileNamedDeclaration> value)
			{
				return DescriptorUtils.getFQName(value.getFirst()).getFqName();
			}

			@Override
			public Icon getIconFor(Pair<DeclarationDescriptor, NapileNamedDeclaration> value)
			{
				return NapileIconProvider.INSTANCE.getIcon(value.getSecond(), 0);
			}
		};
	}

	protected static void addImport(final PsiElement element, final Project project, final Pair<DeclarationDescriptor, NapileNamedDeclaration> selectedImport)
	{
		PsiDocumentManager.getInstance(project).commitAllDocuments();

		CommandProcessor.getInstance().executeCommand(project, new Runnable()
		{
			@Override
			public void run()
			{
				ApplicationManager.getApplication().runWriteAction(new Runnable()
				{
					@Override
					public void run()
					{
						PsiFile file = element.getContainingFile();
						if(!(file instanceof NapileFile))
							return;
						ImportInsertHelper.addImportDirective(selectedImport, (NapileFile) file);
					}
				});
			}
		}, QuickFixBundle.message("add.import"), null);
	}

	private void chooseClassAndImport()
	{
		JBPopupFactory.getInstance().createListPopup(getImportSelectionPopup()).showInBestPositionFor(myEditor);
	}
}
