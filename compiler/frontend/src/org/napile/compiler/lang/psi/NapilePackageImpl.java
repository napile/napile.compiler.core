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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.NapileNodeTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;

/**
 * @author abreslav
 */
public class NapilePackageImpl extends NapileReferenceExpression
{
	public NapilePackageImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	@NotNull
	public List<NapileSimpleNameExpression> getParentNamespaceNames()
	{
		List<NapileSimpleNameExpression> parentParts = findChildrenByType(NapileNodeTypes.REFERENCE_EXPRESSION);
		NapileSimpleNameExpression lastPart = (NapileSimpleNameExpression) findLastChildByType(NapileNodeTypes.REFERENCE_EXPRESSION);
		parentParts.remove(lastPart);
		return parentParts;
	}

	@Nullable
	public NapileSimpleNameExpression getLastPartExpression()
	{
		return (NapileSimpleNameExpression) findLastChildByType(NapileNodeTypes.REFERENCE_EXPRESSION);
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
		return references.length == 1 ? references[0] : null;
	}

	@Nullable
	public PsiElement getNameIdentifier()
	{
		NapileSimpleNameExpression lastPart = (NapileSimpleNameExpression) findLastChildByType(NapileNodeTypes.REFERENCE_EXPRESSION);
		if(lastPart == null)
		{
			return null;
		}

		return lastPart.getIdentifier();
	}

	@Override
	@NotNull
	public String getName()
	{
		PsiElement nameIdentifier = getNameIdentifier();
		return nameIdentifier == null ? "" : nameIdentifier.getText();
	}

	@NotNull
	public Name getNameAsName()
	{
		PsiElement nameIdentifier = getNameIdentifier();
		return nameIdentifier == null ? NapilePsiUtil.ROOT_NAMESPACE_NAME : Name.identifier(nameIdentifier.getText());
	}

	public boolean isRoot()
	{
		return getName().length() == 0;
	}

	public String getQualifiedName()
	{
		StringBuilder builder = new StringBuilder();
		for(NapileSimpleNameExpression e : findChildrenByClass(NapileSimpleNameExpression.class))
		{
			if(builder.length() > 0)
			{
				builder.append(".");
			}
			builder.append(e.getReferencedName());
		}
		return builder.toString();
	}
}

