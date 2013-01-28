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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.Modifier;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.VariableRef;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.codegen.CallTransformer;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.resolve.BindingTrace;

/**
 * @author VISTALL
 * @date 22:10/07.09.12
 */
public class AsmNodeUtil
{
	//public static MethodRef constructorRef(@NotNull FqName fqName)
	//{
	//	return new MethodRef(fqName.child(Name.identifier("this")), Collections.<TypeNode>emptyList(), Collections.<TypeNode>emptyList(), new TypeNode(false, new ClassTypeNode(fqName)));
	//}

	public static MethodParameterNode parameterNode(String name, TypeNode typeNode)
	{
		return parameterNode(Modifier.EMPTY, name, typeNode);
	}

	public static MethodParameterNode parameterNode(Modifier[] modifiers, String name, TypeNode typeNode)
	{
		return new MethodParameterNode(modifiers, Name.identifier(name), typeNode);
	}

	public static VariableRef ref(@NotNull ClassNode classNode, @NotNull VariableNode variableNode)
	{
		return new VariableRef(classNode.name.child(variableNode.name), variableNode.returnType);
	}

	public static VariableRef ref(@NotNull VariableDescriptor propertyDescriptor, @NotNull BindingTrace bindingTrace, @NotNull ClassNode classNode)
	{
		propertyDescriptor = (VariableDescriptor) propertyDescriptor.getOriginal();
		return new VariableRef(FqNameGenerator.getFqName(propertyDescriptor, bindingTrace), TypeTransformer.toAsmType(bindingTrace, propertyDescriptor.getType(), classNode));
	}

	public static MethodRef ref(@NotNull MethodDescriptor descriptor, @NotNull BindingTrace bindingTrace, @NotNull ClassNode classNode)
	{
		return ref(descriptor, FqNameGenerator.getFqName(descriptor, bindingTrace), bindingTrace, classNode);
	}

	public static MethodRef ref(@NotNull MethodDescriptor descriptor, @NotNull FqName fqName, @NotNull BindingTrace bindingTrace, @NotNull ClassNode classNode)
	{
		descriptor = CallTransformer.unwrapFakeOverride(descriptor);

		List<MethodParameterNode> typeNodes = new ArrayList<MethodParameterNode>(descriptor.getValueParameters().size());
		for(CallParameterDescriptor p : descriptor.getValueParameters())
			typeNodes.add(new MethodParameterNode(ModifierCodegen.gen(p), p.getName(), TypeTransformer.toAsmType(bindingTrace, p.getType(), classNode)));

		return new MethodRef(fqName, typeNodes, Collections.<TypeNode>emptyList(), TypeTransformer.toAsmType(bindingTrace, descriptor.getReturnType(), classNode));
	}
}
