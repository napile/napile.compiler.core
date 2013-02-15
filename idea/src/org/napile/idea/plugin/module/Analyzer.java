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

package org.napile.idea.plugin.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.NapileFileType;
import org.napile.compiler.analyzer.AnalyzeContext;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.analyzer.AnalyzerFacade;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.util.PluginKeys;
import com.google.common.base.Predicates;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;

/**
 * @author VISTALL
 * @date 15:31/11.01.13
 */
public class Analyzer
{
	private static final Logger LOG = Logger.getInstance(Analyzer.class);

	private static final Key<CachedValue<AnalyzeExhaust>> ANALYZE_EXHAUST_FULL = Key.create("ANALYZE_EXHAUST_FULL");

	private static final AnalyzeExhaust EMPTY = AnalyzeExhaust.success(BindingContext.EMPTY, BodiesResolveContext.EMPTY, null);
	private static final Object lock = new Object();

	@NotNull
	public static AnalyzeExhaust analyze(@NotNull final NapileFile file)
	{
		return analyzeFileWithCache(file, ModuleCollector.createAnalyzeContext(file, false));
	}

	@NotNull
	public static AnalyzeExhaust analyzeAll(@NotNull final NapileFile file)
	{
		return analyzeFileWithCache(file, ModuleCollector.createAnalyzeContext(file, true));
	}

	public static <T extends DeclarationDescriptor> T getDescriptorOrAnalyze(@NotNull NapileElement napileElement)
	{
		DeclarationDescriptor declarationDescriptor = napileElement.getUserData(PluginKeys.DESCRIPTOR_KEY);
		if(declarationDescriptor == null)
			analyzeAll(napileElement.getContainingFile());
		declarationDescriptor = napileElement.getUserData(PluginKeys.DESCRIPTOR_KEY);
		return ((T) declarationDescriptor);
	}

	@NotNull
	public static List<NapileFile> getFilesInScope(PsiElement element, final GlobalSearchScope searchScopes)
	{
		final List<NapileFile> answer = new ArrayList<NapileFile>();

		final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
		List<VirtualFile> contentRoots = Arrays.asList(ProjectRootManager.getInstance(element.getProject()).getContentRoots());
		final PsiManager manager = PsiManager.getInstance(element.getProject());

		CompilerPathsEx.visitFiles(contentRoots, new CompilerPathsEx.FileVisitor()
		{
			@Override
			protected void acceptFile(VirtualFile file, String fileRoot, String filePath)
			{
				final FileType fileType = fileTypeManager.getFileTypeByFile(file);
				if(fileType != NapileFileType.INSTANCE)
					return;

				if(searchScopes.accept(file))
				{
					final PsiFile psiFile = manager.findFile(file);
					if(psiFile instanceof NapileFile)
						answer.add((NapileFile) psiFile);
				}
			}
		});
		return answer;
	}

	@NotNull
	private static AnalyzeExhaust analyzeFileWithCache(@NotNull final NapileFile file, @NotNull final AnalyzeContext context)
	{
		final Project project = file.getProject();

		// Need lock for getValue(), because parallel threads can start evaluation of compute() simultaneously
		//FIXME [VISTALL] if we use module based analyzer - wy it need for all?
		synchronized(lock)
		{
			CachedValue<AnalyzeExhaust> bindingContextCachedValue = file.getUserData(ANALYZE_EXHAUST_FULL);
			if(bindingContextCachedValue == null)
			{
				bindingContextCachedValue = CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<AnalyzeExhaust>()
				{
					@Override
					public Result<AnalyzeExhaust> compute()
					{
						try
						{
							AnalyzeExhaust exhaust = AnalyzerFacade.analyzeFiles(project, context, Predicates.<NapileFile>equalTo(file));

							return new Result<AnalyzeExhaust>(exhaust, PsiModificationTracker.MODIFICATION_COUNT);
						}
						catch(ProcessCanceledException e)
						{
							throw e;
						}
						catch(Throwable e)
						{
							LOG.error(e);
							return new Result<AnalyzeExhaust>(EMPTY, PsiModificationTracker.MODIFICATION_COUNT);
						}
					}
				}, false);

				file.putUserData(ANALYZE_EXHAUST_FULL, bindingContextCachedValue);
			}

			return bindingContextCachedValue.getValue();
		}
	}
}
