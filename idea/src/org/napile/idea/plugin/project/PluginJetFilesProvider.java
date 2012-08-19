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

/*
 * @author max
 */
package org.napile.idea.plugin.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.JetFilesProvider;
import org.napile.compiler.plugin.JetFileType;
import com.google.common.collect.Sets;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ModuleFileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;

public class PluginJetFilesProvider extends JetFilesProvider
{
	private final Project project;

	public PluginJetFilesProvider(Project project)
	{
		this.project = project;
	}

	public static final Function<NapileFile, Collection<NapileFile>> WHOLE_PROJECT_DECLARATION_PROVIDER = new Function<NapileFile, Collection<NapileFile>>()
	{

		@Override
		public Collection<NapileFile> fun(final NapileFile rootFile)
		{
			final Project project = rootFile.getProject();
			final Set<NapileFile> files = Sets.newLinkedHashSet();

			Module rootModule = ModuleUtil.findModuleForPsiElement(rootFile);
			if(rootModule != null)
			{
				Set<Module> allModules = new HashSet<Module>();
				ModuleUtil.getDependencies(rootModule, allModules);

				for(Module module : allModules)
				{
					final ModuleFileIndex index = ModuleRootManager.getInstance(module).getFileIndex();
					index.iterateContent(new ContentIterator()
					{
						@Override
						public boolean processFile(VirtualFile file)
						{
							if(file.isDirectory())
								return true;
							if(!index.isInSourceContent(file) && !index.isInTestSourceContent(file))
								return true;

							final FileType fileType = FileTypeManager.getInstance().getFileTypeByFile(file);
							if(fileType != JetFileType.INSTANCE)
								return true;
							PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
							if(psiFile instanceof NapileFile)
							{
								if(rootFile.getOriginalFile() != psiFile)
								{
									files.add((NapileFile) psiFile);
								}
							}
							return true;
						}
					});
				}
			}

			files.add(rootFile);
			return files;
		}
	};

	@NotNull
	@Override
	public Function<NapileFile, Collection<NapileFile>> sampleToAllFilesInModule()
	{
		return WHOLE_PROJECT_DECLARATION_PROVIDER;
	}

	@Override
	@NotNull
	public List<NapileFile> allInScope(final GlobalSearchScope scope)
	{
		final List<NapileFile> answer = new ArrayList<NapileFile>();

		final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
		List<VirtualFile> contentRoots = Arrays.asList(ProjectRootManager.getInstance(project).getContentRoots());
		final PsiManager manager = PsiManager.getInstance(project);

		CompilerPathsEx.visitFiles(contentRoots, new CompilerPathsEx.FileVisitor()
		{
			@Override
			protected void acceptFile(VirtualFile file, String fileRoot, String filePath)
			{
				final FileType fileType = fileTypeManager.getFileTypeByFile(file);
				if(fileType != JetFileType.INSTANCE)
					return;

				if(scope.accept(file))
				{
					final PsiFile psiFile = manager.findFile(file);
					if(psiFile instanceof NapileFile)
					{
						answer.add((NapileFile) psiFile);
					}
				}
			}
		});

		return answer;
	}
}
