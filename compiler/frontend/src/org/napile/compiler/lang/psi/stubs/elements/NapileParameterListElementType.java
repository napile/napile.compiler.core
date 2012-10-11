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
import org.napile.compiler.lang.psi.NapileParameterList;
import org.napile.compiler.lang.psi.stubs.NapilePsiParameterListStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;

/**
 * @author Nikolay Krasko
 */
public class NapileParameterListElementType extends NapileStubElementType<NapilePsiParameterListStub, NapileParameterList>
{
	public NapileParameterListElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileParameterList createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileParameterList(node);
	}

	@Override
	public NapileParameterList createPsi(@NotNull NapilePsiParameterListStub stub)
	{
		return new NapileParameterList(stub, NapileStubElementTypes.VALUE_PARAMETER_LIST);
	}

	@Override
	public NapilePsiParameterListStub createStub(@NotNull NapileParameterList psi, StubElement parentStub)
	{
		return new NapilePsiParameterListStub(NapileStubElementTypes.VALUE_PARAMETER_LIST, parentStub);
	}

	@Override
	public void serialize(NapilePsiParameterListStub stub, StubOutputStream dataStream) throws IOException
	{
		// Nothing to serialize
	}

	@Override
	public NapilePsiParameterListStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		return new NapilePsiParameterListStub(NapileStubElementTypes.VALUE_PARAMETER_LIST, parentStub);
	}

	@Override
	public void indexStub(NapilePsiParameterListStub stub, IndexSink sink)
	{
		// No index
	}
}

