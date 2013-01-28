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
import org.napile.compiler.lang.psi.NapileCallParameterAsVariable;
import org.napile.compiler.lang.psi.impl.NapileCallParameterAsVariableImpl;
import org.napile.compiler.lang.psi.stubs.NapilePsiCallParameterAsVariableStub;
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
public class NapileParameterElementType extends NapileStubElementType<NapilePsiCallParameterAsVariableStub, NapileCallParameterAsVariable>
{
	public NapileParameterElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileCallParameterAsVariable createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileCallParameterAsVariableImpl(node);
	}

	@Override
	public NapileCallParameterAsVariable createPsi(@NotNull NapilePsiCallParameterAsVariableStub stub)
	{
		return getPsiFactory(stub).createCallParameterAsVariable(stub);
	}

	@Override
	public NapilePsiCallParameterAsVariableStub createStub(@NotNull NapileCallParameterAsVariable psi, StubElement parentStub)
	{
		NapileTypeReference typeReference = psi.getTypeReference();
		NapileExpression defaultValue = psi.getDefaultValue();

		return new NapilePsiCallParameterAsVariableStub(parentStub, psi.getName(), typeReference != null ? typeReference.getText() : null, defaultValue != null ? defaultValue.getText() : null);
	}

	@Override
	public boolean shouldCreateStub(ASTNode node)
	{
		return node.getTreeParent().getElementType() == NapileNodes.VALUE_ARGUMENT_LIST;
	}

	@Override
	public void serialize(NapilePsiCallParameterAsVariableStub stub, StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getName());
		dataStream.writeName(stub.getTypeText());
		dataStream.writeName(stub.getDefaultValueText());
	}

	@Override
	public NapilePsiCallParameterAsVariableStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		StringRef name = dataStream.readName();
		StringRef typeText = dataStream.readName();
		StringRef defaultValueText = dataStream.readName();

		return new NapilePsiCallParameterAsVariableStub(parentStub, name, typeText, defaultValueText);
	}

	@Override
	public void indexStub(NapilePsiCallParameterAsVariableStub stub, IndexSink sink)
	{
		// No index
	}
}
