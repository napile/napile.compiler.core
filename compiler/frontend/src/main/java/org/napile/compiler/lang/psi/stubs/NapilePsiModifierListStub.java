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

package org.napile.compiler.lang.psi.stubs;

import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.stubs.elements.NapileModifierListElementType;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @since 13:44/17.02.13
 */
public class NapilePsiModifierListStub extends StubBase<NapileModifierList>
{
	private final int modifiers;

	public NapilePsiModifierListStub(StubElement parent, int modifiers)
	{
		super(parent, NapileStubElementTypes.MODIFIER_LIST);
		this.modifiers = modifiers;
	}

	public boolean hasModifier(IElementType token)
	{
		return (modifiers & NapileModifierListElementType.ELEMENT_TO_MASK.get(token)) != 0;
	}

	public int getModifiers()
	{
		return modifiers;
	}
}
