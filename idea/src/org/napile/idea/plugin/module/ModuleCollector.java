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

import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.NXmlFileType;
import org.napile.compiler.NapileFileType;
import org.napile.compiler.analyzer.AnalyzeContext;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.idea.plugin.module.type.NapileModuleType;
import com.google.common.collect.Sets;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleFileIndex;
import com.intellij.openapi.roots.ModuleOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleSourceOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.RootPolicy;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.SmartList;

/**
 * @author VISTALL
 * @date 15:31/11.01.13
 */
public class ModuleCollector
{
	public static AnalyzeContext createAnalyzeContext(final @NotNull NapileFile rootFile)
	{
		VirtualFile virtualFile = rootFile.getVirtualFile();
		if(virtualFile == null)
			return AnalyzeContext.EMPTY;

		Module module = ModuleUtilCore.findModuleForPsiElement(rootFile);

		// TODO [VISTALL] rework it
		if(module == null && virtualFile.getFileType() == NXmlFileType.INSTANCE)
		{
			VirtualFile lib = null;
			VirtualFile parent = virtualFile.getParent();
			while(parent != null)
			{
				if("nzip".equals(parent.getExtension()))
				{
					lib = parent;
					break;
				}
				parent = parent.getParent();
			}

			return new AnalyzeContext(Collections.<NapileFile>emptyList(), Collections.singletonList(lib == null ? virtualFile : lib), Collections.<VirtualFile>emptyList());
		}

		if(module == null || ModuleType.get(module) != NapileModuleType.getInstance())
		{
			return AnalyzeContext.EMPTY;
		}

		ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

		final boolean test = moduleRootManager.getFileIndex().isInTestSourceContent(virtualFile);

		return getAnalyzeContext(rootFile.getProject(), rootFile, test, module);
	}

	public static AnalyzeContext getAnalyzeContext(@NotNull final Project project, @Nullable final NapileFile rootFile, final boolean test, @NotNull Module module)
	{
		final Set<NapileFile> analyzeFiles = Sets.newLinkedHashSet();
		final SmartList<VirtualFile> bootpath = new SmartList<VirtualFile>();
		final SmartList<VirtualFile> classpath = new SmartList<VirtualFile>();

		ModuleRootManager rootManager = ModuleRootManager.getInstance(module);

		for(OrderEntry orderEntry : rootManager.getOrderEntries())
		{
			orderEntry.accept(new RootPolicy<Object>()
			{
				@Override
				public Object visitLibraryOrderEntry(LibraryOrderEntry libraryOrderEntry, Object value)
				{
					for(VirtualFile v : libraryOrderEntry.getFiles(OrderRootType.CLASSES))
					{
						String name = v.getName();
						if(name.equals("napile.lang.nzip"))   //TODO [VISTALL] hardcode
							bootpath.add(v);
						else
							classpath.add(v);
					}
					return null;
				}

				@Override
				public Object visitModuleSourceOrderEntry(ModuleSourceOrderEntry moduleSourceOrderEntry, Object value)
				{
					collectSourcesInModule(project, moduleSourceOrderEntry.getOwnerModule(), test, analyzeFiles, rootFile);
					return null;
				}

				@Override
				public Object visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, Object value)
				{
					Module module = moduleOrderEntry.getModule();
					if(module == null || !moduleOrderEntry.isExported())
						return null;

					collectSourcesInModule(project, module, test, analyzeFiles, rootFile);
					return null;
				}
			}, null);
		}

		if(rootFile != null)
			analyzeFiles.add(rootFile);

		return new AnalyzeContext(analyzeFiles, bootpath, classpath);
	}

	private static void collectSourcesInModule(@NotNull final Project project, @NotNull final Module module, final boolean test, @NotNull final Set<NapileFile> files, final NapileFile rootFile)
	{
		if(ModuleType.get(module) != NapileModuleType.getInstance())
			return;

		final ModuleFileIndex index = ModuleRootManager.getInstance(module).getFileIndex();
		index.iterateContent(new ContentIterator()
		{
			@Override
			public boolean processFile(VirtualFile file)
			{
				if(file.isDirectory())
					return true;

				if(!test && index.isInTestSourceContent(file))
					return true;

				final FileType fileType = FileTypeManager.getInstance().getFileTypeByFile(file);
				if(fileType != NapileFileType.INSTANCE)
					return true;

				PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
				if(psiFile instanceof NapileFile)
				{
					if(rootFile == null || rootFile.getOriginalFile() != psiFile)
						files.add((NapileFile) psiFile);
				}
				return true;
			}
		});
	}
}
