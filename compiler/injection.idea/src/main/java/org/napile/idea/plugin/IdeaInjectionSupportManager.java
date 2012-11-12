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

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.injection.CodeInjection;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.openapi.components.ApplicationComponent;

/**
 * @author VISTALL
 * @date 17:59/27.10.12
 */
public class IdeaInjectionSupportManager implements ApplicationComponent
{
	public static IdeaInjectionSupportManager INSTANCE;

	private final List<IdeaInjectionSupport> injectionSupports = new ArrayList<IdeaInjectionSupport>();

	private IdeaInjectionSupportManager()
	{
		INSTANCE = this;
	}

	private void addInjectionSupport(IdeaInjectionSupport ideaInjectionSupport)
	{
		injectionSupports.add(ideaInjectionSupport);
		CodeInjection c = ideaInjectionSupport.getInjectionType();

		LanguageParserDefinitions.INSTANCE.addExplicitExtension(c.getLanguage(), c);
	}

	public List<IdeaInjectionSupport> getInjectionSupports()
	{
		return injectionSupports;
	}

	@Override
	public void initComponent()
	{
		//TODO [VISTALL] rework it
		for(String className : new String[]
		{
				"org.napile.idea.injection.protobuf.PbIdeaInjectionSupport",
				"org.napile.idea.plugin.injection.regexp.RegExpIdeaInjectionSupport",
				"org.napile.idea.plugin.injection.text.TextIdeaInjectionSupport"
		})
			try
			{
				Class<?> clazz = Class.forName(className);

				IdeaInjectionSupport injection = (IdeaInjectionSupport) clazz.newInstance();

				addInjectionSupport(injection);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
	}

	@Override
	public void disposeComponent()
	{
	}

	@NotNull
	@Override
	public String getComponentName()
	{
		return "IdeaInjectionSupportManager";
	}
}
