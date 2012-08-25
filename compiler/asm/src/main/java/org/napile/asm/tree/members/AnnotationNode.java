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

package org.napile.asm.tree.members;

import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.asmNew.visitors.AsmVisitor;
import com.sun.istack.internal.NotNull;

/**
 * @author VISTALL
 * @date 0:25/14.08.12
 */
public class AnnotationNode implements AsmNode
{
	private final FqName name;

	public AnnotationNode(@NotNull FqName name)
	{
		this.name = name;
	}

	@Override
	public <T> void accept(@NotNull AsmVisitor<T> visitor, T arg)
	{
		visitor.visitAnnotationNode(this, arg);
	}

	public FqName getName()
	{
		return name;
	}
}
