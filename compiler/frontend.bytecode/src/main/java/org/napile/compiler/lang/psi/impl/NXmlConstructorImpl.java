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

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.psi.stubs.NapilePsiConstructorStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;

/**
 * @author VISTALL
 * @since 15:07/19.10.12
 */
public class NXmlConstructorImpl extends NXmlTypeParameterOwnerStub<NapilePsiConstructorStub> implements NapileConstructor
{
	public NXmlConstructorImpl(NapilePsiConstructorStub stub)
	{
		super(stub);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileConstructor mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		setMirrorIfPresent(getCallParameterList(), mirror.getCallParameterList());
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return NXmlMirrorUtil.getAllToPsiArray(getCallParameterList());
	}

	@NotNull
	@Override
	public List<NapileTypeReference> getSuperCallTypeList()
	{
		return Collections.emptyList();
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
		return false;
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
	public NapileCallParameterList getCallParameterList()
	{
		final StubElement childStubByType = getStub().findChildStubByType(NapileStubElementTypes.CALL_PARAMETER_LIST);
		return childStubByType == null ? null : (NapileCallParameterList) childStubByType.getPsi();
	}

	@Nullable
	@Override
	public NapileDelegationSpecifierList getDelegationSpecifierList()
	{
		return null;
	}

	@NotNull
	@Override
	public List<NapileDelegationToSuperCall> getDelegationSpecifiers()
	{
		return Collections.emptyList();
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitConstructor(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitConstructor(this, data);
	}

	@Override
	public IStubElementType getElementType()
	{
		return NapileStubElementTypes.CONSTRUCTOR;
	}
}
