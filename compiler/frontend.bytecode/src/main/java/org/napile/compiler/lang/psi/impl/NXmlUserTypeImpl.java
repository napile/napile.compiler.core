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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NXmlParentedElementBase;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileTypeArgumentList;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileUserType;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;

/**
 * @author VISTALL
 * @since 16:45/16.02.13
 */
public class NXmlUserTypeImpl extends NXmlParentedElementBase implements NapileUserType
{
	private List<? extends NapileTypeReference> parameters;
	private NapileUserType qualifier;
	private NapileSimpleNameExpression ref;

	public NXmlUserTypeImpl(PsiElement parent, PsiElement mirror)
	{
		super(parent, mirror);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		setMirrorCheckingType(element, null);

		NapileUserType mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		parameters = NXmlMirrorUtil.mirrorTypes(this, mirror.getTypeArguments());
		qualifier = (NapileUserType) NXmlMirrorUtil.mirrorTypeElement(this, mirror.getQualifier());
		ref = new NXmlSimpleNameExpressionImpl(this, mirror.getReferenceExpression());
	}

	@Nullable
	@Override
	public NapileTypeArgumentList getTypeArgumentList()
	{
		return null;
	}

	@Nullable
	@Override
	public NapileSimpleNameExpression getReferenceExpression()
	{
		return ref;
	}

	@Nullable
	@Override
	public NapileUserType getQualifier()
	{
		return qualifier;
	}

	@Nullable
	@Override
	public String getReferencedName()
	{
		return ref == null ? null : ref.getReferencedName();
	}

	@NotNull
	@Override
	public List<? extends NapileTypeReference> getTypeArguments()
	{
		return parameters;
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return NXmlMirrorUtil.getAllToPsiArray(qualifier, ref, parameters);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitUserType(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitUserType(this, data);
	}
}
