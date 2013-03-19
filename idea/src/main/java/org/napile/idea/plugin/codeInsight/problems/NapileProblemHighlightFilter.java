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

package org.napile.idea.plugin.codeInsight.problems;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.NapileFileType;
import com.intellij.codeInsight.daemon.ProblemHighlightFilter;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

/**
 * @author VISTALL
 * @since 12:37/25.02.13
 */
public class NapileProblemHighlightFilter extends ProblemHighlightFilter
{
	@Override
	public boolean shouldHighlight(@NotNull PsiFile psiFile)
	{
		return psiFile.getFileType() != NapileFileType.INSTANCE || !ProjectRootsUtil.isOutsideSourceRoot(psiFile);
	}

	@Override
	public boolean shouldProcessInBatch(@NotNull PsiFile psiFile)
	{
		final boolean shouldHighlight = shouldHighlightFile(psiFile);
		if(shouldHighlight)
		{
			if(psiFile.getFileType() == NapileFileType.INSTANCE)
			{
				final VirtualFile virtualFile = psiFile.getVirtualFile();
				if(virtualFile != null && ProjectRootManager.getInstance(psiFile.getProject()).getFileIndex().isInLibrarySource(virtualFile))
				{
					return false;
				}
			}
		}
		return shouldHighlight;
	}
}
