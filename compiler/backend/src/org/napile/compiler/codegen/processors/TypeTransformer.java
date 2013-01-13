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

package org.napile.compiler.codegen.processors;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.Modifier;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.asm.tree.members.types.constructors.MethodTypeNode;
import org.napile.asm.tree.members.types.constructors.MultiTypeNode;
import org.napile.asm.tree.members.types.constructors.ThisTypeNode;
import org.napile.asm.tree.members.types.constructors.TypeConstructorNode;
import org.napile.asm.tree.members.types.constructors.TypeParameterValueTypeNode;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.MethodTypeConstructor;
import org.napile.compiler.lang.types.MultiTypeConstructor;
import org.napile.compiler.lang.types.MultiTypeEntry;
import org.napile.compiler.lang.types.SelfTypeConstructor;

/**
 * @author VISTALL
 * @date 0:48/07.09.12
 */
public class TypeTransformer
{
	@NotNull
	public static TypeNode toAsmType(BindingTrace bindingTrace, @NotNull JetType jetType, ClassNode classNode)
	{
		TypeConstructorNode typeConstructorNode = null;
		ClassifierDescriptor owner = jetType.getConstructor().getDeclarationDescriptor();
		if(jetType.getConstructor() instanceof SelfTypeConstructor)
			typeConstructorNode = new ThisTypeNode();
		else if(jetType.getConstructor() instanceof MethodTypeConstructor)
		{
			MethodTypeConstructor methodTypeConstructor = (MethodTypeConstructor)jetType.getConstructor();
			typeConstructorNode = new MethodTypeNode();

			MethodTypeNode methodTypeNode = (MethodTypeNode) typeConstructorNode;

			methodTypeNode.returnType = toAsmType(bindingTrace, methodTypeConstructor.getReturnType(), classNode);
			for(Map.Entry<Name, JetType> entry : methodTypeConstructor.getParameterTypes().entrySet())
				methodTypeNode.parameters.add(new MethodParameterNode(Modifier.EMPTY, entry.getKey(), toAsmType(bindingTrace, entry.getValue(), classNode)));
		}
		else if(jetType.getConstructor() instanceof MultiTypeConstructor)
		{
			MultiTypeConstructor multiTypeConstructor = (MultiTypeConstructor) jetType.getConstructor();

			typeConstructorNode = new MultiTypeNode();

			MultiTypeNode multiTypeNode = (MultiTypeNode) typeConstructorNode;

			for(MultiTypeEntry entry : multiTypeConstructor.getEntries())
			{
				boolean mutable = entry.mutable != null && entry.mutable;
				Name name = entry.name == null ? Name.identifier("p" + entry.index) : entry.name;
				multiTypeNode.variables.add(new VariableNode(mutable ? Modifier.list(Modifier.MUTABLE) : Modifier.EMPTY, name, toAsmType(bindingTrace, entry.type, classNode)));
			}
		}
		else if(owner instanceof ClassDescriptor)
			typeConstructorNode = new ClassTypeNode(FqNameGenerator.getFqName(owner, bindingTrace));
		else if(owner instanceof TypeParameterDescriptor)
			typeConstructorNode = new TypeParameterValueTypeNode(owner.getName());
		else
			throw new RuntimeException("invalid " + jetType.getConstructor());

		TypeNode typeNode = new TypeNode(jetType.isNullable(), typeConstructorNode);
		for(JetType argument : jetType.getArguments())
			typeNode.arguments.add(toAsmType(bindingTrace, argument, classNode));

		AnnotationCodegen.gen(bindingTrace, jetType, typeNode, classNode);

		return typeNode;
	}
}
