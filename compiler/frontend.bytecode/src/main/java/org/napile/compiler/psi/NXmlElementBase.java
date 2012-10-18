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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiElementBase;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.util.IncorrectOperationException;

/**
 * @author VISTALL
 * @date 19:36/09.10.12
 */
public abstract class NXmlElementBase<E extends NapileDeclaration> extends PsiElementBase implements NapileDeclarationContainer<E>
{
	private static final Logger LOGGER = Logger.getInstance(NXmlElementBase.class);

	private final PsiManager psiManager;
	private volatile TreeElement mirror = null;

	public NXmlElementBase(PsiManager psiManager)
	{
		this.psiManager = psiManager;
	}

	public PsiElement getMirror()
	{
		TreeElement mirror = this.mirror;
		if(mirror == null)
		{
			((NXmlFileImpl) getContainingFile()).getMirror();
			mirror = this.mirror;
		}
		return SourceTreeToPsiMap.treeElementToPsi(mirror);
	}

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

	@Override
	public abstract PsiElement getParent();

	@Override
	public final TextRange getTextRange()
	{
		PsiElement mirror = getMirror();
		return mirror != null ? mirror.getTextRange() : TextRange.EMPTY_RANGE;
	}

	@Override
	public final int getStartOffsetInParent()
	{
		PsiElement mirror = getMirror();
		return mirror != null ? mirror.getStartOffsetInParent() : -1;
	}

	@Override
	public int getTextLength()
	{
		String text = getText();
		if(text == null)
		{
			LOGGER.error("getText() == null, element = " + this + ", parent = " + getParent());
			return 0;
		}
		return text.length();
	}

	@Override
	public PsiElement findElementAt(int offset)
	{
		return null;
	}

	@Override
	public final int getTextOffset()
	{
		PsiElement mirror = getMirror();
		return mirror != null ? mirror.getTextOffset() : -1;
	}

	@Override
	public String getText()
	{
		PsiElement mirror = getMirror();
		return mirror != null ? mirror.getText() : null;
	}

	@Override
	@NotNull
	public char[] textToCharArray()
	{
		return getMirror().textToCharArray();
	}

	@Override
	public boolean textMatches(@NotNull CharSequence text)
	{
		return getText().equals(text.toString());
	}

	@Override
	public boolean textMatches(@NotNull PsiElement element)
	{
		return getText().equals(element.getText());
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
