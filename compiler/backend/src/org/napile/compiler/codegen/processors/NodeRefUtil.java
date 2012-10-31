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
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.VariableRef;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.compiler.codegen.processors.codegen.CallTransformer;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.resolve.DescriptorUtils;

/**
 * @author VISTALL
 * @date 22:10/07.09.12
 */
public class NodeRefUtil
{
	public static MethodRef constructorRef(@NotNull FqName fqName)
	{
		return new MethodRef(fqName.child(Name.identifier("this")), Collections.<TypeNode>emptyList(), Collections.<TypeNode>emptyList(), new TypeNode(false, new ClassTypeNode(fqName)));
	}

	public static VariableRef ref(@NotNull ClassNode classNode, @NotNull VariableNode variableNode)
	{
		return new VariableRef(classNode.name.child(variableNode.name), variableNode.returnType);
	}

	public static VariableRef ref(@NotNull PropertyDescriptor propertyDescriptor)
	{
		propertyDescriptor = propertyDescriptor.getOriginal();
		return new VariableRef(DescriptorUtils.getFQName(propertyDescriptor).toSafe(), TypeTransformer.toAsmType(propertyDescriptor.getType()));
	}

	public static MethodRef ref(@NotNull MethodDescriptor descriptor)
	{
		return ref(descriptor, DescriptorUtils.getFQName(descriptor).toSafe());
	}

	public static MethodRef ref(@NotNull MethodDescriptor descriptor, @NotNull FqName fqName)
	{
		descriptor = CallTransformer.unwrapFakeOverride(descriptor);

		List<TypeNode> typeNodes = new ArrayList<TypeNode>(descriptor.getValueParameters().size());
		for(ParameterDescriptor p : descriptor.getValueParameters())
			typeNodes.add(TypeTransformer.toAsmType(p.getType()));

		return new MethodRef(fqName, typeNodes, Collections.<TypeNode>emptyList(), TypeTransformer.toAsmType(descriptor.getReturnType()));
	}
}
