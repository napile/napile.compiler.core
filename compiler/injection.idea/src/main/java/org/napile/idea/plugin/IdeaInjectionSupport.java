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

package org.napile.idea.plugin;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.injection.CodeInjection;
import org.napile.idea.plugin.highlighter.InjectionSyntaxHighlighter;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NotNullLazyKey;
import com.intellij.util.NotNullFunction;

/**
 * @author VISTALL
 * @date 17:58/27.10.12
 */
public interface IdeaInjectionSupport<T extends CodeInjection>
{
	Key<IdeaInjectionSupport<?>> IDEA_SUPPORT = Key.create("idea-support");

	NotNullLazyKey<InjectionSyntaxHighlighter, CodeInjection> SYNTAX_HIGHLIGHTER = NotNullLazyKey.create("syntax-highlighter", new NotNullFunction<CodeInjection, InjectionSyntaxHighlighter>()
	{
		@NotNull
		@Override
		public InjectionSyntaxHighlighter fun(CodeInjection dom)
		{
			IdeaInjectionSupport support = IDEA_SUPPORT.get(dom);
			if(support == null)
				return InjectionSyntaxHighlighter.EMPTY;
			return support.createSyntaxHighlighter();
		}
	});

	@NotNull
	InjectionSyntaxHighlighter createSyntaxHighlighter();

	@NotNull
	Class<T> getInjectionType();
}
