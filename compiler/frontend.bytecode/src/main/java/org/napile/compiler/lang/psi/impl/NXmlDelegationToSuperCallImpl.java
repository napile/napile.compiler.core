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
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;

/**
 * @author VISTALL
 * @date 16:27/21.02.13
 */
public class NXmlDelegationToSuperCallImpl extends NXmlParentedElementBase implements NapileDelegationToSuperCall
{
	private NXmlConstructorCalleeExpressionImpl calleeExpression;

	public NXmlDelegationToSuperCallImpl(PsiElement parent, PsiElement mirror)
	{
		super(parent, mirror);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileDelegationToSuperCall mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		calleeExpression = new NXmlConstructorCalleeExpressionImpl(this, mirror.getCalleeExpression());
	}

	@Override
	public NapileTypeReference getTypeReference()
	{
		return getCalleeExpression().getTypeReference();
	}

	@Override
	@Nullable
	public NapileUserType getTypeAsUserType()
	{
		final NapileTypeReference reference = getTypeReference();
		if(reference != null)
		{
			final NapileTypeElement element = reference.getTypeElement();
			if(element instanceof NapileUserType)
			{
				return ((NapileUserType) element);
			}
		}
		return null;
	}

	@NotNull
	@Override
	public NapileConstructorCalleeExpression getCalleeExpression()
	{
		return calleeExpression;
	}

	@Nullable
	@Override
	public NapileValueArgumentList getValueArgumentList()
	{
		return null;
	}

	@NotNull
	@Override
	public List<? extends ValueArgument> getValueArguments()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public List<NapileExpression> getFunctionLiteralArguments()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public List<? extends NapileTypeReference> getTypeArguments()
	{
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public NapileTypeArgumentList getTypeArgumentList()
	{
		return null;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitDelegationToSuperCallSpecifier(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitDelegationToSuperCallSpecifier(this, data);
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return NXmlMirrorUtil.getAllToPsiArray(calleeExpression);
	}
}
