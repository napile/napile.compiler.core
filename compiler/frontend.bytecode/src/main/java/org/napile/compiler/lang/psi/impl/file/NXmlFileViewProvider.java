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

package org.napile.compiler.lang.psi.impl.file;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.NapileLanguage;
import org.napile.compiler.lang.psi.impl.NXmlFileImpl;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.impl.PsiManagerImpl;

/**
 * @author VISTALL
 * @date 18:44/09.10.12
 */
public class NXmlFileViewProvider extends SingleRootFileViewProvider
{
	public NXmlFileViewProvider(@NotNull PsiManager manager, @NotNull VirtualFile virtualFile)
	{
		super(manager, virtualFile, true, NapileLanguage.INSTANCE);
	}

	@Override
	protected PsiFile createFile(@NotNull final Project project, @NotNull final VirtualFile vFile, @NotNull final FileType fileType)
	{
		final FileIndexFacade fileIndex = ServiceManager.getService(project, FileIndexFacade.class);
		if(fileIndex.isInLibraryClasses(vFile) || !fileIndex.isInSource(vFile))
			return new NXmlFileImpl((PsiManagerImpl) PsiManager.getInstance(project), this);
		return null;
	}
}
