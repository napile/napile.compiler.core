/*
 * Copyright 2010-2012 napile.org
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

package org.napile.idea.plugin.injection.text;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.injection.text.TextCodeInjection;
import org.napile.compiler.injection.text.lang.psi.TextExpressionInsert;
import org.napile.compiler.injection.text.lang.psi.TextPsiVisitor;
import org.napile.idea.plugin.IdeaInjectionSupport;
import org.napile.idea.plugin.injection.text.highlighter.TextHighlighterColors;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;

/**
 * @author VISTALL
 * @date 21:28/09.11.12
 */
public class TextIdeaInjectionSupport extends IdeaInjectionSupport<TextCodeInjection>
{
	@NotNull
	@Override
	public Class<TextCodeInjection> getInjectionType()
	{
		return TextCodeInjection.class;
	}

	@Nullable
	@Override
	public PsiElementVisitor createVisitorForHighlight(@NotNull final List<HighlightInfo> holder)
	{
		return new TextPsiVisitor()
		{
			@Override
			public void visitElement(PsiElement e)
			{
				e.acceptChildren(this);
			}

			@Override
			public void visitTextInsertElement(TextExpressionInsert e)
			{
				HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION);
				builder.range(e);
				builder.textAttributes(TextHighlighterColors.EXPRESSION_INSERT_COLORS);

				holder.add(builder.create());
			}
		};
	}
}
