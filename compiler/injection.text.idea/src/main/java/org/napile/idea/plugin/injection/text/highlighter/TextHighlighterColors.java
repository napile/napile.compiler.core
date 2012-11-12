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

package org.napile.idea.plugin.injection.text.highlighter;

import java.awt.Color;
import java.awt.Font;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;

/**
 * @author VISTALL
 * @date 15:37/12.11.12
 */
public interface TextHighlighterColors
{
	TextAttributesKey EXPRESSION_RANGES = TextAttributesKey.createTextAttributesKey("NAPILE-TEXT-EXPRESSION_RANGES", new TextAttributes(Color.BLACK, null, null, null, Font.BOLD));
}
