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
import org.napile.compiler.lang.psi.NapileCallParameter;
import org.napile.compiler.lang.psi.NapileCallParameterList;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.stubs.NamedStub;
import com.intellij.psi.stubs.StubElement;

/**
 * @author VISTALL
 * @date 13:12/16.02.13
 */
public abstract class NXmlNamedMethodOrMacroImpl<S extends NamedStub> extends NXmlTypeParameterOwnerStub<S> implements NapileNamedMethodOrMacro
{
	private NapileTypeReference returnType;

	protected NXmlNamedMethodOrMacroImpl(S stub)
	{
		super(stub);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileNamedMethodOrMacro mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		setMirrorIfPresent(getTypeParameterList(), mirror.getTypeParameterList());
		setMirrorIfPresent(getCallParameterList(), mirror.getCallParameterList());

		returnType = NXmlMirrorUtil.mirrorType(this, mirror.getReturnTypeRef());

		nameIdentifier = NXmlMirrorUtil.mirrorIdentifier(this, mirror.getNameIdentifier());
	}

	@Override
	@NotNull
	public PsiElement[] getChildren()
	{
		return NXmlMirrorUtil.getAllToPsiArray(getTypeParameterList(), nameIdentifier, getCallParameterList(), returnType);
	}

	@Nullable
	@Override
	public PsiElement getEqualsToken()
	{
		return null;
	}

	@Nullable
	@Override
	public NapileCallParameterList getCallParameterList()
	{
		final StubElement childStubByType = getStub().findChildStubByType(NapileStubElementTypes.CALL_PARAMETER_LIST);
		return childStubByType == null ? null : (NapileCallParameterList) childStubByType.getPsi();
	}

	@Nullable
	@Override
	public NapileTypeReference getReturnTypeRef()
	{
		return returnType;
	}

	@Nullable
	@Override
	public NapileExpression getBodyExpression()
	{
		return null;
	}

	@Override
	public boolean hasBlockBody()
	{
		return true;
	}

	@Override
	public boolean hasDeclaredReturnType()
	{
		return true;
	}

	@NotNull
	@Override
	public NapileCallParameter[] getCallParameters()
	{
		NapileCallParameterList list = getCallParameterList();
		return list != null ? list.getParameters() : NapileCallParameter.EMPTY_ARRAY;
	}

	@Nullable
	@Override
	public NapileExpression getInitializer()
	{
		return null;
	}

	@Override
	public ItemPresentation getPresentation()
	{
		return ItemPresentationProviders.getItemPresentation(this);
	}
}
