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
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.MethodTypeConstructor;
import org.napile.compiler.lang.types.MultiTypeConstructor;
import org.napile.compiler.lang.types.MultiTypeEntry;
import org.napile.compiler.lang.types.SelfTypeConstructor;

/**
 * @author VISTALL
 * @since 0:48/07.09.12
 */
public class TypeTransformer
{
	@NotNull
	public static TypeNode toAsmType(BindingTrace bindingTrace, @NotNull NapileType napileType, ClassNode classNode)
	{
		TypeConstructorNode typeConstructorNode = null;
		ClassifierDescriptor owner = napileType.getConstructor().getDeclarationDescriptor();
		if(napileType.getConstructor() instanceof SelfTypeConstructor)
			typeConstructorNode = new ThisTypeNode();
		else if(napileType.getConstructor() instanceof MethodTypeConstructor)
		{
			MethodTypeConstructor methodTypeConstructor = (MethodTypeConstructor) napileType.getConstructor();
			typeConstructorNode = new MethodTypeNode();

			MethodTypeNode methodTypeNode = (MethodTypeNode) typeConstructorNode;

			methodTypeNode.name = methodTypeConstructor.getExpectedName();
			methodTypeNode.returnType = toAsmType(bindingTrace, methodTypeConstructor.getReturnType(), classNode);
			for(Map.Entry<Name, NapileType> entry : methodTypeConstructor.getParameterTypes().entrySet())
				methodTypeNode.parameters.add(new MethodParameterNode(Modifier.EMPTY, entry.getKey(), toAsmType(bindingTrace, entry.getValue(), classNode)));
		}
		else if(napileType.getConstructor() instanceof MultiTypeConstructor)
		{
			MultiTypeConstructor multiTypeConstructor = (MultiTypeConstructor) napileType.getConstructor();

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
			throw new RuntimeException("invalid " + napileType.getConstructor());

		TypeNode typeNode = new TypeNode(napileType.isNullable(), typeConstructorNode);
		for(NapileType argument : napileType.getArguments())
			typeNode.arguments.add(toAsmType(bindingTrace, argument, classNode));

		AnnotationCodegen.gen(bindingTrace, napileType, typeNode, classNode);

		return typeNode;
	}
}
