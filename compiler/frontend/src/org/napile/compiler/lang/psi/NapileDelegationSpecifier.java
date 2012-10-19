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
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.psi.NapileTypeReference;
import com.intellij.lang.ASTNode;

/**
 * @author max
 */
public class NapileDelegationSpecifier extends NapileElementImpl
{
	public NapileDelegationSpecifier(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitDelegationSpecifier(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitDelegationSpecifier(this, data);
	}

	@Nullable
	public NapileTypeReference getTypeReference()
	{
		return (NapileTypeReference) findChildByType(NapileNodes.TYPE_REFERENCE);
	}

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
}
