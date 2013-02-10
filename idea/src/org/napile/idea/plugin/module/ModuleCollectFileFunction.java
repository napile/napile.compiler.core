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

import java.util.Collection;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.NapileFileType;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.idea.plugin.module.type.NapileModuleType;
import com.google.common.collect.Sets;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
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
import com.intellij.util.Function;

/**
 * @author VISTALL
 * @date 15:31/11.01.13
 */
public class ModuleCollectFileFunction implements Function<NapileFile, Collection<NapileFile>>
{
	private final Module module;
	private final boolean test;

	public ModuleCollectFileFunction(Module module, boolean test)
	{
		this.module = module;
		this.test = test;
	}

	@Override
	public Collection<NapileFile> fun(final NapileFile rootFile)
	{
		final Set<NapileFile> files = Sets.newLinkedHashSet();

		ModuleRootManager rootManager = ModuleRootManager.getInstance(module);

		for(OrderEntry orderEntry : rootManager.getOrderEntries())
		{
			orderEntry.accept(new RootPolicy<Object>()
			{
				@Override
				public Object visitLibraryOrderEntry(LibraryOrderEntry libraryOrderEntry, Object value)
				{
					PsiManager manager = PsiManager.getInstance(rootFile.getProject());
					for(VirtualFile v : libraryOrderEntry.getFiles(OrderRootType.CLASSES))
					{
						System.out.println(manager.findFile(v));
					}
					//if(!libraryOrderEntry.isExported())
					//	return null;
					return null;
				}

				@Override
				public Object visitModuleSourceOrderEntry(ModuleSourceOrderEntry moduleSourceOrderEntry, Object value)
				{
					collectSourcesInModule(moduleSourceOrderEntry.getOwnerModule(), files, rootFile);
					return null;
				}

				@Override
				public Object visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, Object value)
				{
					Module module = moduleOrderEntry.getModule();
					if(module == null || !moduleOrderEntry.isExported())
						return null;
					collectSourcesInModule(module, files, rootFile);
					return null;
				}
			}, null);
		}

		files.add(rootFile);
		return files;
	}

	private void collectSourcesInModule(@NotNull final Module module, @NotNull final Set<NapileFile> files, @NotNull final NapileFile rootFile)
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

				PsiFile psiFile = PsiManager.getInstance(rootFile.getProject()).findFile(file);
				if(psiFile instanceof NapileFile)
				{
					if(rootFile.getOriginalFile() != psiFile)
						files.add((NapileFile) psiFile);
				}
				return true;
			}
		});
	}
}
