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
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.compiler.lang.psi.impl.NapileNamedMacroImpl;
import org.napile.compiler.lang.psi.stubs.NapilePsiMethodOrMacroStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;

/**
 * @author VISTALL
 */
public class NapileMacroElementType extends NapileStubElementType<NapilePsiMethodOrMacroStub, NapileNamedMethodOrMacro>
{

	public NapileMacroElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileNamedMethodOrMacro createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileNamedMacroImpl(node);
	}

	@Override
	public NapileNamedMethodOrMacro createPsi(@NotNull NapilePsiMethodOrMacroStub stub)
	{
		return getPsiFactory(stub).createNamedMethod(stub);
	}

	@Override
	public NapilePsiMethodOrMacroStub createStub(@NotNull NapileNamedMethodOrMacro psi, @NotNull StubElement parentStub)
	{
		return new NapilePsiMethodOrMacroStub(parentStub, psi.getName(), NapileStubElementTypes.MACRO);
	}

	@Override
	public void serialize(NapilePsiMethodOrMacroStub stub, StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getName());
	}

	@Override
	public NapilePsiMethodOrMacroStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		StringRef name = dataStream.readName();
		return new NapilePsiMethodOrMacroStub(parentStub, name, NapileStubElementTypes.MACRO);
	}

	@Override
	public void indexStub(NapilePsiMethodOrMacroStub stub, IndexSink sink)
	{
		StubIndexServiceFactory.getInstance().indexMacro(stub, sink);
	}
}
