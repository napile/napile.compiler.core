/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.napile.idea.plugin.stubindex.resolve;

import java.util.Collection;
import java.util.HashSet;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileNamedFunction;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.resolve.lazy.PackageMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.napile.idea.plugin.stubindex.JetTopLevelFunctionsFqnNameIndex;
import org.napile.idea.plugin.stubindex.JetTopLevelPropertiesFqnNameIndex;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * @author Nikolay Krasko
 */
public class StubPackageMemberDeclarationProvider extends AbstractStubDeclarationProvider implements PackageMemberDeclarationProvider
{
	@NotNull
	private final FqName fqName;
	@NotNull
	private final Project project;

	public StubPackageMemberDeclarationProvider(@NotNull FqName fqName, @NotNull Project project)
	{
		this.fqName = fqName;
		this.project = project;
	}

	@Override
	public boolean isPackageDeclared(@NotNull Name name)
	{
		FqName childFqName = fqName.child(name);

		for(NapileFile jetFile : getJetFiles())
		{
			if(childFqName.toString().equals(jetFile.getPackageName()))
			{
				return true;
			}
		}

		return false;
	}

	@NotNull
	@Override
	public Collection<NapileNamedFunction> getFunctionDeclarations(@NotNull Name name)
	{
		return JetTopLevelFunctionsFqnNameIndex.getInstance().get(fqName.child(name).toString(), project, GlobalSearchScope.allScope(project));
	}

	@NotNull
	@Override
	public Collection<NapileProperty> getPropertyDeclarations(@NotNull Name name)
	{
		return JetTopLevelPropertiesFqnNameIndex.getInstance().get(fqName.child(name).toString(), project, GlobalSearchScope.allScope(project));
	}

	@Override
	public Collection<FqName> getAllDeclaredPackages()
	{
		Collection<FqName> subPackages = new HashSet<FqName>();

		for(NapileFile file : getJetFiles())
		{
			String packageName = file.getPackageName();
			if(packageName != null)
			{
				FqName packageFqName = new FqName(packageName);
				if(!packageFqName.isRoot() && packageFqName.parent().equalsTo(fqName))
				{
					subPackages.add(packageFqName);
				}
			}
		}

		return subPackages;
	}

	private Collection<NapileFile> getJetFiles()
	{
		Collection<VirtualFile> kotlinFiles = FilenameIndex.getAllFilesByExt(project, "kt");
		kotlinFiles.addAll(FilenameIndex.getAllFilesByExt(project, "idea"));

		Collection<NapileFile> jetFiles = new HashSet<NapileFile>(kotlinFiles.size());

		for(VirtualFile file : kotlinFiles)
		{
			PsiFile kotlinFile = PsiManager.getInstance(project).findFile(file);
			if(kotlinFile instanceof NapileFile)
			{
				jetFiles.add((NapileFile) kotlinFile);
			}
		}

		return jetFiles;
	}
}
