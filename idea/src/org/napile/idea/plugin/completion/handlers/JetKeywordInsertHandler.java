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

package org.napile.idea.plugin.completion.handlers;

import java.util.Set;

import org.napile.compiler.lang.psi.NapileMethod;
import org.napile.compiler.lang.lexer.NapileTokens;
import com.google.common.collect.Sets;
import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author Nikolay Krasko
 */
public class JetKeywordInsertHandler implements InsertHandler<LookupElement>
{

	private final static Set<String> NO_SPACE_AFTER = Sets.newHashSet(NapileTokens.THIS_KEYWORD.toString(), NapileTokens.SUPER_KEYWORD.toString(), NapileTokens.THIS_KEYWORD.toString(), NapileTokens.FALSE_KEYWORD.toString(), NapileTokens.NULL_KEYWORD.toString(), NapileTokens.BREAK_KEYWORD.toString(), NapileTokens.CONTINUE_KEYWORD.toString());

	@Override
	public void handleInsert(InsertionContext context, LookupElement item)
	{
		String keyword = item.getLookupString();

		if(NO_SPACE_AFTER.contains(keyword))
		{
			return;
		}

		if(keyword.equals(NapileTokens.RETURN_KEYWORD.toString()))
		{
			PsiElement element = context.getFile().findElementAt(context.getStartOffset());
			if(element != null)
			{
				NapileMethod napileMethod = PsiTreeUtil.getParentOfType(element, NapileMethod.class);
				if(napileMethod != null)
				{
					if(!napileMethod.hasDeclaredReturnType())
					{
						// No space for void function
						return;
					}
				}
			}
		}

		// Add space after keyword
		context.setAddCompletionChar(false);
		final TailType tailType = TailType.SPACE;
		tailType.processTail(context.getEditor(), context.getTailOffset());
	}
}
