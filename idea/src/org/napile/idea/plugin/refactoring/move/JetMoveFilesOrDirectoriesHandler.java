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

package org.napile.idea.plugin.refactoring.move;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.napile.compiler.psi.NapileClass;
import org.napile.compiler.psi.NapileClassLike;
import org.napile.compiler.psi.NapileFile;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesHandler;

/**
 * @author Alefas
 * @since 06.03.12
 */
public class JetMoveFilesOrDirectoriesHandler extends MoveFilesOrDirectoriesHandler
{
	public static boolean isMovableClass(NapileClassLike clazz)
	{
		if(!(clazz.getParent() instanceof NapileFile))
			return false;
		NapileFile file = clazz.getContainingFile();
		List<NapileClass> declarations = file.getDeclarations();
		for(NapileClass declaration : declarations)
		{
			if(declaration != null && declaration != clazz)
				return false;
		}
		return true;
	}

	@Override
	public boolean canMove(PsiElement[] elements, @Nullable PsiElement targetContainer)
	{
		for(PsiElement element : elements)
		{
			if(!(element instanceof PsiFile) && !(element instanceof PsiDirectory) &&
					(!(element instanceof NapileClassLike) || !isMovableClass((NapileClassLike) element)))
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public PsiElement[] adjustForMove(Project project, PsiElement[] sourceElements, PsiElement targetElement)
	{
		ArrayList<PsiElement> result = new ArrayList<PsiElement>();
		for(PsiElement element : sourceElements)
		{
			if(element instanceof NapileClassLike)
			{
				result.add(element.getContainingFile());
			}
			else
			{
				result.add(element);
			}
		}
		return result.toArray(new PsiElement[result.size()]);
	}
}
