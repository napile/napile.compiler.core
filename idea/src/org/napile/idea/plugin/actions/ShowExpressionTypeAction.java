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

import org.napile.compiler.lang.NapileLanguage;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author yole
 */
public class ShowExpressionTypeAction extends AnAction
{
	@Override
	public void actionPerformed(AnActionEvent e)
	{
		Editor editor = e.getData(PlatformDataKeys.EDITOR);
		PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
		assert editor != null && psiFile != null;
		NapileExpression expression;
		BindingContext bindingContext = ModuleAnalyzerUtil.analyze((NapileFile) psiFile).getBindingContext();
		if(editor.getSelectionModel().hasSelection())
		{
			int startOffset = editor.getSelectionModel().getSelectionStart();
			int endOffset = editor.getSelectionModel().getSelectionEnd();
			expression = CodeInsightUtilBase.findElementInRange(psiFile, startOffset, endOffset, NapileExpression.class, NapileLanguage.INSTANCE);
		}
		else
		{
			int offset = editor.getCaretModel().getOffset();
			expression = PsiTreeUtil.getParentOfType(psiFile.findElementAt(offset), NapileExpression.class);
			while(expression != null && bindingContext.get(BindingContext.EXPRESSION_TYPE, expression) == null)
			{
				expression = PsiTreeUtil.getParentOfType(expression, NapileExpression.class);
			}
			if(expression != null)
			{
				editor.getSelectionModel().setSelection(expression.getTextRange().getStartOffset(), expression.getTextRange().getEndOffset());
			}
		}
		if(expression != null)
		{
			JetType type = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression);
			if(type != null)
			{
				HintManager.getInstance().showInformationHint(editor, type.toString());
			}
		}
	}

	@Override
	public void update(AnActionEvent e)
	{
		Editor editor = e.getData(PlatformDataKeys.EDITOR);
		PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
		e.getPresentation().setEnabled(editor != null && psiFile instanceof NapileFile);
	}
}
