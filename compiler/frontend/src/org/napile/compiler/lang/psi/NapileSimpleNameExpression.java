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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.parsing.JetExpressionParsing;
import org.napile.compiler.lexer.NapileTokens;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * @author max
 */
public class NapileSimpleNameExpression extends NapileReferenceExpression
{
	public static final TokenSet REFERENCE_TOKENS = TokenSet.create(NapileTokens.IDENTIFIER, NapileTokens.FIELD_IDENTIFIER, NapileTokens.THIS_KEYWORD, NapileTokens.SUPER_KEYWORD);

	public NapileSimpleNameExpression(@NotNull ASTNode node)
	{
		super(node);
	}

	/**
	 * null if it's not a code expression
	 *
	 * @return receiver expression
	 */
	@Nullable
	public NapileExpression getReceiverExpression()
	{
		PsiElement parent = getParent();
		if(parent instanceof NapileQualifiedExpression && !isImportDirectiveExpression())
		{
			NapileQualifiedExpression qualifiedExpression = (NapileQualifiedExpression) parent;
			if(!isFirstPartInQualifiedExpression(qualifiedExpression))
			{
				return qualifiedExpression.getReceiverExpression();
			}
		}
		else if(parent instanceof NapileCallExpression)
		{
			//This is in case `a().b()`
			NapileCallExpression callExpression = (NapileCallExpression) parent;
			parent = callExpression.getParent();
			if(parent instanceof NapileQualifiedExpression)
			{
				NapileQualifiedExpression qualifiedExpression = (NapileQualifiedExpression) parent;
				return qualifiedExpression.getReceiverExpression();
			}
		}
		return null;
	}

	// Check that this is simple name expression is first part in full qualified name: firstPart.otherPart.otherPart.call()
	private boolean isFirstPartInQualifiedExpression(NapileQualifiedExpression qualifiedExpression)
	{
		if(qualifiedExpression.getParent() instanceof NapileQualifiedExpression)
		{
			return isFirstPartInQualifiedExpression((NapileQualifiedExpression) qualifiedExpression.getParent());
		}

		return qualifiedExpression.getFirstChild() == this;
	}

	public boolean isImportDirectiveExpression()
	{
		PsiElement parent = getParent();
		if(parent == null)
		{
			return false;
		}
		else
		{
			return parent instanceof NapileImportDirective || parent.getParent() instanceof NapileImportDirective;
		}
	}

	@Nullable
	@IfNotParsed
	public String getReferencedName()
	{
		String text = getReferencedNameElement().getNode().getText();
		return text != null ? NapilePsiUtil.unquoteIdentifierOrFieldReference(text) : null;
	}

	public Name getReferencedNameAsName()
	{
		String name = getReferencedName();
		if(name != null && name.length() == 0)
		{
			// TODO: fix parser or do something // stepan.koltsov@
		}
		return name != null ? Name.identifierNoValidate(name) : null;
	}

	@NotNull
	public PsiElement getReferencedNameElement()
	{
		PsiElement element = findChildByType(REFERENCE_TOKENS);
		if(element == null)
		{
			element = findChildByType(JetExpressionParsing.ALL_OPERATIONS);
		}

		if(element != null)
		{
			return element;
		}

		return this;
	}

	@Nullable
	public PsiElement getIdentifier()
	{
		return findChildByType(NapileTokens.IDENTIFIER);
	}

	@Nullable
	@IfNotParsed
	public IElementType getReferencedNameElementType()
	{
		return getReferencedNameElement().getNode().getElementType();
	}

	@NotNull
	@Override
	public PsiReference[] getReferences()
	{
		return ReferenceProvidersRegistry.getReferencesFromProviders(this, PsiReferenceService.Hints.NO_HINTS);
	}

	@Nullable
	@Override
	public PsiReference getReference()
	{
		PsiReference[] references = getReferences();
		if(references.length == 1)
		{
			return references[0];
		}
		else
		{
			return null;
		}
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitSimpleNameExpression(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitSimpleNameExpression(this, data);
	}
}
