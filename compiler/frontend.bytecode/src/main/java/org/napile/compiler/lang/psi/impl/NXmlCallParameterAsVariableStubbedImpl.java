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

package org.napile.compiler.lang.psi.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NapileCallParameterAsVariable;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.psi.stubs.NapilePsiCallParameterAsVariableStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.stubs.IStubElementType;

/**
 * @author VISTALL
 * @date 20:25/16.02.13
 */
public class NXmlCallParameterAsVariableStubbedImpl extends NXmlNamedDeclarationImpl<NapilePsiCallParameterAsVariableStub> implements NapileCallParameterAsVariable
{
	private NapileExpression defaultValue;
	private NXmlTypeReferenceImpl returnType;
	private boolean mutable;
	private boolean ref;
	private NXmlIdentifierImpl nameIdentifier;

	public NXmlCallParameterAsVariableStubbedImpl(NapilePsiCallParameterAsVariableStub stub)
	{
		super(stub);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		final NapileCallParameterAsVariable mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		returnType = NXmlMirrorUtil.mirrorType(this, mirror.getTypeReference());
		nameIdentifier = new NXmlIdentifierImpl(this, mirror.getNameIdentifier());
		mutable = mirror.isMutable();
		ref = mirror.isRef();
		final NapileExpression defaultValue = mirror.getDefaultValue();
		if(defaultValue != null)
		{
			this.defaultValue = NXmlMirrorUtil.mirrorExpression(this, defaultValue);
		}
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return NXmlMirrorUtil.getAllToPsiArray(nameIdentifier, returnType, defaultValue);
	}

	@Nullable
	@Override
	public NapileTypeReference getTypeReference()
	{
		return returnType;
	}

	@Nullable
	@Override
	public ASTNode getVarOrValNode()
	{
		return null;
	}

	@Override
	public boolean isMutable()
	{
		return mutable;
	}

	@Override
	public boolean isRef()
	{
		return ref;
	}

	@Override
	public NapileExpression getDefaultValue()
	{
		return defaultValue;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitCallParameterAsVariable(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitCallParameterAsVariable(this, data);
	}

	@Override
	public IStubElementType getElementType()
	{
		return NapileStubElementTypes.CALL_PARAMETER_AS_VARIABLE;
	}
}
