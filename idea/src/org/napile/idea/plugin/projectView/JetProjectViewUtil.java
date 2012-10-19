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

package org.napile.idea.plugin.projectView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NapileStaticConstructor;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileFile;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

/**
 * @author slukjanov aka Frostman
 */
public final class JetProjectViewUtil
{

	private JetProjectViewUtil()
	{
	}

	public static Collection<AbstractTreeNode> getChildren(@NotNull NapileClass napileClass, Project project, ViewSettings settings)
	{
		if(settings.isShowMembers())
		{
			NapileDeclaration[] declarations = napileClass.getDeclarations();
			List<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>(declarations.length);

			for(NapileDeclaration declaration : declarations)
			{
				if(declaration instanceof NapileClass)
					result.add(new NapileClassTreeNode(project, (NapileClass) declaration, settings));
				else if(!(declaration instanceof NapileStaticConstructor))
					result.add(new JetDeclarationTreeNode(project, declaration, settings));
			}

			return result;
		}
		else
			return Collections.emptyList();
	}

	public static boolean canRepresentPsiElement(PsiElement value, Object element, ViewSettings settings)
	{
		if(value == null || !value.isValid())
		{
			return false;
		}

		PsiFile file = value.getContainingFile();
		if(file != null && (file == element || file.getVirtualFile() == element))
		{
			return true;
		}

		if(value == element)
		{
			return true;
		}

		if(!settings.isShowMembers())
		{
			if(element instanceof PsiElement && ((PsiElement) element).getContainingFile() != null)
			{
				PsiFile elementFile = ((PsiElement) element).getContainingFile();
				if(elementFile != null && file != null)
				{
					return elementFile.equals(file);
				}
			}
		}

		return false;
	}

	@Nullable
	public static NapileClass getClassIfHeSingle(@NotNull NapileFile file)
	{
		NapileClass[] list = file.getDeclarations();
		return list.length == 1 ? list[0] : null;
	}
}
