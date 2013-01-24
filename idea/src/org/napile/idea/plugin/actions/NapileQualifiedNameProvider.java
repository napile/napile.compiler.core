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

package org.napile.idea.plugin.actions;

import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileNamedDeclaration;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import com.intellij.ide.actions.QualifiedNameProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @date 16:58/24.01.13
 */
public class NapileQualifiedNameProvider implements QualifiedNameProvider
{
	@Nullable
	@Override
	public PsiElement adjustElementToCopy(PsiElement element)
	{
		return null;
	}

	@Nullable
	@Override
	public String getQualifiedName(PsiElement element)
	{
		if(element instanceof NapileFile)
			return NapilePsiUtil.getFQName(((NapileFile) element)).getFqName();
		if(element instanceof NapileNamedDeclaration)
		{
			FqName fqName = NapilePsiUtil.getFQName(((NapileNamedDeclaration) element));
			if(fqName !=  null)
			{
				return fqName.getFqName();
			}
		}
		return null;
	}

	@Override
	public PsiElement qualifiedNameToElement(String fqn, Project project)
	{
		return null;
	}

	@Override
	public void insertQualifiedName(String fqn, PsiElement element, Editor editor, Project project)
	{
	}
}
