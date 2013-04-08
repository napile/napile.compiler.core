/*
 * Copyright 2006 Sascha Weinreuter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.napile.compiler.injection.regexp.lang.psi.impl;

import org.napile.compiler.injection.regexp.lang.RegExpLanguage;
import org.napile.compiler.injection.regexp.lang.psi.RegExpElement;
import org.napile.compiler.injection.regexp.lang.psi.RegExpElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;

public abstract class RegExpElementImpl extends ASTWrapperPsiElement implements RegExpElement
{
	public RegExpElementImpl(ASTNode node)
	{
		super(node);
	}

	@NotNull
	public Language getLanguage()
	{
		return RegExpLanguage.INSTANCE;
	}

	@NotNull
	@SuppressWarnings({
			"ConstantConditions",
			"EmptyMethod"
	})
	public ASTNode getNode()
	{
		return super.getNode();
	}

	public String toString()
	{
		return getClass().getSimpleName() + ": <" + getText() + ">";
	}

	public void accept(@NotNull PsiElementVisitor visitor)
	{
		if(visitor instanceof RegExpElementVisitor)
		{
			accept((RegExpElementVisitor) visitor);
		}
		else
		{
			super.accept(visitor);
		}
	}

	public void accept(RegExpElementVisitor visitor)
	{
		visitor.visitRegExpElement(this);
	}

	public PsiElement replace(@NotNull PsiElement psiElement) throws IncorrectOperationException
	{
		final ASTNode node = psiElement.getNode();
		assert node != null;
		getNode().getTreeParent().replaceChild(getNode(), node);
		return psiElement;
	}

	public void delete() throws IncorrectOperationException
	{
		getNode().getTreeParent().removeChild(getNode());
	}

	public final String getUnescapedText()
	{
		return getText();
	}

	public static boolean isLiteralExpression(@Nullable PsiElement context)
	{
		if(context == null)
			return false;
		final ASTNode astNode = context.getNode();
		if(astNode == null)
		{
			return false;
		}
		final IElementType elementType = astNode.getElementType();
		final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(context.getLanguage());
		return parserDefinition.getStringLiteralElements().contains(elementType);
	}
}
