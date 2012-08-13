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

package org.jetbrains.jet.lang.resolve;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * @author VISTALL
 * @date 21:39/08.08.12
 */
public class NapileClassResolver
{
	public static NapileClassResolver getInstance(Project project)
	{
		return ServiceManager.getService(project, NapileClassResolver.class);
	}

	private final Project project;

	public NapileClassResolver(Project project)
	{
		this.project = project;
	}

	@Nullable
	public JetClass findClass(@NotNull FqName fqName)
	{
		return findClass(fqName, GlobalSearchScope.allScope(project));
	}

	@Nullable
	public JetClass findClass(@NotNull FqName fqName, @NotNull GlobalSearchScope scope)
	{
		JetFilesProvider filesProvider = JetFilesProvider.getInstance(project);

		List<JetFile> list = filesProvider.allInScope(scope);

		for(JetFile jetFile : list)
		{
			for(JetDeclaration jetDeclaration : jetFile.getDeclarations())
			{
				if(jetDeclaration instanceof JetClass)
				{
					FqName declarationName = JetPsiUtil.getFQName((JetClass)jetDeclaration);
					if(fqName.equals(declarationName))
						return (JetClass)jetDeclaration;
				}
			}
		}
		return null;
	}
}
