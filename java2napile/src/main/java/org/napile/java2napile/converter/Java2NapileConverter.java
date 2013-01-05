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

package org.napile.java2napile.converter;

import java.util.ArrayList;
import java.util.List;

import org.napile.java2napile.psi.visitor.ConverterVisitor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.vcsUtil.VcsUtil;

/**
 * @author VISTALL
 * @date 13:13/04.01.13
 */
public class Java2NapileConverter
{
	private final Project project;

	public Java2NapileConverter(Project project)
	{
		this.project = project;
	}

	public void convert(VirtualFile virtualFile, Java2NapileConvertAction convertAction)
	{
		List<VirtualFile> virtualFiles = new ArrayList<VirtualFile>();
		if(virtualFile.isDirectory())
			VcsUtil.collectFiles(virtualFile, virtualFiles, true, false);
		else
			virtualFiles.add(virtualFile);

		PsiManager manager = PsiManager.getInstance(project);
		for(VirtualFile child : virtualFiles)
		{
			PsiFile psiFile = manager.findFile(child);
			assert psiFile != null;

			if(!"java".equals(child.getExtension()))
				continue;

			ConverterVisitor converterVisitor = new ConverterVisitor();
			psiFile.accept(converterVisitor);

			convertAction.storeData(child, converterVisitor.getBuilder());
		}
	}
}
