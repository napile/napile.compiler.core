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

/*
 * @author max
 */
package org.napile.idea.plugin.highlighter;

import org.napile.compiler.lang.psi.NapileBreakExpression;
import org.napile.compiler.lang.psi.NapileLabelExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;

class LabelsHighlightingVisitor extends HighlightingVisitor
{
	LabelsHighlightingVisitor(AnnotationHolder holder)
	{
		super(holder);
	}

	@Override
	public void visitLabelExpression(NapileLabelExpression expression)
	{
		PsiElement targetLabel = expression.getLabelNameElement();
		if(targetLabel != null)
		{
			JetPsiChecker.highlightName(holder, targetLabel, JetHighlightingColors.LABEL);
		}
	}

	@Override
	public void visitBreakExpression(NapileBreakExpression expression)
	{
		NapileSimpleNameExpression targetLabel = expression.getTargetLabel();
		if(targetLabel != null)
		{
			JetPsiChecker.highlightName(holder, targetLabel, JetHighlightingColors.LABEL);
		}
	}
}
