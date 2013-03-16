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

package org.napile.idea.plugin.editor;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiFile;

/**
 * @author VISTALL
 * @since 26.02.2013
 */
public class NapileTypedHandler extends TypedHandlerDelegate
{
	@Override
	public Result beforeCharTyped(final char c, final Project project, final Editor editor, final PsiFile file, final FileType fileType)
	{
		if(c == '.' || c == ':')
		{
			AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, new Condition<PsiFile>() {

				@Override
				public boolean value(PsiFile psiFile)
				{
					//TODO [VISTALL] some update ?
					return true;
				}
			});
		}
		return Result.CONTINUE;
	}
}
