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
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.util.PluginKeys;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * @author VISTALL
 * @date 15:31/11.01.13
 */
public class ModuleAnalyzerUtil
{
	@NotNull
	public static AnalyzeExhaust analyzeAll(@NotNull final NapileFile file)
	{
		return analyzeFileWithCache(file);
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
	private static AnalyzeExhaust analyzeFileWithCache(@NotNull final NapileFile file)
	{
		Module module = ModuleUtilCore.findModuleForPsiElement(file);
		if(module == null)
		{
			final OrderEntry libraryEntry = LibraryUtil.findLibraryEntry(file.getVirtualFile(), file.getProject());
			if(libraryEntry != null)
			{
				module = libraryEntry.getOwnerModule();
			}
		}

		if(module == null)
		{
			return ModuleAnalyzer.EMPTY;
		}

		ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

		final boolean test = moduleRootManager.getFileIndex().isInTestSourceContent(file.getVirtualFile());

		final ModuleAnalyzer instance = ModuleAnalyzer.getInstance(module);

		return test ? instance.getTestSourceAnalyze() : instance.getSourceAnalyze();
	}
}
