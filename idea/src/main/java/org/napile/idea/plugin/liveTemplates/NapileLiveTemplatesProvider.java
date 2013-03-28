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

package org.napile.idea.plugin.liveTemplates;

import org.jetbrains.annotations.Nullable;
import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider;

/**
 * @author VISTALL
 * @since 17:32/28.03.13
 */
public class NapileLiveTemplatesProvider implements DefaultLiveTemplatesProvider
{
	@Override
	public String[] getDefaultLiveTemplateFiles()
	{
		return new String[] {"liveTemplates/Napile"};
	}

	@Nullable
	@Override
	public String[] getHiddenLiveTemplateFiles()
	{
		return null;
	}
}
