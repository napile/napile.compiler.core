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

package org.napile.idea.plugin.psi.file;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.psi.NXmlFileImpl;
import com.intellij.openapi.fileTypes.BinaryFileDecompiler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;

/**
 * @author VISTALL
 * @date 18:11/09.10.12
 */
public class NXmlFileDecompiler implements BinaryFileDecompiler
{
	@NotNull
	@Override
	public CharSequence decompile(VirtualFile virtualFile)
	{
		final Project[] projects = ProjectManager.getInstance().getOpenProjects();
		if(projects.length == 0)
			return "";

		final Project project = projects[0];
		return NXmlFileImpl.decompile(PsiManager.getInstance(project), virtualFile);
	}
}
