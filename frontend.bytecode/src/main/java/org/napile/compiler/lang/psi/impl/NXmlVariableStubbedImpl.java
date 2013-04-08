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

package org.napile.compiler.lang.psi.impl;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.psi.NapileVariableAccessor;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.psi.stubs.NapilePsiVariableStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.stubs.IStubElementType;

/**
 * @author VISTALL
 * @since 15:07/19.10.12
 */
public class NXmlVariableStubbedImpl extends NXmlTypeParameterOwnerStub<NapilePsiVariableStub> implements NapileVariable
{
	private boolean mutable;
	private NXmlTypeReferenceImpl returnType;
	private List<NXmlVariableAccessorImpl> accessors;

	public NXmlVariableStubbedImpl(NapilePsiVariableStub stub)
	{
		super(stub);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		final NapileVariable mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);
		setMirrorIfPresent(getModifierList(),  mirror.getModifierList());

		returnType = new NXmlTypeReferenceImpl(this, mirror.getType());
		nameIdentifier = new NXmlIdentifierImpl(this, mirror.getNameIdentifier());
		mutable = mirror.isMutable();

		accessors = new ArrayList<NXmlVariableAccessorImpl>(2);
		for(NapileVariableAccessor accessor : mirror.getAccessors())
		{
			accessors.add(new NXmlVariableAccessorImpl(this, accessor));
		}

		setMirrors(getAccessors(), mirror.getAccessors());
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return NXmlMirrorUtil.getAllToPsiArray(getModifierList(), nameIdentifier, returnType, accessors);
	}

	@Nullable
	@Override
	public NapileTypeReference getType()
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

	@NotNull
	@Override
	public NapileVariableAccessor[] getAccessors()
	{
		return accessors.toArray(new NapileVariableAccessor[accessors.size()]);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitVariable(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitVariable(this, data);
	}

	@Nullable
	@Override
	public NapileExpression getInitializer()
	{
		return null;
	}

	@Override
	public IStubElementType getElementType()
	{
		return NapileStubElementTypes.VARIABLE;
	}

	@Override
	public ItemPresentation getPresentation()
	{
		return ItemPresentationProviders.getItemPresentation(this);
	}
}
