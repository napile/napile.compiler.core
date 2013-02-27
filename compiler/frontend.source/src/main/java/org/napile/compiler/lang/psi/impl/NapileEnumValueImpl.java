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
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.psi.stubs.NapilePsiEnumValueStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;

/**
 * @author VISTALL
 * @date 21:04/08.01.13
 */
public class NapileEnumValueImpl extends NapileTypeParameterListOwnerStub<NapilePsiEnumValueStub> implements NapileEnumValue
{
	public NapileEnumValueImpl(@NotNull NapilePsiEnumValueStub stub)
	{
		super(stub, NapileStubElementTypes.ENUM_VALUE);
	}

	public NapileEnumValueImpl(@NotNull ASTNode node)
	{
		super(node);
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

	@Override
	@Nullable
	public NapileDelegationSpecifierList getDelegationSpecifierList()
	{
		return (NapileDelegationSpecifierList) findChildByType(NapileNodes.DELEGATION_SPECIFIER_LIST);
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
	public List<NapileTypeReference> getSuperTypes()
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
		return getDelegationSpecifierList();
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
