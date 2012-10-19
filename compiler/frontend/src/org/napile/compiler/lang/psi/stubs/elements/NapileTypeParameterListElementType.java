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
import org.napile.compiler.lang.psi.NapileTypeParameterList;
import org.napile.compiler.lang.psi.NapileTypeParameterListImpl;
import org.napile.compiler.lang.psi.stubs.NapilePsiTypeParameterListStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;

/**
 * @author Nikolay Krasko
 */
public class NapileTypeParameterListElementType extends NapileStubElementType<NapilePsiTypeParameterListStub, NapileTypeParameterList>
{
	public NapileTypeParameterListElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileTypeParameterList createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileTypeParameterListImpl(node);
	}

	@Override
	public NapileTypeParameterList createPsi(@NotNull NapilePsiTypeParameterListStub stub)
	{
		return getPsiFactory(stub).createTypeParameterList(stub);
	}

	@Override
	public NapilePsiTypeParameterListStub createStub(@NotNull NapileTypeParameterList psi, StubElement parentStub)
	{
		return new NapilePsiTypeParameterListStub(parentStub);
	}

	@Override
	public void serialize(NapilePsiTypeParameterListStub stub, StubOutputStream dataStream) throws IOException
	{
		// Do nothing
	}

	@Override
	public NapilePsiTypeParameterListStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		return new NapilePsiTypeParameterListStub(parentStub);
	}

	@Override
	public void indexStub(NapilePsiTypeParameterListStub stub, IndexSink sink)
	{
		// No index
	}
}

