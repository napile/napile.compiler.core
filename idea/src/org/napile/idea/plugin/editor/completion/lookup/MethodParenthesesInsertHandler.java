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

package org.napile.idea.plugin.editor.completion.lookup;

import org.napile.compiler.lang.descriptors.MethodDescriptor;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;

/**
 * @author VISTALL
 * @since 12:16/07.02.13
 */
public class MethodParenthesesInsertHandler extends ParenthesesInsertHandler<LookupElement>
{
	private final MethodDescriptor methodDescriptor;

	public MethodParenthesesInsertHandler(MethodDescriptor methodDescriptor)
	{
		this.methodDescriptor = methodDescriptor;
	}

	@Override
	protected boolean placeCaretInsideParentheses(InsertionContext context, LookupElement item)
	{
		return !methodDescriptor.getValueParameters().isEmpty();
	}
}
