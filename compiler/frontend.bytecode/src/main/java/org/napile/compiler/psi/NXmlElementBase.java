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

package org.napile.compiler.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.NapileLanguage;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiElementBase;
import com.intellij.util.IncorrectOperationException;

/**
 * @author VISTALL
 * @date 19:36/09.10.12
 */
public abstract class NXmlElementBase extends PsiElementBase
{
	private final PsiManager psiManager;

	public NXmlElementBase(PsiManager psiManager)
	{
		this.psiManager = psiManager;
	}

	public abstract void appendMirrorText(int indent, StringBuilder builder);

	@NotNull
	@Override
	public Language getLanguage()
	{
		return NapileLanguage.INSTANCE;
	}

	@Override
	public PsiManager getManager()
	{
		return psiManager;
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return new PsiElement[0];
	}

	@Override
	public PsiElement getParent()
	{
		return null;
	}

	@Override
	public TextRange getTextRange()
	{
		return TextRange.EMPTY_RANGE;
	}

	@Override
	public int getStartOffsetInParent()
	{
		return 0;
	}

	@Override
	public int getTextLength()
	{
		return 0;
	}

	@Nullable
	@Override
	public PsiElement findElementAt(int i)
	{
		return null;
	}

	@Override
	public int getTextOffset()
	{
		return 0;
	}

	@Override
	public String getText()
	{
		return null;
	}

	@NotNull
	@Override
	public char[] textToCharArray()
	{
		return new char[0];
	}

	@Override
	public PsiElement copy()
	{
		return null;
	}

	@Override
	public PsiElement add(@NotNull PsiElement psiElement) throws IncorrectOperationException
	{
		return null;
	}

	@Override
	public PsiElement addBefore(@NotNull PsiElement psiElement, @Nullable PsiElement psiElement1) throws IncorrectOperationException
	{
		return null;
	}

	@Override
	public PsiElement addAfter(@NotNull PsiElement psiElement, @Nullable PsiElement psiElement1) throws IncorrectOperationException
	{
		return null;
	}

	@Override
	public void checkAdd(@NotNull PsiElement psiElement) throws IncorrectOperationException
	{
	}

	@Override
	public void delete() throws IncorrectOperationException
	{
	}

	@Override
	public void checkDelete() throws IncorrectOperationException
	{
	}

	@Override
	public PsiElement replace(@NotNull PsiElement psiElement) throws IncorrectOperationException
	{
		return null;
	}

	@Override
	public ASTNode getNode()
	{
		return null;
	}
}
