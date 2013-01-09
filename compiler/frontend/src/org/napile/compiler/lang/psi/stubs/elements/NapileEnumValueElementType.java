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

import java.io.IOException;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileEnumValue;
import org.napile.compiler.lang.psi.impl.NapileEnumValueImpl;
import org.napile.compiler.lang.psi.stubs.NapilePsiEnumValueStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;

/**
 * @author VISTALL
 */
public class NapileEnumValueElementType extends NapileStubElementType<NapilePsiEnumValueStub, NapileEnumValue>
{
	public NapileEnumValueElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileEnumValue createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileEnumValueImpl(node);
	}

	@Override
	public NapileEnumValue createPsi(@NotNull NapilePsiEnumValueStub stub)
	{
		return getPsiFactory(stub).createEnumValue(stub);
	}

	@Override
	public NapilePsiEnumValueStub createStub(@NotNull NapileEnumValue psi, StubElement parentStub)
	{
		return new NapilePsiEnumValueStub(parentStub, psi.getName());
	}

	@Override
	public void serialize(NapilePsiEnumValueStub stub, StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getName());
	}

	@Override
	public NapilePsiEnumValueStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		StringRef name = dataStream.readName();

		return new NapilePsiEnumValueStub(parentStub, name);
	}

	@Override
	public void indexStub(NapilePsiEnumValueStub stub, IndexSink sink)
	{
		StubIndexServiceFactory.getInstance().indexEnumValue(stub, sink);
	}
}
