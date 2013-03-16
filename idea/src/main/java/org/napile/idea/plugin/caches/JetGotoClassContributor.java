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

package org.napile.idea.plugin.caches;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.idea.plugin.stubindex.NapileShortClassNameIndex;
import com.intellij.navigation.GotoClassContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * @author Nikolay Krasko
 */
public class JetGotoClassContributor implements GotoClassContributor
{
	@Override
	public String getQualifiedName(NavigationItem item)
	{
		if(item instanceof NapileClass)
			return ((NapileClass) item).getFqName().toString();
		return StringUtils.EMPTY;
	}

	@Override
	public String getQualifiedNameSeparator()
	{
		return ".";
	}

	@NotNull
	@Override
	public String[] getNames(Project project, boolean includeNonProjectItems)
	{
		return NapileClassResolver.getInstance(project).getAllClassNames();
	}

	@NotNull
	@Override
	public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems)
	{
		final GlobalSearchScope scope = GlobalSearchScope.allScope(project);

		Collection<NapileClass> classes = NapileShortClassNameIndex.getInstance().get(name, project, scope);

		return classes.toArray(NapileClass.EMPTY_ARRAY);
	}
}
