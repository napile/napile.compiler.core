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
import org.napile.compiler.lang.lexer.NapileNodes;
import com.intellij.lang.ASTNode;

/**
 * Type reference element.
 * Underlying token is {@link org.napile.compiler.lang.lexer.NapileNodes#TYPE_REFERENCE}
 *
 * @author max
 */
public class NapileTypeReferenceImpl extends NapileElementImpl implements NapileTypeReference
{
	public NapileTypeReferenceImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitTypeReference(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitTypeReference(this, data);
	}

	@Override
	@Nullable
	public NapileTypeElement getTypeElement()
	{
		return findChildByClass(NapileTypeElement.class);
	}

	@Override
	@NotNull
	public List<NapileAnnotation> getAnnotations()
	{
		return findChildrenByType(NapileNodes.ANNOTATION);
	}
}
