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

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileAnonymClass;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileLikeClass;
import org.napile.compiler.lang.psi.NapileNamedDeclaration;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.idea.plugin.stubindex.JetShortClassNameIndex;
import com.intellij.navigation.GotoClassContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;

/**
 * @author Nikolay Krasko
 */
public class JetGotoClassContributor implements GotoClassContributor
{
	@Override
	public String getQualifiedName(NavigationItem item)
	{
		if(item instanceof NapileLikeClass)
			return ((NapileLikeClass) item).getFqName().toString();
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
		return JetShortNamesCache.getInstance(project).getAllClassNames();
	}

	@NotNull
	@Override
	public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems)
	{
		final GlobalSearchScope scope = GlobalSearchScope.allScope(project);

		ArrayList<NavigationItem> items = new ArrayList<NavigationItem>();
		Collection<NapileLikeClass> classesOrObjects = JetShortClassNameIndex.getInstance().get(name, project, scope);

		for(NapileLikeClass classOrObject : classesOrObjects)
		{
			if(classOrObject instanceof NapileNamedDeclaration)
			{
				FqName fqName = NapilePsiUtil.getFQName((NapileNamedDeclaration) classOrObject);
				if(fqName == null)
					continue;

				if(classOrObject instanceof NapileAnonymClass)
				{
					// items.add((NapileObjectDeclaration) classOrObject);
				}
				else if(classOrObject instanceof NapileClass)
				{
					items.add((NapileClass) classOrObject);
				}
				else
				{
					assert false;
				}
			}
		}

		return ArrayUtil.toObjectArray(items, NavigationItem.class);
	}
}
