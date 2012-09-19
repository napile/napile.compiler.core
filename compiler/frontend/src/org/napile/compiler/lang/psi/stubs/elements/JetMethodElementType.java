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
import org.napile.compiler.lang.psi.NapileNamedFunction;
import org.napile.compiler.lang.psi.stubs.PsiJetFileStub;
import org.napile.compiler.lang.psi.stubs.PsiJetFunctionStub;
import org.napile.compiler.lang.psi.stubs.impl.PsiJetFunctionStubImpl;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class JetMethodElementType extends JetStubElementType<PsiJetFunctionStub, NapileNamedFunction>
{

	public JetMethodElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileNamedFunction createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileNamedFunction(node);
	}

	@Override
	public NapileNamedFunction createPsi(@NotNull PsiJetFunctionStub stub)
	{
		return new NapileNamedFunction(stub);
	}

	@Override
	public boolean shouldCreateStub(ASTNode node)
	{
		if(super.shouldCreateStub(node))
		{
			PsiElement psi = node.getPsi();
			if(psi instanceof NapileNamedFunction)
			{
				NapileNamedFunction function = (NapileNamedFunction) psi;
				return function.getName() != null;
			}
		}

		return false;
	}

	@Override
	public PsiJetFunctionStub createStub(@NotNull NapileNamedFunction psi, @NotNull StubElement parentStub)
	{
		final boolean isTopLevel = parentStub instanceof PsiJetFileStub;
		final boolean isExtension = psi.getReceiverTypeRef() != null;

		FqName qualifiedName = psi.getQualifiedName();

		return new PsiJetFunctionStubImpl(JetStubElementTypes.METHOD, parentStub, psi.getName(), isTopLevel, qualifiedName, isExtension);
	}

	@Override
	public void serialize(PsiJetFunctionStub stub, StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getName());
		dataStream.writeBoolean(stub.isTopLevel());

		FqName topFQName = stub.getTopFQName();
		dataStream.writeName(topFQName != null ? topFQName.toString() : null);

		dataStream.writeBoolean(stub.isExtension());
	}

	@Override
	public PsiJetFunctionStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		StringRef name = dataStream.readName();
		boolean isTopLevel = dataStream.readBoolean();

		StringRef topFQNameStr = dataStream.readName();
		FqName fqName = topFQNameStr != null ? new FqName(topFQNameStr.toString()) : null;

		boolean isExtension = dataStream.readBoolean();

		return new PsiJetFunctionStubImpl(JetStubElementTypes.METHOD, parentStub, name, isTopLevel, fqName, isExtension);
	}

	@Override
	public void indexStub(PsiJetFunctionStub stub, IndexSink sink)
	{
		StubIndexServiceFactory.getInstance().indexMethod(stub, sink);
	}
}
