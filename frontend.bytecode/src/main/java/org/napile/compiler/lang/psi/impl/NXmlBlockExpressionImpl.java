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
import org.napile.compiler.lang.psi.NXmlParentedElementBase;
import org.napile.compiler.lang.psi.NapileBlockExpression;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;

/**
 * @author VISTALL
 * @since 17:37/19.02.13
 */
public class NXmlBlockExpressionImpl extends NXmlParentedElementBase implements NapileBlockExpression
{
	private NapileElement[] elements;

	public NXmlBlockExpressionImpl(PsiElement parent)
	{
		super(parent);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileBlockExpression mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		NapileElement[] mirrorElements = mirror.getStatements();
		elements = NapileElement.ARRAY_FACTORY.create(mirrorElements.length);
		for(int i = 0; i < mirrorElements.length; i++)
		{
			final NapileElement mirrorElement = mirrorElements[i];
			if(!(mirrorElement instanceof NapileExpression))
				continue;
			elements[i] = NXmlMirrorUtil.mirrorExpression(this, (NapileExpression) mirrorElement);
		}
	}

	@NotNull
	@Override
	public NapileElement[] getStatements()
	{
		return elements;
	}

	@Nullable
	@Override
	public TextRange getLastBracketRange()
	{
		return null;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitBlockExpression(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitBlockExpression(this, data);
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return getStatements();
	}
}
