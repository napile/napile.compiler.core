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
import org.napile.compiler.lang.resolve.BindingTraceKeys;
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
 * @since 15:31/11.01.13
 */
public class ModuleAnalyzerUtil
{
	@NotNull
	public static AnalyzeExhaust lastAnalyze(@NotNull final NapileFile file)
	{
		return analyzeOrGet(file, false);
	}

	/**
	 * Return new AnalyzeExhaust
	 * @param file
	 * @return
	 */
	@NotNull
	public static AnalyzeExhaust analyze(@NotNull final NapileFile file)
	{
		return analyzeOrGet(file, true);
	}

	public static <T extends DeclarationDescriptor> T getDescriptorOrAnalyze(@NotNull NapileElement napileElement)
	{
		AnalyzeExhaust analyzeExhaust = lastAnalyze(napileElement.getContainingFile());

		DeclarationDescriptor declarationDescriptor = analyzeExhaust.getBindingTrace().get(BindingTraceKeys.DECLARATION_TO_DESCRIPTOR, napileElement);

		return (T) declarationDescriptor;
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
	private static AnalyzeExhaust analyzeOrGet(@NotNull final NapileFile file, boolean updateIfNeed)
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

		final VirtualFile virtualFile = file.getVirtualFile();
		if(virtualFile == null)
		{
			return ModuleAnalyzer.EMPTY;
		}
		final boolean test = moduleRootManager.getFileIndex().isInTestSourceContent(virtualFile);

		final ModuleAnalyzer instance = ModuleAnalyzer.getInstance(module);

		return test ? instance.getTestSourceAnalyze(updateIfNeed) : instance.getSourceAnalyze(updateIfNeed);
	}
}
