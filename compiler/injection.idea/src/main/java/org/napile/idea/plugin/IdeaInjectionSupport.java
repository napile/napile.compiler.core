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

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.injection.CodeInjection;
import org.napile.idea.plugin.highlighter.InjectionSyntaxHighlighter;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.util.NotNullLazyKey;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.util.NotNullFunction;

/**
 * @author VISTALL
 * @since 17:58/27.10.12
 */
public abstract class IdeaInjectionSupport<T extends CodeInjection>
{
	public static final IdeaInjectionSupport<CodeInjection> NOT_SUPPORTED_INJECTION = new IdeaInjectionSupport<CodeInjection>()
	{
		@NotNull
		@Override
		public Class<CodeInjection> getInjectionType()
		{
			return CodeInjection.class;
		}
	};

	public static final NotNullLazyKey<IdeaInjectionSupport<?>, CodeInjection> IDEA_SUPPORT = NotNullLazyKey.create("idea-support", new NotNullFunction<CodeInjection, IdeaInjectionSupport<?>>()
	{
		@NotNull
		@Override
		public IdeaInjectionSupport<?> fun(CodeInjection dom)
		{
			IdeaInjectionSupport injectionSupport = null;
			for(IdeaInjectionSupport it : IdeaInjectionSupportManager.getInstance())
				if(it.getInjectionType() == dom.getClass())
				{
					injectionSupport = it;
					break;
				}

			return injectionSupport == null ? NOT_SUPPORTED_INJECTION : injectionSupport;
		}
	});

	public static final NotNullLazyKey<InjectionSyntaxHighlighter, CodeInjection> SYNTAX_HIGHLIGHTER = NotNullLazyKey.create("syntax-highlighter", new NotNullFunction<CodeInjection, InjectionSyntaxHighlighter>()
	{
		@NotNull
		@Override
		public InjectionSyntaxHighlighter fun(CodeInjection dom)
		{
			return IDEA_SUPPORT.getValue(dom).createSyntaxHighlighter();
		}
	});


	@NotNull
	public abstract Class<T> getInjectionType();

	@NotNull
	public InjectionSyntaxHighlighter createSyntaxHighlighter()
	{
		return InjectionSyntaxHighlighter.DEFAULT;
	}

	@Nullable
	public PsiElementVisitor createVisitorForHighlight(@NotNull Collection<HighlightInfo> holder)
	{
		return null;
	}
}
