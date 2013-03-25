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

package org.napile.idea.plugin.editor.highlight.postHighlight;

import java.util.Collection;

import org.napile.compiler.lang.psi.NapileBreakExpression;
import org.napile.compiler.lang.psi.NapileLabelExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.idea.plugin.editor.highlight.NapileHighlightingColors;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 21:55/26.02.13
 */
public class LabelsHighlightingVisitor extends PostHighlightVisitor
{
	public LabelsHighlightingVisitor(BindingTrace context, Collection<HighlightInfo> holder)
	{
		super(context, holder);
	}

	@Override
	public void visitLabelExpression(NapileLabelExpression expression)
	{
		super.visitLabelExpression(expression);
		PsiElement targetLabel = expression.getLabelNameElement();
		if(targetLabel != null)
		{
			highlightName(targetLabel, NapileHighlightingColors.LABEL, null);
		}
	}

	@Override
	public void visitBreakExpression(NapileBreakExpression expression)
	{
		super.visitBreakExpression(expression);
		NapileSimpleNameExpression targetLabel = expression.getTargetLabel();
		if(targetLabel != null)
		{
			highlightName(targetLabel, NapileHighlightingColors.LABEL, null);
		}
	}
}
