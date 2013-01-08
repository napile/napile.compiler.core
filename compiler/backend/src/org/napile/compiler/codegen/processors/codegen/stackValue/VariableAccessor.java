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

package org.napile.compiler.codegen.processors.codegen.stackValue;

import java.util.Collections;

import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.codegen.CallTransformer;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.resolve.BindingTrace;

/**
 * @author VISTALL
 * @date 23:06/08.12.12
 */
public class VariableAccessor extends StackValue
{
	private final CallableMethod callableMethod;

	public VariableAccessor(TypeNode type, MethodDescriptor methodDescriptor, BindingTrace bindingTrace, ClassNode classNode, boolean nullable)
	{
		super(type);

		callableMethod = CallTransformer.transformToCallable(bindingTrace, classNode, methodDescriptor, Collections.<TypeNode>emptyList(), nullable, false, false);
	}

	@Override
	public void put(TypeNode type, InstructionAdapter instructs)
	{
		if(!callableMethod.getName().endsWith("get"))
			throw new IllegalArgumentException("cant get to variable with incorrect getter : " + callableMethod.getName());

		callableMethod.invoke(instructs);
	}

	@Override
	public void store(TypeNode topOfStackType, InstructionAdapter instructs)
	{
		if(!callableMethod.getName().endsWith("set"))
			throw new IllegalArgumentException("cant set to variable with incorrect setter : " + callableMethod.getName());

		callableMethod.invoke(instructs);
		instructs.pop();
	}

	@Override
	public int receiverSize()
	{
		return callableMethod.getCallType() == CallableMethod.CallType.STATIC ? 0 : 1;
	}

	@Override
	public void dupReceiver(InstructionAdapter v)
	{
		if(callableMethod.getCallType() != CallableMethod.CallType.STATIC)
			v.dup();
	}

}
