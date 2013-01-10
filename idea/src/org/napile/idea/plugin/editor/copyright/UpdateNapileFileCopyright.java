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

package org.napile.idea.plugin.editor.copyright;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileFile;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.options.JavaOptions;
import com.maddyhome.idea.copyright.psi.UpdatePsiFileCopyright;

/**
 * @author VISTALL
 * @date 12:23/10.01.13
 */
public class UpdateNapileFileCopyright extends UpdatePsiFileCopyright
{
	public UpdateNapileFileCopyright(Project project, Module module, VirtualFile root, CopyrightProfile options)
	{
		super(project, module, root, options);
	}

	@Override
	protected boolean accept()
	{
		return getFile() instanceof NapileFile;
	}

	@Override
	protected void scanFile()
	{
		NapileFile file = (NapileFile) getFile();
		PsiElement pkg = getPackageStatement();
		PsiElement[] imports = getImportsList();
		NapileClass topclass = null;
		NapileClass[] classes = file.getDeclarations();
		if(classes.length > 0)
			topclass = classes[0];

		PsiElement first = file.getFirstChild();

		int location = JavaOptions.LOCATION_BEFORE_PACKAGE;  //FIXME [VISTALL] hardcode because bad copyright API
		if(pkg != null)
		{
			checkComments(first, pkg, true);
			first = pkg;
		}
		else if(location == JavaOptions.LOCATION_BEFORE_PACKAGE)
		{
			location = JavaOptions.LOCATION_BEFORE_IMPORT;
		}

		if(imports != null && imports.length > 0)
		{
			checkComments(first, imports[0], location == JavaOptions.LOCATION_BEFORE_IMPORT);
			first = imports[0];
		}
		else if(location == JavaOptions.LOCATION_BEFORE_IMPORT)
		{
			location = JavaOptions.LOCATION_BEFORE_CLASS;
		}

		if(topclass != null)
		{
			final List<PsiComment> comments = new ArrayList<PsiComment>();
			collectComments(first, topclass, comments);
			collectComments(topclass.getFirstChild(), topclass.getModifierList(), comments);
			checkCommentsForTopClass(topclass, location, comments);
		}
		else if(location == JavaOptions.LOCATION_BEFORE_CLASS)
		{
			// no package, no imports, no top level class
		}
	}

	protected void checkCommentsForTopClass(NapileClass topclass, int location, List<PsiComment> comments)
	{
		checkComments(topclass.getModifierList(), location == JavaOptions.LOCATION_BEFORE_CLASS, comments);
	}

	@Nullable
	protected PsiElement[] getImportsList()
	{
		final NapileFile napilePsiFile = (NapileFile) getFile();
		assert napilePsiFile != null;
		return napilePsiFile.getImportDirectives().toArray(PsiElement.EMPTY_ARRAY);
	}

	@Nullable
	protected PsiElement getPackageStatement()
	{
		NapileFile javaFile = (NapileFile) getFile();
		assert javaFile != null;
		return javaFile.getNamespaceHeader();
	}
}
