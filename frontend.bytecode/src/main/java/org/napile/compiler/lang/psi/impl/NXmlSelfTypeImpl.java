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

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NXmlParentedElementBase;
import org.napile.compiler.lang.psi.NapileSelfType;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;

/**
 * @author VISTALL
 * @since 18:49/16.02.13
 */
public class NXmlSelfTypeImpl extends NXmlParentedElementBase implements NapileSelfType
{
	private NapileSimpleNameExpression expression;

	public NXmlSelfTypeImpl(PsiElement parent, PsiElement mirror)
	{
		super(parent, mirror);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		setMirrorCheckingType(element, null);

		NapileSelfType mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		expression = (NapileSimpleNameExpression) NXmlMirrorUtil.mirrorExpression(this, mirror.getThisExpression());
	}

	@NotNull
	@Override
	public List<? extends NapileTypeReference> getTypeArguments()
	{
		return Collections.emptyList();
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitSelfType(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitSelfType(this, data);
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return PsiElement.EMPTY_ARRAY;
	}

	@NotNull
	@Override
	public NapileSimpleNameExpression getThisExpression()
	{
		return expression;
	}
}
