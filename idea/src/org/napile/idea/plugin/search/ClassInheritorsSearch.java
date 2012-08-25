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

package org.napile.idea.plugin.search;

import gnu.trove.THashSet;

import java.lang.ref.Reference;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileClassOrObject;
import org.napile.compiler.lang.psi.NapileObjectDeclaration;
import org.napile.compiler.lang.resolve.NapileClassResolver;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lang.types.InheritorUtil;
import org.napile.compiler.lexer.JetTokens;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiBundle;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchScopeUtil;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ExtensibleQueryFactory;
import com.intellij.reference.SoftReference;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import com.intellij.util.QueryExecutor;
import com.intellij.util.containers.Stack;

/**
 * @author max
 */
public class ClassInheritorsSearch extends ExtensibleQueryFactory<NapileClassOrObject, ClassInheritorsSearch.SearchParameters>
{
	private static final Logger LOG = Logger.getInstance(ClassInheritorsSearch.class);

	public static final ClassInheritorsSearch INSTANCE = new ClassInheritorsSearch();

	static
	{
		INSTANCE.registerExecutor(new QueryExecutor<NapileClassOrObject, SearchParameters>()
		{
			@Override
			public boolean execute(@NotNull final SearchParameters parameters, @NotNull final Processor<NapileClassOrObject> consumer)
			{
				final NapileClassOrObject baseClass = parameters.getClassToProcess();
				final SearchScope searchScope = parameters.getScope();

				LOG.assertTrue(searchScope != null);

				ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();
				if(progress != null)
				{
					progress.pushState();
					String className = ApplicationManager.getApplication().runReadAction(new Computable<String>()
					{
						@Override
						public String compute()
						{
							return baseClass.getName();
						}
					});
					progress.setText(className != null ? PsiBundle.message("psi.search.inheritors.of.class.progress", className) : PsiBundle.message("psi.search.inheritors.progress"));
				}

				boolean result = processInheritors(consumer, baseClass, searchScope, parameters);

				if(progress != null)
				{
					progress.popState();
				}

				return result;
			}
		});
	}

	public static class SearchParameters
	{
		private final NapileClassOrObject myClass;
		private final SearchScope myScope;
		private final boolean myCheckDeep;
		private final boolean myCheckInheritance;
		private final boolean myIncludeAnonymous;
		private final Condition<String> myNameCondition;

		public SearchParameters(@NotNull final NapileClassOrObject aClass, @NotNull SearchScope scope, final boolean checkDeep, final boolean checkInheritance, boolean includeAnonymous)
		{
			this(aClass, scope, checkDeep, checkInheritance, includeAnonymous, Condition.TRUE);
		}

		public SearchParameters(@NotNull final NapileClassOrObject aClass, @NotNull SearchScope scope, final boolean checkDeep, final boolean checkInheritance, boolean includeAnonymous, @NotNull final Condition<String> nameCondition)
		{
			myClass = aClass;
			myScope = scope;
			myCheckDeep = checkDeep;
			myCheckInheritance = checkInheritance;
			myIncludeAnonymous = includeAnonymous;
			myNameCondition = nameCondition;
		}

		@NotNull
		public NapileClassOrObject getClassToProcess()
		{
			return myClass;
		}

		@NotNull
		public Condition<String> getNameCondition()
		{
			return myNameCondition;
		}

		public boolean isCheckDeep()
		{
			return myCheckDeep;
		}

		public SearchScope getScope()
		{
			return myScope;
		}

		public boolean isCheckInheritance()
		{
			return myCheckInheritance;
		}

		public boolean isIncludeAnonymous()
		{
			return myIncludeAnonymous;
		}
	}

	private ClassInheritorsSearch()
	{
		super("org.napile.napile4idea");
	}

	public static Query<NapileClassOrObject> search(@NotNull final NapileClassOrObject aClass, @NotNull SearchScope scope, final boolean checkDeep, final boolean checkInheritance, boolean includeAnonymous)
	{
		return search(new SearchParameters(aClass, scope, checkDeep, checkInheritance, includeAnonymous));
	}

	public static Query<NapileClassOrObject> search(@NotNull SearchParameters parameters)
	{
		return INSTANCE.createQuery(parameters);
	}

	public static Query<NapileClassOrObject> search(@NotNull final NapileClassOrObject aClass, @NotNull SearchScope scope, final boolean checkDeep, final boolean checkInheritance)
	{
		return search(aClass, scope, checkDeep, checkInheritance, true);
	}

	public static Query<NapileClassOrObject> search(@NotNull final NapileClassOrObject aClass, @NotNull SearchScope scope, final boolean checkDeep)
	{
		return search(aClass, scope, checkDeep, true);
	}

	public static Query<NapileClassOrObject> search(@NotNull final NapileClassOrObject aClass, final boolean checkDeep)
	{
		return search(aClass, aClass.getUseScope(), checkDeep);
	}

	public static Query<NapileClassOrObject> search(@NotNull NapileClassOrObject aClass)
	{
		return search(aClass, true);
	}

	private static boolean processInheritors(@NotNull final Processor<NapileClassOrObject> consumer, @NotNull final NapileClassOrObject baseClass, @NotNull final SearchScope searchScope, @NotNull final SearchParameters parameters)
	{
		if(baseClass instanceof PsiAnonymousClass || isFinal(baseClass))
			return true;

		final FqName qname = ApplicationManager.getApplication().runReadAction(new Computable<FqName>()
		{
			@Override
			public FqName compute()
			{
				return baseClass.getFqName();
			}
		});

		final Ref<NapileClassOrObject> currentBase = Ref.create(null);
		final Stack<Pair<Reference<NapileClassOrObject>, FqName>> stack = new Stack<Pair<Reference<NapileClassOrObject>, FqName>>();
		// there are two sets for memory optimization: it's cheaper to hold FQN than NapileClassOrObject
		final Set<FqName> processedFqns = new THashSet<FqName>(); // FQN of processed classes if the class has one
		final Set<NapileClassOrObject> processed = new THashSet<NapileClassOrObject>();   // processed classes without FQN (e.g. anonymous)

		final Processor<NapileClassOrObject> processor = new Processor<NapileClassOrObject>()
		{
			@Override
			public boolean process(final NapileClassOrObject candidate)
			{
				ProgressManager.checkCanceled();

				final Ref<Boolean> result = new Ref<Boolean>();
				final FqName[] fqn = new FqName[1];
				ApplicationManager.getApplication().runReadAction(new Runnable()
				{
					@Override
					public void run()
					{
						fqn[0] = candidate.getFqName();
						if(parameters.isCheckInheritance() || parameters.isCheckDeep() && !(candidate instanceof PsiAnonymousClass))
						{
							if(!InheritorUtil.isInheritor(candidate, currentBase.get()))
							{
								result.set(true);
								return;
							}
						}

						if(PsiSearchScopeUtil.isInScope(searchScope, candidate))
						{
							if(candidate instanceof NapileObjectDeclaration)
							{
								result.set(consumer.process(candidate));
							}
							else
							{
								final String name = candidate.getName();
								if(name != null && parameters.getNameCondition().value(name) && !consumer.process(candidate))
									result.set(false);
							}
						}
					}
				});
				if(!result.isNull())
					return result.get().booleanValue();

				if(parameters.isCheckDeep() && !(candidate instanceof NapileObjectDeclaration) && !isFinal(candidate))
				{
					Reference<NapileClassOrObject> ref = fqn[0] == null ? createHardReference(candidate) : new SoftReference<NapileClassOrObject>(candidate);
					stack.push(Pair.create(ref, fqn[0]));
				}

				return true;
			}
		};
		stack.push(Pair.create(createHardReference(baseClass), qname));
		final GlobalSearchScope projectScope = GlobalSearchScope.allScope(baseClass.getProject());
		final NapileClassResolver facade = NapileClassResolver.getInstance(projectScope.getProject());
		while(!stack.isEmpty())
		{
			ProgressManager.checkCanceled();

			Pair<Reference<NapileClassOrObject>, FqName> pair = stack.pop();
			NapileClassOrObject classOrObject = pair.getFirst().get();
			final FqName fqn = pair.getSecond();
			if(classOrObject == null)
			{
				classOrObject = ApplicationManager.getApplication().runReadAction(new Computable<NapileClassOrObject>()
				{
					@Override
					public NapileClassOrObject compute()
					{
						return facade.findClass(fqn, projectScope);
					}
				});
				if(classOrObject == null)
					continue;
			}
			if(fqn == null)
			{
				if(!processed.add(classOrObject))
					continue;
			}
			else
			{
				if(!processedFqns.add(fqn))
					continue;
			}

			currentBase.set(classOrObject);
			if(!DirectClassInheritorsSearch.search(classOrObject, projectScope, parameters.isIncludeAnonymous(), false).forEach(processor))
				return false;
		}
		return true;
	}

	private static Reference<NapileClassOrObject> createHardReference(final NapileClassOrObject candidate)
	{
		return new SoftReference<NapileClassOrObject>(candidate)
		{
			@Override
			public NapileClassOrObject get()
			{
				return candidate;
			}
		};
	}

	private static boolean isFinal(@NotNull final NapileClassOrObject baseClass)
	{
		return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>()
		{
			@Override
			public Boolean compute()
			{
				return Boolean.valueOf(baseClass.hasModifier(JetTokens.FINAL_KEYWORD));
			}
		}).booleanValue();
	}
}
