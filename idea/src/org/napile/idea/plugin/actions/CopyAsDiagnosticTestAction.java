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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.List;

import org.napile.compiler.checkers.CheckerTestUtil;
import org.napile.compiler.lang.resolve.AnalyzingUtils;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.psi.NapileFile;
import org.napile.idea.plugin.project.WholeProjectAnalyzerFacade;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;

/**
 * @author abreslav
 */
public class CopyAsDiagnosticTestAction extends AnAction
{
	@Override
	public void actionPerformed(AnActionEvent e)
	{
		Editor editor = e.getData(PlatformDataKeys.EDITOR);
		PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
		assert editor != null && psiFile != null;

		BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((NapileFile) psiFile).getBindingContext();
		List<PsiErrorElement> syntaxError = AnalyzingUtils.getSyntaxErrorRanges(psiFile);

		String result = CheckerTestUtil.addDiagnosticMarkersToText(psiFile, bindingContext, syntaxError).toString();

		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(result), new ClipboardOwner()
		{
			@Override
			public void lostOwnership(Clipboard clipboard, Transferable contents)
			{
			}
		});
	}


	@Override
	public void update(AnActionEvent e)
	{
		Editor editor = e.getData(PlatformDataKeys.EDITOR);
		PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
		e.getPresentation().setEnabled(editor != null && psiFile instanceof NapileFile && ApplicationManager.getApplication().isInternal());
	}
}
