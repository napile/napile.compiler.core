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

package org.napile.compiler.util;

import org.napile.asm.tree.members.AnnotationNode;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MacroNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.NodeVisitor;
import org.napile.asm.tree.members.TypeParameterNode;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.bytecode.tryCatch.TryCatchBlockNode;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.asm.tree.members.types.constructors.MethodTypeNode;
import org.napile.asm.tree.members.types.constructors.MultiTypeNode;
import org.napile.asm.tree.members.types.constructors.ThisTypeNode;
import org.napile.asm.tree.members.types.constructors.TypeParameterValueTypeNode;

/**
 * @author VISTALL
 * @date 18:30/15.02.13
 */
public class DefaultNodeVisitor implements NodeVisitor<StringBuilder, Void>
{
	@Override
	public Void visitAnnotationNode(AnnotationNode annotationNode, StringBuilder a2)
	{
		return null;
	}

	@Override
	public Void visitClassNode(ClassNode classNode, StringBuilder a2)
	{
		return null;
	}

	@Override
	public Void visitMethodNode(MethodNode methodNode, StringBuilder a2)
	{
		return null;
	}

	@Override
	public Void visitMacroNode(MacroNode methodNode, StringBuilder a2)
	{
		return null;
	}

	@Override
	public Void visitVariableNode(VariableNode variableNode, StringBuilder a2)
	{
		return null;
	}

	@Override
	public Void visitMethodParameterNode(MethodParameterNode methodParameterNode, StringBuilder a2)
	{
		return null;
	}

	@Override
	public Void visitTypeParameter(TypeParameterNode typeParameterNode, StringBuilder a2)
	{
		return null;
	}

	@Override
	public Void visitTypeNode(TypeNode typeNode, StringBuilder a2)
	{
		return null;
	}

	@Override
	public Void visitClassTypeNode(ClassTypeNode classTypeNode, StringBuilder a2)
	{
		return null;
	}

	@Override
	public Void visitThisTypeNode(ThisTypeNode thisTypeNode, StringBuilder a2)
	{
		return null;
	}

	@Override
	public Void visitMethodTypeNode(MethodTypeNode methodTypeNode, StringBuilder a2)
	{
		return null;
	}

	@Override
	public Void visitMultiTypeNode(MultiTypeNode multiTypeNode, StringBuilder a2)
	{
		return null;
	}

	@Override
	public Void visitTypeParameterValueTypeNode(TypeParameterValueTypeNode typeParameterValueTypeNode, StringBuilder a2)
	{
		return null;
	}

	@Override
	public Void visitTryCatchBlockNode(TryCatchBlockNode tryCatchBlockNode, StringBuilder a2)
	{
		return null;
	}
}
