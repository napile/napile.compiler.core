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

package org.napile.compiler.lang.parsing.injection;

import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 12:39/12.10.12
 */
public class FragmentCharSequence implements CharSequence
{
	private final CharSequence parent;
	private final int offset;

	public FragmentCharSequence(@NotNull CharSequence parent, int offset)
	{
		this.parent = parent;
		this.offset = offset;
	}

	@Override
	public int length()
	{
		return parent.length() - offset;
	}

	@Override
	public char charAt(int index)
	{
		return parent.charAt(offset + index);
	}

	@Override
	public CharSequence subSequence(int start, int end)
	{
		return parent.subSequence(offset + start, offset + end);
	}

	public int getOffset()
	{
		return offset;
	}
}
