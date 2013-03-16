/*
 * Copyright 2010-2012 napile.org
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

package org.napile.idea.plugin.highlighter;

import java.util.Map;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * @author VISTALL
 * @since 17:56/27.10.12
 */
public class SyntaxHighlighterUtil
{
	protected static final TextAttributesKey[] EMPTY = new TextAttributesKey[0];

	public static void fillMap(Map<IElementType, TextAttributesKey> map, TokenSet keys, TextAttributesKey value)
	{
		IElementType[] types = keys.getTypes();
		fillMap(map, value, types);
	}

	public static void fillMap(final Map<IElementType, TextAttributesKey> map, final TextAttributesKey value, final IElementType... types)
	{
		for(IElementType type : types)
			map.put(type, value);
	}

	public static TextAttributesKey[] pack(TextAttributesKey key)
	{
		if(key == null)
			return EMPTY;
		return new TextAttributesKey[]{key};
	}
}
