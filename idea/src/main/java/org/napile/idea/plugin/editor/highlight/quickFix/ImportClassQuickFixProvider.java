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

package org.napile.idea.plugin.editor.highlight.quickFix;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.idea.plugin.editor.highlight.NapileQuickFixProvider;
import org.napile.idea.plugin.quickfix.ImportClassAndFunFix;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 22:40/26.02.13
 */
public class ImportClassQuickFixProvider implements NapileQuickFixProvider
{
	@Override
	public IntentionAction createQuickFix(@NotNull Diagnostic diagnostic, @NotNull Editor editor, @NotNull HighlightInfo highlightInfo)
	{
		// There could be different psi elements (i.e. NapileArrayAccessExpressionImpl), but we can fix only NapileSimpleNameExpressionImpl case
		final PsiElement element = diagnostic.getPsiElement();
		if(element instanceof NapileSimpleNameExpression)
		{
			final ImportClassAndFunFix importClassAndFunFix = new ImportClassAndFunFix((NapileSimpleNameExpression) element);
			if(importClassAndFunFix.needShowHint(editor))
			{
				return importClassAndFunFix;
			}
		}

		return null;
	}
}
