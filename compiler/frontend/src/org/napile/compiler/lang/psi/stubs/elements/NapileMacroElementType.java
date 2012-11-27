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
import org.napile.compiler.lang.psi.NapileNamedMacro;
import org.napile.compiler.lang.psi.impl.NapileNamedMacroImpl;
import org.napile.compiler.lang.psi.stubs.NapilePsiMacroStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;

/**
 * @author VISTALL
 */
public class NapileMacroElementType extends NapileStubElementType<NapilePsiMacroStub, NapileNamedMacro>
{

	public NapileMacroElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileNamedMacro createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileNamedMacroImpl(node);
	}

	@Override
	public NapileNamedMacro createPsi(@NotNull NapilePsiMacroStub stub)
	{
		return getPsiFactory(stub).createNamedMacro(stub);
	}

	@Override
	public NapilePsiMacroStub createStub(@NotNull NapileNamedMacro psi, @NotNull StubElement parentStub)
	{
		return new NapilePsiMacroStub(parentStub, psi.getName());
	}

	@Override
	public void serialize(NapilePsiMacroStub stub, StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getName());
	}

	@Override
	public NapilePsiMacroStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		StringRef name = dataStream.readName();
		return new NapilePsiMacroStub(parentStub, name);
	}

	@Override
	public void indexStub(NapilePsiMacroStub stub, IndexSink sink)
	{
		StubIndexServiceFactory.getInstance().indexMacro(stub, sink);
	}
}
