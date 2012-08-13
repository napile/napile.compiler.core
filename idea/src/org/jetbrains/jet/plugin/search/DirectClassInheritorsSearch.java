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

package org.jetbrains.jet.plugin.search;

import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ExtensibleQueryFactory;
import com.intellij.util.FilteredQuery;
import com.intellij.util.Query;

/**
 * @author max
 */
public class DirectClassInheritorsSearch extends ExtensibleQueryFactory<JetClassOrObject, DirectClassInheritorsSearch.SearchParameters>
{
	public static DirectClassInheritorsSearch INSTANCE = new DirectClassInheritorsSearch();

	public static class SearchParameters
	{
		private final JetClassOrObject myClass;
		private final SearchScope myScope;
		private final boolean myIncludeAnonymous;
		private final boolean myCheckInheritance;

		public SearchParameters(JetClassOrObject aClass, SearchScope scope, boolean includeAnonymous, boolean checkInheritance)
		{
			myClass = aClass;
			myScope = scope;
			myIncludeAnonymous = includeAnonymous;
			myCheckInheritance = checkInheritance;
		}

		public SearchParameters(final JetClassOrObject aClass, SearchScope scope, final boolean includeAnonymous)
		{
			this(aClass, scope, includeAnonymous, true);
		}

		public SearchParameters(final JetClassOrObject aClass, final SearchScope scope)
		{
			this(aClass, scope, true);
		}

		public JetClassOrObject getClassToProcess()
		{
			return myClass;
		}

		public SearchScope getScope()
		{
			return myScope;
		}

		public boolean isCheckInheritance()
		{
			return myCheckInheritance;
		}

		public boolean includeAnonymous()
		{
			return myIncludeAnonymous;
		}
	}

	private DirectClassInheritorsSearch()
	{
		super("org.napile.napile4idea");
	}

	public static Query<JetClassOrObject> search(final JetClassOrObject aClass)
	{
		return search(aClass, GlobalSearchScope.allScope(aClass.getProject()));
	}

	public static Query<JetClassOrObject> search(final JetClassOrObject aClass, SearchScope scope)
	{
		return INSTANCE.createUniqueResultsQuery(new SearchParameters(aClass, scope));
	}

	public static Query<JetClassOrObject> search(final JetClassOrObject aClass, SearchScope scope, boolean includeAnonymous)
	{
		return search(aClass, scope, includeAnonymous, true);
	}

	public static Query<JetClassOrObject> search(final JetClassOrObject aClass, SearchScope scope, boolean includeAnonymous, final boolean checkInheritance)
	{
		final Query<JetClassOrObject> raw = INSTANCE.createUniqueResultsQuery(new SearchParameters(aClass, scope, includeAnonymous, checkInheritance));

		if(!includeAnonymous)
		{
			return new FilteredQuery<JetClassOrObject>(raw, new Condition<JetClassOrObject>()
			{
				@Override
				public boolean value(final JetClassOrObject psiClass)
				{
					return !(psiClass instanceof JetObjectDeclaration);
				}
			});
		}

		return raw;
	}
}
