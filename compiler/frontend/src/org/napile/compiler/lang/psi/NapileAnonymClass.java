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
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.lexer.NapileToken;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.impl.NapileModifierListImpl;
import org.napile.doc.lang.psi.NapileDoc;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;

/**
 * @author abreslav
 */
public class NapileAnonymClass extends NapileElementImpl implements NapileClassLike, NapileExpression, NapileNamedDeclaration, NapileDelegationSpecifierListOwner
{
	private static final FqName FQ_NAME = new FqName("@anonym");

	public NapileAnonymClass(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public String getName()
	{
		return FQ_NAME.getFqName();
	}

	@Override
	public FqName getFqName()
	{
		return FQ_NAME;
	}

	@Override
	public PsiElement getNameIdentifier()
	{
		return null;
	}

	@Override
	public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException
	{
		return null;
	}

	@Override
	public NapileElement getSuperTypesElement()
	{
		return getDelegationSpecifierList();
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

	@Override
	@Nullable
	public NapileModifierListImpl getModifierList()
	{
		return null;
	}

	@Override
	public boolean hasModifier(NapileToken modifier)
	{
		return false;
	}

	@Override
	public ASTNode getModifierNode(NapileToken token)
	{
		return null;
	}

	@Override
	@Nullable
	public NapileDelegationSpecifierList getDelegationSpecifierList()
	{
		return (NapileDelegationSpecifierList) findChildByType(NapileNodes.DELEGATION_SPECIFIER_LIST);
	}

	@Override
	@NotNull
	public List<NapileDelegationToSuperCall> getDelegationSpecifiers()
	{
		NapileDelegationSpecifierList list = getDelegationSpecifierList();
		return list != null ? list.getDelegationSpecifiers() : Collections.<NapileDelegationToSuperCall>emptyList();
	}

	@NotNull
	@Override
	public Name getNameAsName()
	{
		return FQ_NAME.shortName();
	}

	@Override
	public NapileClassBody getBody()
	{
		return (NapileClassBody) findChildByType(NapileNodes.CLASS_BODY);
	}

	@Override
	@NotNull
	public NapileDeclaration[] getDeclarations()
	{
		NapileClassBody body = getBody();
		if(body == null)
			return NapileDeclaration.EMPTY_ARRAY;

		return body.getDeclarations();
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitAnonymClass(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitAnonymClass(this, data);
	}

	@NotNull
	public PsiElement getObjectKeyword()
	{
		return findNotNullChildByType(NapileTokens.ANONYM_KEYWORD);
	}

	@Override
	public void delete() throws IncorrectOperationException
	{
		NapilePsiUtil.deleteClass(this);
	}

	@NotNull
	@Override
	public Name getNameAsSafeName()
	{
		return getNameAsName();
	}

	@Nullable
	@Override
	public NapileDoc getDocComment()
	{
		return null;
	}
}
