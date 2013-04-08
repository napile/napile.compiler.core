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

package org.napile.compiler.lang.psi.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.IfNotParsed;
import org.napile.compiler.lang.psi.NapileConstructorCalleeExpression;
import org.napile.compiler.lang.psi.NapileExpressionImpl;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.psi.NapileTypeElement;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileUserType;
import com.intellij.lang.ASTNode;

/**
 * @author abreslav
 */
public class NapileConstructorCalleeExpressionImpl extends NapileExpressionImpl implements NapileConstructorCalleeExpression
{
	public NapileConstructorCalleeExpressionImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	@Nullable
	@IfNotParsed
	public NapileTypeReference getTypeReference()
	{
		return findChildByClass(NapileTypeReference.class);
	}

	@Override
	@Nullable
	@IfNotParsed
	@Deprecated
	public NapileReferenceExpression getConstructorReferenceExpression()
	{
		NapileTypeReference typeReference = getTypeReference();
		if(typeReference == null)
		{
			return null;
		}
		NapileTypeElement typeElement = typeReference.getTypeElement();
		if(typeElement == null)
		{
			return null;
		}
		assert typeElement instanceof NapileUserType : typeElement;
		return ((NapileUserType) typeElement).getReferenceExpression();
	}
}
