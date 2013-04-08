/*
 * Copyright 2010-2012 napile.org
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
import org.napile.compiler.lang.psi.NapileElementImpl;
import org.napile.compiler.lang.psi.NapileMultiType;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.lang.ASTNode;

/**
 * @author VISTALL
 * @since 18:02/26.12.12
 */
public class NapileMultiTypeImpl extends NapileElementImpl implements NapileMultiType
{
	public NapileMultiTypeImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	@NotNull
	@Override
	public List<? extends NapileTypeReference> getTypeArguments()
	{
		return Collections.emptyList();
	}

	@Override
	public NapileVariable[] getVariables()
	{
		return findChildrenByClass(NapileVariable.class);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitMultiType(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitMultiType(this, data);
	}
}
