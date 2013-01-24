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

package org.napile.idea.plugin.editor.completion;

import org.jetbrains.annotations.NotNull;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.ProcessingContext;

/**
 * @author VISTALL
 * @date 19:24/24.01.13
 */
public class NapileKeywordCompletionProvider extends CompletionProvider<CompletionParameters>
{
	private final IElementType[] elementTypes;

	public NapileKeywordCompletionProvider(TokenSet tokenSet)
	{
		this(tokenSet.getTypes());
	}

	public NapileKeywordCompletionProvider(IElementType... elementTypes)
	{
		this.elementTypes = elementTypes;
	}

	@Override
	protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result)
	{
		for(IElementType elementType : elementTypes)
		{
			LookupElementBuilder builder = LookupElementBuilder.create(elementType.toString());
			builder = builder.bold();
			builder = builder.withInsertHandler(new InsertHandler<LookupElement>()
			{
				@Override
				public void handleInsert(InsertionContext context, LookupElement item)
				{
					context.getDocument().insertString(context.getEditor().getCaretModel().getOffset(), " ");
					//context.getEditor().getCaretModel().moveToOffset(context.getEditor().getCaretModel().getOffset());
				}
			});
			result.addElement(builder);
		}
	}
}
