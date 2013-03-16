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

package org.napile.idea.plugin.injection.regexp;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.injection.regexp.RegexpCodeInjection;
import org.napile.idea.plugin.IdeaInjectionSupport;
import org.napile.idea.plugin.highlighter.InjectionSyntaxHighlighter;
import org.napile.idea.plugin.injection.regexp.highlighter.RegExpHighlighter;

/**
 * @author VISTALL
 * @since 7:36/08.11.12
 */
public class RegExpIdeaInjectionSupport extends IdeaInjectionSupport<RegexpCodeInjection>
{
	@NotNull
	@Override
	public InjectionSyntaxHighlighter createSyntaxHighlighter()
	{
		return new RegExpHighlighter();
	}

	@NotNull
	@Override
	public Class<RegexpCodeInjection> getInjectionType()
	{
		return RegexpCodeInjection.class;
	}
}
