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

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileRetellEntry;
import org.napile.compiler.lang.psi.stubs.NapilePsiRetellEntryStub;
import org.napile.compiler.lang.psi.stubs.impl.NapilePsiRetellEntryStubImpl;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;

/**
 * @author VISTALL
 * @date 23:44/05.09.12
 */
public class NapileRetellEntryElementType extends JetStubElementType<NapilePsiRetellEntryStub, NapileRetellEntry>
{
	public NapileRetellEntryElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileRetellEntry createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileRetellEntry(node);
	}

	@Override
	public NapileRetellEntry createPsi(@NotNull NapilePsiRetellEntryStub stub)
	{
		return new NapileRetellEntry(stub);
	}

	@Override
	public NapilePsiRetellEntryStub createStub(@NotNull NapileRetellEntry psi, StubElement parentStub)
	{
		return new NapilePsiRetellEntryStubImpl(parentStub, StringRef.fromString(psi.getName()));
	}

	@Override
	public void serialize(NapilePsiRetellEntryStub stub, StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getName());
	}

	@Override
	public NapilePsiRetellEntryStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		StringRef name = dataStream.readName();
		return new NapilePsiRetellEntryStubImpl(parentStub, name);
	}

	@Override
	public void indexStub(NapilePsiRetellEntryStub stub, IndexSink sink)
	{
	}
}