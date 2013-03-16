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

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.lexer.NapileToken;
import org.napile.compiler.lang.psi.NXmlStubElementBase;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.NapileNamedDeclaration;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import org.napile.doc.lang.psi.NapileDoc;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.stubs.NamedStub;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.IncorrectOperationException;

/**
 * @author VISTALL
 * @since 21:21/15.02.13
 */
public abstract class NXmlNamedDeclarationImpl<T extends NamedStub> extends NXmlStubElementBase<T> implements NapileNamedDeclaration, StubBasedPsiElement<T>
{
	protected NXmlIdentifierImpl nameIdentifier;

	protected NXmlNamedDeclarationImpl(T stub)
	{
		super(stub);
	}

	@Nullable
	@Override
	public PsiElement getNameIdentifier()
	{
		//

		return nameIdentifier;
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return PsiElement.EMPTY_ARRAY;
	}

	@Nullable
	@Override
	public NapileDoc getDocComment()
	{
		return null;
	}

	@Nullable
	@Override
	public NapileModifierList getModifierList()
	{
		final StubElement childStubByType = getStub().findChildStubByType(NapileStubElementTypes.MODIFIER_LIST);
		return childStubByType != null ? (NapileModifierList) childStubByType.getPsi() : null;
	}

	@Override
	public boolean hasModifier(NapileToken modifier)
	{
		NapileModifierList modifierList = getModifierList();
		return modifierList != null && modifierList.hasModifier(modifier);
	}

	@Nullable
	@Override
	public ASTNode getModifierNode(NapileToken token)
	{
		return null;
	}

	@Override
	public Name getNameAsName()
	{
		String name = getName();
		return name != null ? Name.identifier(name) : null;
	}

	@Override
	public String getName()
	{
		return getStub().getName();
	}

	@Override
	@NotNull
	public Name getNameAsSafeName()
	{
		return NapilePsiUtil.safeName(getName());
	}

	@Override
	public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException
	{
		return null;
	}
}
