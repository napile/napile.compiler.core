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

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.impl.NapileClassImpl;
import org.napile.compiler.lang.psi.stubs.NapilePsiClassStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class NapileClassElementType extends NapileStubElementType<NapilePsiClassStub, NapileClass>
{

	public NapileClassElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileClass createPsi(@NotNull NapilePsiClassStub stub)
	{
		return getPsiFactory(stub).createClass(stub);
	}

	@Override
	public NapileClass createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileClassImpl(node);
	}

	@Override
	public NapilePsiClassStub createStub(@NotNull NapileClass psi, StubElement parentStub)
	{
		return new NapilePsiClassStub(parentStub, psi.getFqName(), psi.getName());
	}

	@Override
	public void serialize(NapilePsiClassStub stub, StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getName());
		dataStream.writeName(stub.getFqName().getFqName());
	}

	@Override
	public NapilePsiClassStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		StringRef name = dataStream.readName();
		FqName fqName = new FqName(StringRef.toString(dataStream.readName()));
		return new NapilePsiClassStub(parentStub, fqName, name);
	}

	@Override
	public void indexStub(NapilePsiClassStub stub, IndexSink sink)
	{
		StubIndexServiceFactory.getInstance().indexClass(stub, sink);
	}
}
