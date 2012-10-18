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

package org.napile.compiler.lang.psi.stubs.elements;

import java.io.IOException;

import org.napile.compiler.NapileLanguage;
import org.napile.compiler.lang.psi.stubs.NapilePsiFileStub;
import com.intellij.psi.StubBuilder;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class NapileFileElementType extends IStubFileElementType<NapilePsiFileStub>
{
	public static final int STUB_VERSION = 22;

	public NapileFileElementType()
	{
		super("napile.FILE", NapileLanguage.INSTANCE);
	}

	@Override
	public StubBuilder getBuilder()
	{
		return new NapileFileStubBuilder();
	}

	@Override
	public int getStubVersion()
	{
		return STUB_VERSION;
	}

	@Override
	public String getExternalId()
	{
		return toString();
	}

	@Override
	public void serialize(final NapilePsiFileStub stub, final StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getPackageName());
		dataStream.writeBoolean(stub.isCompiled());
	}

	@Override
	public NapilePsiFileStub deserialize(final StubInputStream dataStream, final StubElement parentStub) throws IOException
	{
		StringRef packName = dataStream.readName();
		boolean compiled = dataStream.readBoolean();
		return new NapilePsiFileStub(null, packName, compiled);
	}

	@Override
	public void indexStub(final NapilePsiFileStub stub, final IndexSink sink)
	{
		// Don't index file
	}
}
