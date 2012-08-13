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

package org.napile.asm.tree.members.types;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.jet.lang.resolve.name.FqName;
import org.napile.asm.tree.members.AnnotableNode;
import org.napile.asmNew.visitors.AsmVisitor;
import com.sun.istack.internal.NotNull;

/**
 * @author VISTALL
 * @date 0:07/14.08.12
 */
public class ClassTypeNode extends AnnotableNode<ClassTypeNode> implements TypeNode
{
	private final List<TypeNode> typeParameters = new ArrayList<TypeNode>(0);
	private final FqName className;
	private final boolean nullable;

	public ClassTypeNode(@NotNull FqName className)
	{
		this(className, false);
	}

	public ClassTypeNode(@NotNull FqName className, boolean nullable)
	{
		this.nullable = nullable;
		this.className = className;
	}

	@NotNull
	public ClassTypeNode addTypeParameter(@NotNull TypeNode typeNode)
	{
		typeParameters.add(typeNode);
		return this;
	}

	@NotNull
	public FqName getClassName()
	{
		return className;
	}

	@NotNull
	public List<TypeNode> getTypeParameters()
	{
		return typeParameters;
	}

	@Override
	public <T> void accept(AsmVisitor<T> visitor, T a2)
	{
		visitor.visitClassTypeNode(this, a2);
	}

	@Override
	public boolean isNullable()
	{
		return nullable;
	}
}
