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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.psi.NXmlParentedElementBase;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @since 14:42/16.02.13
 */
public class NXmlSimpleNameExpressionImpl extends NXmlParentedElementBase implements NapileSimpleNameExpression
{
	private NXmlIdentifierImpl identifier;

	public NXmlSimpleNameExpressionImpl(PsiElement parent, PsiElement mirror)
	{
		super(parent, mirror);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileSimpleNameExpression mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		identifier = new NXmlIdentifierImpl(this, mirror.getReferencedNameElement());
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return new PsiElement[] {identifier};
	}

	@Nullable
	@Override
	public NapileExpression getReceiverExpression()
	{
		return null;
	}

	@Override
	public boolean isImportDirectiveExpression()
	{
		return false;
	}

	@Nullable
	@Override
	public String getReferencedName()
	{
		return identifier.getText();
	}

	@Override
	public Name getReferencedNameAsName()
	{
		return Name.identifier(getReferencedName());
	}

	@NotNull
	@Override
	public PsiElement getReferencedNameElement()
	{
		return identifier;
	}

	@Nullable
	@Override
	public IElementType getReferencedNameElementType()
	{
		return null;
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
