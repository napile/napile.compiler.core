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

package org.napile.compiler.lang.psi.stubs.elements;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.io.IOException;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.impl.NapileModifierListImpl;
import org.napile.compiler.lang.psi.stubs.NapilePsiModifierListStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @date 13:46/17.02.13
 */
public class NapileModifierListElementType extends NapileStubElementType<NapilePsiModifierListStub, NapileModifierList>
{
	public static final TObjectIntHashMap<IElementType> ELEMENT_TO_MASK = new TObjectIntHashMap<IElementType>();
	public static final TIntObjectHashMap<IElementType> MASK_TO_ELEMENT = new TIntObjectHashMap<IElementType>();

	static
	{
		final IElementType[] types = NapileTokens.MODIFIER_KEYWORDS.getTypes();

		assert types.length < 32;

		for(int i = 0; i < types.length; i++)
		{
			final int mask = 1 << i;
			final IElementType elementType = types[i];

			ELEMENT_TO_MASK.put(elementType, mask);
			MASK_TO_ELEMENT.put(mask, elementType);
		}
	}

	public NapileModifierListElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileModifierList createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileModifierListImpl(node);
	}

	@Override
	public NapileModifierList createPsi(@NotNull NapilePsiModifierListStub stub)
	{
		return getPsiFactory(stub).createModifierList(stub);
	}

	@Override
	public NapilePsiModifierListStub createStub(@NotNull NapileModifierList psi, StubElement parentStub)
	{
		int modifiers = 0;

		final TObjectIntIterator<IElementType> iterator = ELEMENT_TO_MASK.iterator();
		while(iterator.hasNext())
		{
			iterator.advance();

			IElementType type = iterator.key();
			int mask = iterator.value();

			if(psi.hasModifier(type))
				modifiers |= mask;
		}

		return new NapilePsiModifierListStub(parentStub, modifiers);
	}

	@Override
	public void serialize(NapilePsiModifierListStub stub, StubOutputStream dataStream) throws IOException
	{
		dataStream.writeInt(stub.getModifiers());
	}

	@Override
	public NapilePsiModifierListStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		int modifiers = dataStream.readInt();
		return new NapilePsiModifierListStub(parentStub, modifiers);
	}

	@Override
	public void indexStub(NapilePsiModifierListStub stub, IndexSink sink)
	{
	}
}
