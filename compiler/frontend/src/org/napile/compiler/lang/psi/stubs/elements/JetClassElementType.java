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
import java.util.List;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.psi.NapileClass;
import org.napile.compiler.psi.NapileClassImpl;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.psi.stubs.PsiJetClassStub;
import org.napile.compiler.lang.psi.stubs.impl.PsiJetClassStubImpl;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class JetClassElementType extends JetStubElementType<PsiJetClassStub, NapileClass>
{

	public JetClassElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileClass createPsi(@NotNull PsiJetClassStub stub)
	{
		return new NapileClassImpl(stub);
	}

	@Override
	public NapileClass createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileClassImpl(node);
	}

	@Override
	public PsiJetClassStub createStub(@NotNull NapileClass psi, StubElement parentStub)
	{
		FqName fqName = NapilePsiUtil.getFQName(psi);

		return new PsiJetClassStubImpl(JetStubElementTypes.CLASS, parentStub, fqName != null ? fqName.getFqName() : null, psi.getName(), psi.getSuperNames());
	}

	@Override
	public void serialize(PsiJetClassStub stub, StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getName());
		dataStream.writeName(stub.getQualifiedName());

		List<String> superNames = stub.getSuperNames();
		dataStream.writeVarInt(superNames.size());
		for(String name : superNames)
		{
			dataStream.writeName(name);
		}
	}

	@Override
	public PsiJetClassStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		StringRef name = dataStream.readName();
		StringRef qualifiedName = dataStream.readName();

		int superCount = dataStream.readVarInt();
		StringRef[] superNames = StringRef.createArray(superCount);
		for(int i = 0; i < superCount; i++)
			superNames[i] = dataStream.readName();

		return new PsiJetClassStubImpl(JetStubElementTypes.CLASS, parentStub, qualifiedName, name, superNames);
	}

	@Override
	public void indexStub(PsiJetClassStub stub, IndexSink sink)
	{
		StubIndexServiceFactory.getInstance().indexClass(stub, sink);
	}
}
