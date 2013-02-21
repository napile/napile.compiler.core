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


import java.util.List;

import org.napile.asm.AsmConstants;
import org.napile.asm.Modifier;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.AbstractMemberNode;
import org.napile.asm.tree.members.AnnotableNode;
import org.napile.asm.tree.members.AnnotationNode;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MacroNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.TypeParameterNode;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.bytecode.Instruction;
import org.napile.asm.tree.members.bytecode.impl.NewObjectInstruction;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.asm.tree.members.types.constructors.MethodTypeNode;
import org.napile.asm.tree.members.types.constructors.MultiTypeNode;
import org.napile.asm.tree.members.types.constructors.ThisTypeNode;
import org.napile.asm.tree.members.types.constructors.TypeParameterValueTypeNode;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;

/**
 * @author VISTALL
 * @date 17:52/15.02.13
 *
 * TODO [VISTALL] rework to implements NodeVisitor
 */
public class NodeToStringBuilder
{
	public static String convertClass(ClassNode classNode)
	{
		StringBuilder builder = new StringBuilder();

		FqName fqName = classNode.name;

		FqName parent = fqName.parent();
		if(!parent.isRoot())
		{
			builder.append("package ").append(parent).append("\n\n");
		}

		int indent = 0;

		appendClass(classNode, builder, indent);
		return builder.toString();
	}

	private static void appendClass(ClassNode classNode, StringBuilder builder, int indent)
	{
		appendAnnotations(classNode, builder, indent);

		StringUtil.repeatSymbol(builder, '\t', indent);

		appendModifiers(classNode.modifiers, builder);

		builder.append("class ");

		builder.append(classNode.name.shortName());

		appendTypeParameters(classNode.typeParameters, builder);

		if(!classNode.supers.isEmpty())
		{
			builder.append(" : ");

			appendTypes(classNode.supers, builder, " & ");
		}

		builder.append("\n{\n");

		indent ++;

		for(AbstractMemberNode<?> memberNode : classNode.getMembers())
		{
			if(memberNode instanceof MacroNode)
				appendMethod((MethodNode) memberNode, builder, "macro", indent);
			else if(memberNode instanceof MethodNode)
				appendMethod((MethodNode) memberNode, builder, "meth", indent);
			else if(memberNode instanceof VariableNode)
				appendVariable(classNode, (VariableNode) memberNode, builder, indent);
		}

		builder.append("}");
	}

	private static void appendVariable(ClassNode classNode, VariableNode variableNode, StringBuilder builder, int indent)
	{
		if(variableNode.name.getName().contains(AsmConstants.ANONYM_SPLITTER))
			return;

		appendAnnotations(variableNode, builder, indent);

		StringUtil.repeatSymbol(builder, '\t', indent);

		appendModifiers(variableNode.modifiers, builder);

		if(ArrayUtil.contains(Modifier.ENUM, variableNode.modifiers))
		{
			builder.append("val ").append(variableNode.name);

			ClassTypeNode t = (ClassTypeNode)variableNode.returnType.typeConstructorNode;

			ClassNode targetClass = null;
			for(AbstractMemberNode<?> memberNode : classNode.getMembers())
			{
				if(memberNode instanceof ClassNode && ((ClassNode) memberNode).name.equals(t.className))
				{
					targetClass = (ClassNode) memberNode;
					break;
				}
			}

			builder.append(" : ");
			if(targetClass == null)
			{
				// error in bytecode
				builder.append(NapileLangPackage.ANY);
			}
			else
			{
				final List<TypeNode> supers = targetClass.supers;
				for(int i = 0; i < supers.size(); i++)
				{
					if(i != 0)
						builder.append(" & ");

					TypeNode typeNode = supers.get(i);
					appendType(typeNode, builder);
					builder.append("()");
				}
			}
		}
		else
		{
			appendVariableInfo(variableNode.modifiers, variableNode.name, variableNode.returnType, null, builder);

			builder.append("\n");
			StringUtil.repeatSymbol(builder, '\t', indent);

			builder.append("{\n");

			for(AbstractMemberNode<?> memberNode : classNode.getMembers())
			{
				if(memberNode instanceof MethodNode)
				{
					MethodNode methodNode = (MethodNode) memberNode;

					String name = methodNode.name.getName();

					String methodStartName = variableNode.name + "$";
					if(name.startsWith(methodStartName))
					{
						StringUtil.repeatSymbol(builder, '\t', indent + 1);

						appendModifiers(methodNode.modifiers, builder);

						builder.append(name.substring(methodStartName.length(), name.length()));

						builder.append("\n");
					}
				}
			}

			StringUtil.repeatSymbol(builder, '\t', indent);

			builder.append("}");
		}
		builder.append("\n\n");
	}

	private static void appendMethod(MethodNode methodNode, StringBuilder builder, String key, int indent)
	{
		if(methodNode.name.getName().contains(AsmConstants.ANONYM_SPLITTER))
			return;

		appendAnnotations(methodNode, builder, indent);

		StringUtil.repeatSymbol(builder, '\t', indent);

		appendModifiers(methodNode.modifiers, builder);

		final boolean constructor = methodNode.name.equals(MethodNode.CONSTRUCTOR_NAME) ||methodNode.name.equals(MethodNode.STATIC_CONSTRUCTOR_NAME);

		if(constructor)
			builder.append("this");
		else
		{
			builder.append(key).append(" ").append(methodNode.name);

			appendTypeParameters(methodNode.typeParameters, builder);
		}

		builder.append("(");

		for(int i = 0; i < methodNode.parameters.size(); i++)
		{
			if(i != 0)
				builder.append(", ");

			final MethodParameterNode parameterNode = methodNode.parameters.get(i);

			appendVariableInfo(parameterNode.modifiers, parameterNode.name, parameterNode.returnType, parameterNode.defaultValue, builder);
		}
		builder.append(")");

		if(!constructor)
		{
			builder.append(" : ");

			appendType(methodNode.returnType, builder);
		}

		builder.append("\n");

		if(!ArrayUtil.contains(Modifier.ABSTRACT, methodNode.modifiers) && !ArrayUtil.contains(Modifier.NATIVE, methodNode.modifiers))
		{
			StringUtil.repeatSymbol(builder, '\t', indent);
			builder.append("{\n");
			StringUtil.repeatSymbol(builder, '\t', indent + 1);
			builder.append("// compiled code\n");
			StringUtil.repeatSymbol(builder, '\t', indent);
			builder.append("}\n");
		}
		builder.append("\n");
	}

	private static void appendTypes(List<TypeNode> types, StringBuilder builder, String split)
	{
		for(int i = 0; i < types.size(); i++)
		{
			if(i != 0)
				builder.append(split);

			appendType(types.get(i), builder);
		}
	}

	private static void appendTypeParameters(List<TypeParameterNode> typeParameterNodes, StringBuilder builder)
	{
		if(typeParameterNodes.isEmpty())
			return;

		builder.append("<");
		for(int i = 0; i < typeParameterNodes.size(); i++)
		{
			if(i != 0)
				builder.append(", ");

			TypeParameterNode typeParameterNode = typeParameterNodes.get(i);
			builder.append(typeParameterNode.name);

			if(!typeParameterNode.supers.isEmpty())
			{
				builder.append(" : ");
				appendTypes(typeParameterNode.supers, builder, " & ");
			}
		}
		builder.append(">");
	}

	public static void appendType(TypeNode typeNode, StringBuilder builder)
	{
		appendAnnotations(typeNode, builder, -1);

		typeNode.typeConstructorNode.accept(new DummyNodeVisitor<StringBuilder>()
		{
			@Override
			public Void visitClassTypeNode(ClassTypeNode classTypeNode, StringBuilder a2)
			{
				a2.append(classTypeNode.className);

				return super.visitClassTypeNode(classTypeNode, a2);
			}

			@Override
			public Void visitThisTypeNode(ThisTypeNode thisTypeNode, StringBuilder a2)
			{
				a2.append("this");

				return super.visitThisTypeNode(thisTypeNode, a2);
			}

			@Override
			public Void visitMethodTypeNode(MethodTypeNode methodTypeNode, StringBuilder a2)
			{
				a2.append("{");
				a2.append("(");
				for(int i = 0; i < methodTypeNode.parameters.size(); i++)
				{
					if(i != 0)
						a2.append(", ");
					final MethodParameterNode parameterNode = methodTypeNode.parameters.get(i);
					appendVariableInfo(parameterNode.modifiers, parameterNode.name, parameterNode.returnType, parameterNode.defaultValue, a2);
				}
				a2.append(")");
				a2.append(" -> ");
				appendType(methodTypeNode.returnType, a2);
				a2.append("}");
				return super.visitMethodTypeNode(methodTypeNode, a2);
			}

			@Override
			public Void visitMultiTypeNode(MultiTypeNode multiTypeNode, StringBuilder a2)
			{
				a2.append("[");
				for(int i = 0; i < multiTypeNode.variables.size(); i++)
				{
					if(i != 0)
						a2.append(", ");

					final VariableNode variableNode = multiTypeNode.variables.get(i);

					appendVariableInfo(variableNode.modifiers, variableNode.name, variableNode.returnType, null, a2);
				}
				a2.append("]");
				return super.visitMultiTypeNode(multiTypeNode, a2);
			}

			@Override
			public Void visitTypeParameterValueTypeNode(TypeParameterValueTypeNode typeParameterValueTypeNode, StringBuilder a2)
			{
				a2.append(typeParameterValueTypeNode.name);

				return super.visitTypeParameterValueTypeNode(typeParameterValueTypeNode, a2);
			}
		}, builder);

		if(!typeNode.arguments.isEmpty())
		{
			builder.append("<");
			appendTypes(typeNode.arguments, builder, ", ");
			builder.append(">");
		}

		if(typeNode.nullable)
			builder.append("?");
	}

	private static void appendAnnotations(AnnotableNode<?> annotableNode, StringBuilder builder, int indent)
	{
		if(!annotableNode.annotations.isEmpty())
		{
			for(AnnotationNode annotationNode : annotableNode.annotations)
			{
				if(indent > 0)
					StringUtil.repeatSymbol(builder, '\t', indent);

				builder.append("@");

				TypeNode typeNode = null;
				for(Instruction instruction : annotationNode.code.instructions)
				{
					if(instruction instanceof NewObjectInstruction)
					{
						typeNode = ((NewObjectInstruction) instruction).value;
					}
				}

				if(typeNode == null)
					throw new IllegalArgumentException();

				appendType(typeNode, builder);

				if(indent >= 0)
					builder.append("\n");
				else
					builder.append(" ");
			}
		}
	}

	private static void appendVariableInfo(Modifier[] modifiers, Name name, TypeNode typeNode, String defaultValue, StringBuilder builder)
	{
		if(ArrayUtil.contains(Modifier.REF, modifiers))
			builder.append("ref ");
		else if(ArrayUtil.contains(Modifier.MUTABLE, modifiers))
			builder.append("var ");
		else
			builder.append("val ");
		builder.append(name);
		builder.append(" : ");
		appendType(typeNode, builder);
		if(defaultValue != null)
		{
			builder.append(" = ").append(defaultValue);
		}
	}

	private static void appendModifiers(Modifier[] modifiers, StringBuilder builder)
	{
		for(Modifier modifier : modifiers)
		{
			if(modifier == Modifier.MUTABLE || modifier == Modifier.REF)
				continue;

			builder.append(modifier.name().toLowerCase()).append(" ");
		}
	}
}
