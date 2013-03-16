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
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.impl.NapileConstructorImpl;
import org.napile.compiler.lang.psi.stubs.NapilePsiConstructorStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;

/**
 * @author VISTALL
 * @since 12:17/16.02.13
 */
public class NapileConstructorElementType extends NapileStubElementType<NapilePsiConstructorStub, NapileConstructor>
{
	public NapileConstructorElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileConstructor createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileConstructorImpl(node);
	}

	@Override
	public NapileConstructor createPsi(@NotNull NapilePsiConstructorStub stub)
	{
		return getPsiFactory(stub).createConstructor(stub);
	}

	@Override
	public NapilePsiConstructorStub createStub(@NotNull NapileConstructor psi, StubElement parentStub)
	{
		return new NapilePsiConstructorStub(parentStub);
	}

	@Override
	public void serialize(NapilePsiConstructorStub stub, StubOutputStream dataStream) throws IOException
	{
	}

	@Override
	public NapilePsiConstructorStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		return new NapilePsiConstructorStub(parentStub);
	}

	@Override
	public void indexStub(NapilePsiConstructorStub stub, IndexSink sink)
	{
	}
}
