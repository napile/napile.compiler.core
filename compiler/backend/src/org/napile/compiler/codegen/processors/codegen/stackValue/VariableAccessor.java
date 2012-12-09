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

import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.ExpressionCodegen;
import org.napile.compiler.codegen.processors.codegen.CallTransformer;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;

/**
 * @author VISTALL
 * @date 23:06/08.12.12
 */
public class VariableAccessor extends StackValue
{
	private final ResolvedCall<? extends CallableDescriptor> resolvedCall;
	private final StackValue receiver;
	private final CallableMethod callableMethod;
	private final ExpressionCodegen expressionCodegen;

	public VariableAccessor(TypeNode type, ResolvedCall<? extends CallableDescriptor> resolvedCall, StackValue receiver, ExpressionCodegen expressionCodegen)
	{
		super(type);
		this.resolvedCall = resolvedCall;
		this.receiver = receiver;
		this.expressionCodegen = expressionCodegen;
		callableMethod = CallTransformer.transformToCallable(resolvedCall, false, false);
	}

	@Override
	public void put(TypeNode type, InstructionAdapter instructs)
	{
		if(!callableMethod.getName().endsWith("get"))
			throw new IllegalArgumentException("cant get to variable with incorrect getter");

		StackValue r = StackValue.receiver(resolvedCall, receiver, expressionCodegen, callableMethod);

		r.put(receiver.getType(), instructs);

		expressionCodegen.invokeMethodWithArguments(callableMethod, resolvedCall, receiver);
	}

	@Override
	public void store(TypeNode topOfStackType, InstructionAdapter instructs)
	{
		if(!callableMethod.getName().endsWith("set"))
			throw new IllegalArgumentException("cant set to variable with incorrect setter");

		StackValue r = StackValue.receiver(resolvedCall, receiver, expressionCodegen, callableMethod);

		r.put(receiver.getType(), instructs);

		expressionCodegen.invokeMethodWithArguments(callableMethod, resolvedCall, receiver);
	}

	@Override
	public int receiverSize()
	{
		return resolvedCall.getResultingDescriptor().isStatic() ? 0 : 1;
	}
}
