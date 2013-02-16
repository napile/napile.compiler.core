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

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileDotQualifiedExpressionImpl;
import org.napile.compiler.lang.psi.NapilePsiFactory;
import org.napile.compiler.lang.psi.NapileQualifiedExpressionImpl;
import org.napile.compiler.lang.psi.NapileSafeQualifiedExpressionImpl;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.idea.plugin.JetBundle;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;

/**
 * @author svtk
 * @author slukjanov aka Frostman
 */
public class ReplaceCallFix implements IntentionAction
{
	private final boolean toSafe;

	private ReplaceCallFix(boolean safe)
	{
		this.toSafe = safe;
	}

	/**
	 * @return quickfix for replacing dot call with toSafe (?.) call
	 */
	public static ReplaceCallFix toSafeCall()
	{
		return new ReplaceCallFix(true);
	}

	/**
	 * @return quickfix for replacing unnecessary toSafe (?.) call with dot call
	 */
	public static ReplaceCallFix toDotCallFromSafeCall()
	{
		return new ReplaceCallFix(false);
	}

	@NotNull
	@Override
	public String getText()
	{
		return toSafe ? JetBundle.message("replace.with.safe.call") : JetBundle.message("replace.with.dot.call");
	}

	@NotNull
	@Override
	public String getFamilyName()
	{
		return getText();
	}

	@Override
	public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file)
	{
		if(file instanceof NapileFile)
		{
			return getCallExpression(editor, (NapileFile) file) != null;
		}
		return false;
	}

	@Override
	public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException
	{
		NapileQualifiedExpressionImpl callExpression = getCallExpression(editor, (NapileFile) file);
		assert callExpression != null;

		NapileExpression selector = callExpression.getSelectorExpression();
		if(selector != null)
		{
			NapileQualifiedExpressionImpl newElement = (NapileQualifiedExpressionImpl) NapilePsiFactory.createExpression(project, callExpression.getReceiverExpression().getText() + (toSafe ? "?." : ".") + selector.getText());

			callExpression.replace(newElement);
		}
	}

	@Override
	public boolean startInWriteAction()
	{
		return true;
	}

	private NapileQualifiedExpressionImpl getCallExpression(@NotNull Editor editor, @NotNull NapileFile file)
	{
		final PsiElement elementAtCaret = getElementAtCaret(editor, file);
		return PsiTreeUtil.getParentOfType(elementAtCaret, toSafe ? NapileDotQualifiedExpressionImpl.class : NapileSafeQualifiedExpressionImpl.class);
	}

	private static PsiElement getElementAtCaret(Editor editor, PsiFile file)
	{
		return file.findElementAt(editor.getCaretModel().getOffset());
	}
}
