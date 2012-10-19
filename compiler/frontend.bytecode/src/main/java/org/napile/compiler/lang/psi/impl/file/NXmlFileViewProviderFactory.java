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
import com.intellij.lang.Language;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.FileViewProviderFactory;
import com.intellij.psi.PsiManager;

/**
 * @author VISTALL
 * @date 18:43/09.10.12
 */
public class NXmlFileViewProviderFactory implements FileViewProviderFactory
{
	@Override
	public FileViewProvider createFileViewProvider(@NotNull VirtualFile virtualFile, Language language, @NotNull PsiManager psiManager, boolean b)
	{
		return new NXmlFileViewProvider(psiManager, virtualFile);
	}
}
