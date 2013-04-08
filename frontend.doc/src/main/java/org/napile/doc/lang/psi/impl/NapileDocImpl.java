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

package org.napile.doc.lang.psi.impl;

import org.jetbrains.annotations.NotNull;
import org.napile.doc.lang.psi.NapileDoc;
import org.napile.doc.lang.psi.NapileDocLine;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @since 9:54/31.01.13
 */
public class NapileDocImpl extends LazyParseablePsiElement implements NapileDoc
{
	public NapileDocImpl(@NotNull IElementType type, CharSequence buffer)
	{
		super(type, buffer);
	}

	@Override
	public NapileDocLine[] getLines()
	{
		return findChildrenByClass(NapileDocLine.class);
	}
}
