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
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.stubs.PsiJetPropertyStub;
import org.napile.compiler.lang.psi.stubs.impl.PsiJetPropertyStubImpl;
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
public class JetPropertyElementType extends JetStubElementType<PsiJetPropertyStub, NapileProperty>
{
	public JetPropertyElementType(@NotNull @NonNls String debugName)
	{
		super(debugName);
	}

	@Override
	public NapileProperty createPsiFromAst(@NotNull ASTNode node)
	{
		return new NapileProperty(node);
	}

	@Override
	public NapileProperty createPsi(@NotNull PsiJetPropertyStub stub)
	{
		return new NapileProperty(stub, JetStubElementTypes.PROPERTY);
	}

	@Override
	public boolean shouldCreateStub(ASTNode node)
	{
		if(super.shouldCreateStub(node))
		{
			PsiElement psi = node.getPsi();
			if(psi instanceof NapileProperty)
			{
				NapileProperty property = (NapileProperty) psi;
				return property.getName() != null;
			}
		}

		return false;
	}

	@Override
	public PsiJetPropertyStub createStub(@NotNull NapileProperty psi, StubElement parentStub)
	{
		NapileTypeReference typeRef = psi.getPropertyTypeRef();
		NapileExpression expression = psi.getInitializer();

		assert !psi.isLocal() : "Should not store local property";

		return new PsiJetPropertyStubImpl(JetStubElementTypes.PROPERTY, parentStub, psi.getName(), typeRef != null ? typeRef.getText() : null, expression != null ? expression.getText() : null);
	}

	@Override
	public void serialize(PsiJetPropertyStub stub, StubOutputStream dataStream) throws IOException
	{
		dataStream.writeName(stub.getName());
		dataStream.writeName(stub.getTypeText());
		dataStream.writeName(stub.getInferenceBodyText());
	}

	@Override
	public PsiJetPropertyStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException
	{
		StringRef name = dataStream.readName();
		StringRef typeText = dataStream.readName();
		StringRef inferenceBodyText = dataStream.readName();

		return new PsiJetPropertyStubImpl(JetStubElementTypes.PROPERTY, parentStub, name, typeText, inferenceBodyText);
	}

	@Override
	public void indexStub(PsiJetPropertyStub stub, IndexSink sink)
	{
		StubIndexServiceFactory.getInstance().indexProperty(stub, sink);
	}
}
