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

package org.napile.idea.plugin.run.ui;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NapileClassLike;
import org.napile.idea.plugin.caches.JetShortNamesCache;
import org.napile.idea.plugin.psi.filter.NapileClassFilterWithScope;
import org.napile.idea.plugin.util.RunUtil;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configuration.BrowseModuleValueActionListener;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.ide.util.TreeChooser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ex.MessagesEx;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * @author VISTALL
 * @date 21:38/22.09.12
 */
public abstract class NapileClassBrowser extends BrowseModuleValueActionListener
{
	private final String myTitle;

	public NapileClassBrowser(final Project project, final String title)
	{
		super(project);
		myTitle = title;
	}

	@Nullable
	protected String showDialog()
	{
		final NapileClassFilterWithScope classFilter;
		try
		{
			classFilter = getFilter();
		}
		catch(NoFilterException e)
		{
			final MessagesEx.MessageInfo info = e.getMessageInfo();
			info.showNow();
			return null;
		}
		final TreeChooser<NapileClassLike> dialog = createClassChooser(classFilter);
		configureDialog(dialog);
		dialog.showDialog();
		final NapileClassLike psiClass = dialog.getSelected();
		if(psiClass == null)
			return null;
		onClassChoosen(psiClass);
		return psiClass.getFqName().getFqName();
	}

	protected NapileTreeClassChooserDialog createClassChooser(NapileClassFilterWithScope classFilter)
	{
		return new NapileTreeClassChooserDialog(myTitle, getProject(), classFilter);
	}

	protected abstract NapileClassFilterWithScope getFilter() throws NoFilterException;

	protected void onClassChoosen(final NapileClassLike psiClass)
	{
	}

	private void configureDialog(final TreeChooser<NapileClassLike> dialog)
	{
		final String className = getText();
		final NapileClassLike psiClass = findClass(className);
		if(psiClass == null)
			return;
		final PsiDirectory directory = psiClass.getContainingFile().getContainingDirectory();
		if(directory != null)
			dialog.selectDirectory(directory);
		dialog.select(psiClass);
	}

	protected abstract NapileClassLike findClass(String className);

	@NotNull
	public static NapileClassBrowser createApplicationClassBrowser(final Project project, final Module module)
	{
		final NapileClassFilterWithScope applicationClass = new NapileClassFilterWithScope()
		{
			@Override
			public boolean isAccepted(@NotNull final NapileClassLike aClass)
			{
				return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>()
				{
					@Override
					public Boolean compute()
					{
						return RunUtil.hasClassPoint(aClass);
					}
				});
			}

			@NotNull
			@Override
			public GlobalSearchScope getScope()
			{
				return GlobalSearchScope.allScope(project);
			}
		};
		return new MainClassBrowser(project, module, ExecutionBundle.message("choose.main.class.dialog.title"))
		{
			@Override
			protected NapileClassFilterWithScope createFilter()
			{
				return applicationClass;
			}
		};
	}
	private abstract static class MainClassBrowser extends NapileClassBrowser
	{
		protected final Project myProject;
		protected final Module module;

		public MainClassBrowser(final Project project, final Module module, final String title)
		{
			super(project, title);
			myProject = project;
			this.module = module;
		}

		@Override
		protected NapileClassLike findClass(final String className)
		{
			try
			{
				NapileClassLike[] classes = JetShortNamesCache.getInstance(myProject).getClassesByName(className, getFilter().getScope());
				return classes.length > 0 ? classes[0] : null;
			}
			catch(NoFilterException e)
			{
				return null;
			}
		}

		@Override
		protected NapileClassFilterWithScope getFilter() throws NoFilterException
		{
			final GlobalSearchScope scope;
			if(module == null)
				scope = GlobalSearchScope.allScope(myProject);
			else
				scope = GlobalSearchScope.moduleScope(module);

			final NapileClassFilterWithScope filter = createFilter();
			return new NapileClassFilterWithScope()
			{
				@Override
				public GlobalSearchScope getScope()
				{
					return scope;
				}

				@Override
				public boolean isAccepted(final NapileClassLike aClass)
				{
					return filter == null || filter.isAccepted(aClass);
				}
			};
		}

		protected NapileClassFilterWithScope createFilter()
		{
			return null;
		}
	}

	public static class NoFilterException extends Exception
	{
		private final MessagesEx.MessageInfo myMessageInfo;

		public NoFilterException(final MessagesEx.MessageInfo messageInfo)
		{
			super(messageInfo.getMessage());
			myMessageInfo = messageInfo;
		}

		public MessagesEx.MessageInfo getMessageInfo()
		{
			return myMessageInfo;
		}

		public static NoFilterException moduleDoesntExist(final ConfigurationModuleSelector moduleSelector)
		{
			final Project project = moduleSelector.getProject();
			final String moduleName = moduleSelector.getModuleName();
			return new NoFilterException(new MessagesEx.MessageInfo(project, moduleName.isEmpty() ? "No module selected" : ExecutionBundle.message("module.does.not.exists", moduleName, project.getName()), ExecutionBundle.message("cannot.browse.test.inheritors.dialog.title")));
		}
	}
}
