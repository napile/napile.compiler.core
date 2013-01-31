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

package org.napile.idea.plugin.internal;

import com.intellij.openapi.editor.Editor;

/**
* @author VISTALL
* @date 10:30/31.01.13
*/
public class EditorLocation
{
	public final Editor editor;
	public final long modificationStamp;
	public final int startOffset;
	public final int endOffset;

	EditorLocation(Editor editor)
	{
		this.editor = editor;
		modificationStamp = editor != null ? editor.getDocument().getModificationStamp() : 0;
		startOffset = editor != null ? editor.getSelectionModel().getSelectionStart() : 0;
		endOffset = editor != null ? editor.getSelectionModel().getSelectionEnd() : 0;
	}

	public static EditorLocation fromEditor(Editor editor)
	{
		return new EditorLocation(editor);
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
}
