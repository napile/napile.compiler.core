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
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.psi.NapilePropertyParameter;
import org.napile.compiler.lang.psi.stubs.NapilePsiMethodParameterStub;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileTypeReference;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class NapileParameterElementType extends NapileStubElementType<NapilePsiMethodParameterStub, NapilePropertyParameter>
{
	public NapileParameterElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapilePropertyParameter createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapilePropertyParameter(node);
	}

	@Override
	public NapilePropertyParameter createPsi(@NotNull NapilePsiMethodParameterStub stub)
	{
		return getPsiFactory(stub).createParameter(stub);
	}

	@Override
	public NapilePsiMethodParameterStub createStub(@NotNull NapilePropertyParameter psi, StubElement parentStub)
	{
		NapileTypeReference typeReference = psi.getTypeReference();
		NapileExpression defaultValue = psi.getDefaultValue();

		return new NapilePsiMethodParameterStub(parentStub, psi.getName(), psi.isVarArg(), typeReference != null ? typeReference.getText() : null, defaultValue != null ? defaultValue.getText() : null);
	}

	@Override
	public boolean shouldCreateStub(ASTNode node)
	{
		return node.getTreeParent().getElementType() == NapileNodes.VALUE_ARGUMENT_LIST;
	}

	@Override
	public void serialize(NapilePsiMethodParameterStub stub, StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getName());
		dataStream.writeBoolean(stub.isVarArg());
		dataStream.writeName(stub.getTypeText());
		dataStream.writeName(stub.getDefaultValueText());
	}

	@Override
	public NapilePsiMethodParameterStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		StringRef name = dataStream.readName();
		boolean isVarArg = dataStream.readBoolean();
		StringRef typeText = dataStream.readName();
		StringRef defaultValueText = dataStream.readName();

		return new NapilePsiMethodParameterStub(parentStub, name, isVarArg, typeText, defaultValueText);
	}

	@Override
	public void indexStub(NapilePsiMethodParameterStub stub, IndexSink sink)
	{
		// No index
	}
}
