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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.psi.stubs.NapilePsiEnumValueStub;
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
 * @since 14:34/21.02.13
 */
public class NXmlEnumValueImpl extends NXmlNamedDeclarationImpl<NapilePsiEnumValueStub> implements NapileEnumValue
{
	private NapileDelegationSpecifierList delegationSpecifierList;
	private NXmlIdentifierImpl identifier;

	public NXmlEnumValueImpl(NapilePsiEnumValueStub stub)
	{
		super(stub);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileEnumValue mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		delegationSpecifierList = new NXmlDelegationSpecifierListImpl(this, mirror.getDelegationSpecifierList());
		identifier = new NXmlIdentifierImpl(this, mirror.getNameIdentifier());
	}

	@Nullable
	@Override
	public NapileDelegationSpecifierList getDelegationSpecifierList()
	{
		return delegationSpecifierList;
	}

	@NotNull
	@Override
	public List<NapileDelegationToSuperCall> getDelegationSpecifiers()
	{
		NapileDelegationSpecifierList list = getDelegationSpecifierList();
		return list != null ? list.getDelegationSpecifiers() : Collections.<NapileDelegationToSuperCall>emptyList();
	}

	@NotNull
	@Override
	public List<? extends NapileTypeReference> getSuperTypes()
	{
		List<NapileDelegationToSuperCall> specifiers = getDelegationSpecifiers();
		List<NapileTypeReference> list = new ArrayList<NapileTypeReference>(specifiers.size());
		for(NapileDelegationToSuperCall s : specifiers)
			list.add(s.getTypeReference());
		return list;
	}

	@Nullable
	@Override
	public NapileElement getSuperTypesElement()
	{
		return null;
	}

	@Nullable
	@Override
	public NapileTypeReference getType()
	{
		return null;
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
		return false;
	}

	@NotNull
	@Override
	public NapileVariableAccessor[] getAccessors()
	{
		return NapileVariableAccessor.EMPTY_ARRAY;
	}

	@Nullable
	@Override
	public NapileTypeParameterList getTypeParameterList()
	{
		return null;
	}

	@NotNull
	@Override
	public NapileTypeParameter[] getTypeParameters()
	{
		return NapileTypeParameter.EMPTY_ARRAY;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitEnumValue(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitEnumValue(this, data);
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
		return NapileStubElementTypes.ENUM_VALUE;
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return NXmlMirrorUtil.getAllToPsiArray(nameIdentifier, delegationSpecifierList);
	}

	@Nullable
	@Override
	public PsiElement getNameIdentifier()
	{
		return identifier;
	}

	@Override
	public ItemPresentation getPresentation()
	{
		return ItemPresentationProviders.getItemPresentation(this);
	}
}