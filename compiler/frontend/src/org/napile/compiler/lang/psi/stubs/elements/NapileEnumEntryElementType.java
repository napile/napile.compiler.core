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

package org.napile.compiler.lang.psi.stubs.elements;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileEnumEntry;
import org.napile.compiler.lang.psi.NapileEnumEntryImpl;
import org.napile.compiler.lang.psi.stubs.NapilePsiEnumEntryStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;

/**
 * @author VISTALL
 * @date 20:45/27.08.12
 */
public class NapileEnumEntryElementType extends NapileStubElementType<NapilePsiEnumEntryStub, NapileEnumEntry>
{
	public NapileEnumEntryElementType(@NotNull String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileEnumEntry createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileEnumEntryImpl(node);
	}

	@Override
	public NapileEnumEntry createPsi(@NotNull NapilePsiEnumEntryStub stub)
	{
		return getPsiFactory(stub).createEnumEntry(stub);
	}

	@Override
	public NapilePsiEnumEntryStub createStub(@NotNull NapileEnumEntry psi, StubElement parentStub)
	{
		return new NapilePsiEnumEntryStub(parentStub, StringRef.fromString(psi.getName()));
	}

	@Override
	public void serialize(NapilePsiEnumEntryStub stub, StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getName());
	}

	@Override
	public NapilePsiEnumEntryStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		StringRef name = dataStream.readName();
		return new NapilePsiEnumEntryStub(parentStub, name);
	}

	@Override
	public void indexStub(NapilePsiEnumEntryStub stub, IndexSink sink)
	{
	}
}
