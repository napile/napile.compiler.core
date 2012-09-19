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

package org.napile.compiler.codegen.processors.codegen;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.TypeTransformer;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.resolve.DescriptorUtils;

/**
 * @author VISTALL
 * @date 14:59/18.09.12
 */
public class CallTransformer
{
	public static IntrinsicMethod findIntrinsicMethod(@NotNull CallableDescriptor callableDescriptor)
	{
		return null;
	}

	@NotNull
	public static Callable transformToCallable(MethodDescriptor methodDescriptor)
	{
		CallableMethod.CallType type = CallableMethod.CallType.VIRTUAL;
		if(methodDescriptor instanceof ConstructorDescriptor)
			type = CallableMethod.CallType.SPECIAL;
		else if(methodDescriptor.isStatic())
			type = CallableMethod.CallType.STATIC;

		FqName fqName = DescriptorUtils.getFQName(methodDescriptor).toSafe();
		List<TypeNode> parameters = new ArrayList<TypeNode>(methodDescriptor.getValueParameters().size());
		for(ParameterDescriptor p : methodDescriptor.getValueParameters())
			parameters.add(TypeTransformer.toAsmType(p.getType()));

		return new CallableMethod(new MethodRef(fqName, parameters, TypeTransformer.toAsmType(methodDescriptor.getReturnType())), type);
	}
}
