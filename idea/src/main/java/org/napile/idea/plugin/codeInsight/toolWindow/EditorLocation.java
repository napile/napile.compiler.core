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

package org.napile.idea.plugin.codeInsight.toolWindow;

import org.napile.compiler.lang.psi.NapileFile;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

public class EditorLocation
{
	public final Editor editor;
	public final long modificationStamp;
	public final int startOffset;
	public final int endOffset;
	private final NapileFile file;

	EditorLocation(Editor editor, Project project)
	{
		this.editor = editor;

		if(editor != null)
		{
			modificationStamp = editor.getDocument().getModificationStamp();
			startOffset = editor.getSelectionModel().getSelectionStart();
			endOffset = editor.getSelectionModel().getSelectionEnd();

			VirtualFile vFile = ((EditorEx) editor).getVirtualFile();
			if(vFile == null)
			{
				file = null;
			}
			else
			{
				PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
				file = psiFile instanceof NapileFile ? (NapileFile) psiFile : null;
			}
		}
		else
		{
			modificationStamp = 0;
			startOffset = 0;
			endOffset = 0;
			file = null;
		}
	}

	public static EditorLocation fromEditor(Editor editor, Project project)
	{
		return new EditorLocation(editor, project);
	}

	public Editor getEditor()
	{
		return editor;
	}

	public long getModificationStamp()
	{
		return modificationStamp;
	}

	public int getStartOffset()
	{
		return startOffset;
	}

	public int getEndOffset()
	{
		return endOffset;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
			return true;
		if(!(o instanceof EditorLocation))
			return false;

		EditorLocation location = (EditorLocation) o;

		if(endOffset != location.endOffset)
			return false;
		if(modificationStamp != location.modificationStamp)
			return false;
		if(startOffset != location.startOffset)
			return false;
		if(editor != null ? !editor.equals(location.editor) : location.editor != null)
			return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		int result = editor != null ? editor.hashCode() : 0;
		result = 31 * result + (int) (modificationStamp ^ (modificationStamp >>> 32));
		result = 31 * result + startOffset;
		result = 31 * result + endOffset;
		return result;
	}

	public NapileFile getFile()
	{
		return file;
	}
}
