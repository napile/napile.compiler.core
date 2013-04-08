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

package org.napile.compiler.lang.psi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.psi.stubs.NapilePsiConstructorStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import org.napile.doc.lang.psi.NapileDoc;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;

/**
 * @author max
 */
public class NapileConstructorImpl extends NapileTypeParameterListOwnerStub<NapilePsiConstructorStub> implements NapileConstructor
{
	public NapileConstructorImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileConstructorImpl(@NotNull NapilePsiConstructorStub stub)
	{
		super(stub, NapileStubElementTypes.CONSTRUCTOR);
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

	@Nullable
	@Override
	public NapileCallParameterList getCallParameterList()
	{
		return getStubOrPsiChild(NapileStubElementTypes.CALL_PARAMETER_LIST);
	}

	@Override
	@NotNull
	public NapileCallParameter[] getCallParameters()
	{
		NapileCallParameterList list = getCallParameterList();
		return list != null ? list.getParameters() : NapileCallParameter.EMPTY_ARRAY;
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
	public List<NapileTypeReference> getSuperCallTypeList()
	{
		List<NapileDelegationToSuperCall> specifiers = getDelegationSpecifiers();
		List<NapileTypeReference> list = new ArrayList<NapileTypeReference>(specifiers.size());
		for(NapileDelegationToSuperCall s : specifiers)
			list.add(s.getTypeReference());
		return list;
	}

	@Override
	public NapileExpression getBodyExpression()
	{
		return findChildByClass(NapileExpression.class);
	}

	@Override
	public boolean hasBlockBody()
	{
		return findChildByType(NapileTokens.EQ) == null;
	}

	@Override
	public boolean hasDeclaredReturnType()
	{
		return true;
	}

	@Override
	@NotNull
	public String getName()
	{
		return "this";  // JetDeclarationTreeNode not show node if name is null
	}

	@Override
	public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException
	{
		throw new IncorrectOperationException();
	}

	@NotNull
	@Override
	public Name getNameAsSafeName()
	{
		return Name.identifier(getName());
	}

	@Override
	@Nullable
	public NapileDoc getDocComment()
	{
		return findChildByClass(NapileDoc.class);
	}

	@Nullable
	@Override
	public Name getNameAsName()
	{
		return Name.identifier(getName());
	}

	@NotNull
	@Override
	public PsiElement getNameIdentifier()
	{
		return findNotNullChildByType(NapileTokens.THIS_KEYWORD);
	}
}