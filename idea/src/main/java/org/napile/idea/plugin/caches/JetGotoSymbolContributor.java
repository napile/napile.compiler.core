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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.idea.plugin.stubindex.NapileIndexKeys;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.ArrayUtil;

/**
 * @author Nikolay Krasko
 */
public class JetGotoSymbolContributor implements ChooseByNameContributor
{
	@NotNull
	@Override
	public String[] getNames(Project project, boolean includeNonProjectItems)
	{
		final Collection<String> it1 = StubIndex.getInstance().getAllKeys(NapileIndexKeys.METHODS_SHORT_NAME_KEY, project);
		final Collection<String> it2 = StubIndex.getInstance().getAllKeys(NapileIndexKeys.VARIABLES_SHORT_NAME_KEY, project);
		final Collection<String> it3 = StubIndex.getInstance().getAllKeys(NapileIndexKeys.MACROS_SHORT_NAME_KEY, project);

		List<String> list = new ArrayList<String>(it1.size() + it2.size() + it3.size());
		list.addAll(it1);
		list.addAll(it2);
		list.addAll(it3);
		return ArrayUtil.toStringArray(list);
	}

	@NotNull
	@Override
	public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems)
	{
		final GlobalSearchScope scope = includeNonProjectItems ? GlobalSearchScope.allScope(project) : GlobalSearchScope.projectScope(project);

		final Collection<? extends NavigationItem> it1 = StubIndex.getInstance().get(NapileIndexKeys.METHODS_SHORT_NAME_KEY, name, project, scope);
		final Collection<? extends NavigationItem> it2 = StubIndex.getInstance().get(NapileIndexKeys.MACROS_SHORT_NAME_KEY, name, project, scope);
		final Collection<? extends NavigationItem> it3 = StubIndex.getInstance().get(NapileIndexKeys.VARIABLES_SHORT_NAME_KEY, name, project, scope);

		List<NavigationItem> symbols = new ArrayList<NavigationItem>(it1.size() + it2.size() + it3.size());
		symbols.addAll(it1);
		symbols.addAll(it2);
		symbols.addAll(it3);

		final List<NavigationItem> items = new ArrayList<NavigationItem>(Collections2.filter(symbols, Predicates.notNull()));
		return ArrayUtil.toObjectArray(items, NavigationItem.class);
	}
}
