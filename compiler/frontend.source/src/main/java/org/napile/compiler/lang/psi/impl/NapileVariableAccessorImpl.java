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

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileDeclarationImpl;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.psi.NapileVariableAccessor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;

/**
 * @author VISTALL
 * @date 15:28/05.12.12
 */
public class NapileVariableAccessorImpl extends NapileDeclarationImpl implements NapileVariableAccessor
{
	public NapileVariableAccessorImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	@Nullable
	@Override
	public PsiElement getAccessorElement()
	{
		return findChildByType(NapileTokens.VARIABLE_ACCESS_KEYWORDS);
	}

	@Nullable
	@Override
	public IElementType getAccessorElementType()
	{
		PsiElement accessorElement = getAccessorElement();
		return accessorElement == null ? null : accessorElement.getNode().getElementType();
	}

	@NotNull
	@Override
	public Name getNameAsSafeName()
	{
		return NapilePsiUtil.NO_NAME_PROVIDED;
	}

	@Nullable
	@Override
	public Name getNameAsName()
	{
		return getNameAsSafeName();
	}

	@Nullable
	@Override
	public PsiElement getNameIdentifier()
	{
		return null;
	}

	@Override
	public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException
	{
		throw new IncorrectOperationException();
	}
}
