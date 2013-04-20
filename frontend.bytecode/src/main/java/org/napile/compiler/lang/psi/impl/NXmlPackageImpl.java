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

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.psi.NXmlParentedElementBase;
import org.napile.compiler.lang.psi.NapilePackage;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.impl.source.tree.TreeElement;

/**
 * @author VISTALL
 * @since 11:43/16.02.13
 */
public class NXmlPackageImpl extends NXmlParentedElementBase implements NapilePackage
{
	private NapileSimpleNameExpression[] expressions;

	public NXmlPackageImpl(PsiElement parent, PsiElement mirror)
	{
		super(parent, mirror);
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return getAllExpressions();
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapilePackage mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, NapileNodes.PACKAGE);

		final NapileSimpleNameExpression[] mirrorExpressions = mirror.getAllExpressions();

		expressions = new NapileSimpleNameExpression[mirrorExpressions.length];
		for(int i = 0; i < expressions.length; i++)
		{
			final NXmlSimpleNameExpressionImpl simpleNameExpression = new NXmlSimpleNameExpressionImpl(this, mirrorExpressions[i]);

			expressions[i] = simpleNameExpression;
		}

		setMirrors(getAllExpressions(), mirror.getAllExpressions());
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

	@Override
	public NapileSimpleNameExpression[] getAllExpressions()
	{
		return expressions;
	}

	@NotNull
	@Override
	public List<NapileSimpleNameExpression> getParentNamespaceNames()
	{
		List<NapileSimpleNameExpression> list = new ArrayList<NapileSimpleNameExpression>(expressions.length - 1);
		for(int i = 0; i < expressions.length; i++)
		{
			if(i != expressions.length - 1)
			{
				list.add(expressions[i]);
			}
		}
		return list;
	}

	@Nullable
	@Override
	public NapileSimpleNameExpression getLastPartExpression()
	{
		if(expressions.length == 0)
			return null;
		else
			return expressions[expressions.length - 1];
	}

	@Override
	@NotNull
	public String getName()
	{
		NapileSimpleNameExpression last = getLastPartExpression();
		if(last == null)
			return "";

		return last.getText();
	}

	@Override
	@NotNull
	public Name getNameAsName()
	{
		NapileSimpleNameExpression last = getLastPartExpression();
		if(last == null)
			return NapilePsiUtil.ROOT_NAMESPACE_NAME;

		return Name.identifier(last.getText());
	}

	@NotNull
	@Override
	public FqName getFqName()
	{
		StringBuilder builder = new StringBuilder();
		for(NapileSimpleNameExpression e : getAllExpressions())
		{
			if(builder.length() > 0)
			{
				builder.append(".");
			}
			builder.append(e.getReferencedName());
		}
		return new FqName(builder.toString());
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitJetElement(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitJetElement(this, data);
	}
}
