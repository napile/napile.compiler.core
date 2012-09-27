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

package org.napile.compiler.lang.psi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.NapileNodeTypes;
import org.napile.compiler.lexer.NapileTokens;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;

/**
 * @author max
 */
public class NapileConstructor extends NapileDeclarationImpl implements NapileDeclarationWithBody, NapileStatementExpression, NapileDelegationSpecifierListOwner, NapileNamedDeclaration
{
	public static final NapileConstructor[] EMPTY_ARRAY = new NapileConstructor[0];

	public NapileConstructor(@NotNull ASTNode node)
	{
		super(node);
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
	@IfNotParsed
	public NapileParameterList getParameterList()
	{
		return (NapileParameterList) findChildByType(NapileNodeTypes.VALUE_PARAMETER_LIST);
	}

	@Override
	@NotNull
	public List<NapileElement> getValueParameters()
	{
		NapileParameterList list = getParameterList();
		return list != null ? list.getParameters() : Collections.<NapileElement>emptyList();
	}

	@Override
	@Nullable
	public NapileDelegationSpecifierList getDelegationSpecifierList()
	{
		return (NapileDelegationSpecifierList) findChildByType(NapileNodeTypes.DELEGATION_SPECIFIER_LIST);
	}

	@NotNull
	@Override
	public List<NapileDelegationSpecifier> getDelegationSpecifiers()
	{
		NapileDelegationSpecifierList list = getDelegationSpecifierList();
		return list != null ? list.getDelegationSpecifiers() : Collections.<NapileDelegationSpecifier>emptyList();
	}

	public List<NapileTypeReference> getSuperCallTypeList()
	{
		List<NapileDelegationSpecifier> specifiers = getDelegationSpecifiers();
		List<NapileTypeReference> list = new ArrayList<NapileTypeReference>(specifiers.size());
		for(NapileDelegationSpecifier s : specifiers)
		{
			if(s instanceof NapileDelegatorToSuperCall)
				list.add(s.getTypeReference());
			else
				throw new UnsupportedOperationException(s.getClass().getName());
		}
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

	@NotNull
	@Override
	public NapileElement asElement()
	{
		return this;
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