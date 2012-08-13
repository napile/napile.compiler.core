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

package org.napile.asmNew.writters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.napile.asm.tree.members.AnnotationNode;
import org.napile.asm.tree.members.AsmNode;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.types.ClassTypeNode;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asmNew.Modifier;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.sun.istack.internal.NotNull;

/**
 * @author VISTALL
 * @date 0:39/14.08.12
 */
public class ShortTextVisitor implements AsmWriter<StringBuilder>
{
	private final Function<? extends AsmNode, String> TO_STRING_NODE_FUNCTION = new Function<AsmNode, String>()
	{
		@Override
		public String fun(AsmNode asmNode)
		{
			StringBuilder b = new StringBuilder();
			asmNode.accept(ShortTextVisitor.this, b);
			return b.toString();
		}
	};

	private StringBuilder builder = new StringBuilder();

	@Override
	public void visitAnnotationNode(AnnotationNode annotationNode, StringBuilder stringBuilder)
	{
		stringBuilder.append("@");
		stringBuilder.append(annotationNode.getName());
	}

	@Override
	public void visitClassNode(ClassNode classNode, StringBuilder stringBuilder)
	{
		for(Modifier modifier : classNode.modifiers)
			builder.append(modifier.name()).append(" ");

		builder.append("class ").append(classNode.name.getFqName());
		if(!classNode.supers.isEmpty())
		{
			builder.append(" : ");
			builder.append(StringUtil.join(classNode.supers, (Function<ClassTypeNode,String>) TO_STRING_NODE_FUNCTION, ", "));
		}
	}

	@Override
	public void visitClassTypeNode(ClassTypeNode classTypeNode, StringBuilder stringBuilder)
	{
		if(!classTypeNode.getAnnotations().isEmpty())
		{
			stringBuilder.append(StringUtil.join(classTypeNode.getAnnotations(), (Function<AnnotationNode, String>) TO_STRING_NODE_FUNCTION, " "));
			stringBuilder.append(" ");
		}

		stringBuilder.append(classTypeNode.getClassName().getFqName());

		if(!classTypeNode.getTypeParameters().isEmpty())
		{
			stringBuilder.append(" <");
			stringBuilder.append(StringUtil.join(classTypeNode.getTypeParameters(), (Function<TypeNode,String>) TO_STRING_NODE_FUNCTION, ", "));
			stringBuilder.append(">");
		}

		if(classTypeNode.isNullable())
			stringBuilder.append("?");
	}

	@Override
	public void write(@NotNull OutputStream stream)
	{
		try
		{
			stream.write(builder.toString().getBytes());
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void write(@NotNull Writer writer)
	{
		try
		{
			writer.write(builder.toString());
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
