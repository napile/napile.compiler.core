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

package org.napile.compiler.lang.resolve.lazy;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lang.resolve.name.Name;

/**
 * @author abreslav
 */
public class FileBasedPackageMemberDeclarationProvider extends AbstractPsiBasedDeclarationProvider implements PackageMemberDeclarationProvider
{

	private final FqName fqName;
	private final FileBasedDeclarationProviderFactory factory;
	private final Collection<NapileFile> allFiles;
	private Collection<FqName> allDeclaredPackages;


	/*package*/ FileBasedPackageMemberDeclarationProvider(@NotNull FqName fqName, @NotNull FileBasedDeclarationProviderFactory factory, @NotNull Collection<NapileFile> allFiles)
	{
		this.fqName = fqName;
		this.factory = factory;
		this.allFiles = allFiles;
	}

	@Override
	protected void doCreateIndex()
	{
		for(NapileFile file : allFiles)
		{
			for(NapileDeclaration declaration : file.getDeclarations())
			{
				putToIndex(declaration);
			}
		}
	}

	@Override
	public boolean isPackageDeclared(@NotNull Name name)
	{
		return factory.isPackageDeclared(fqName.child(name));
	}

	@Override
	public Collection<FqName> getAllDeclaredPackages()
	{
		if(allDeclaredPackages == null)
		{
			allDeclaredPackages = factory.getAllDeclaredSubPackagesOf(fqName);
		}
		return allDeclaredPackages;
	}

	@Override
	public String toString()
	{
		return "Declarations for package " + fqName;
	}
}
