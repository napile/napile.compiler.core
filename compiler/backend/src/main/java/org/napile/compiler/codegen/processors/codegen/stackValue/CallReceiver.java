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

import org.napile.asm.AsmConstants;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.ExpressionCodegen;
import org.napile.compiler.codegen.processors.PositionMarker;
import org.napile.compiler.codegen.processors.TypeTransformer;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;

public class CallReceiver extends StackValue
{
	private final CallableDescriptor callableDescriptor;
	private final ReceiverDescriptor receiverDescriptor;
	private final StackValue receiver;
	private final ExpressionCodegen codegen;

	public CallReceiver(CallableDescriptor callableDescriptor, ReceiverDescriptor receiverDescriptor, StackValue receiver, ExpressionCodegen codegen, CallableMethod callableMethod)
	{
		super(null, calcType(callableDescriptor, receiverDescriptor, codegen, callableMethod));
		this.callableDescriptor = callableDescriptor;
		this.receiverDescriptor = receiverDescriptor;
		this.receiver = receiver;
		this.codegen = codegen;
	}

	private static TypeNode calcType(CallableDescriptor descriptor, ReceiverDescriptor thisObject, ExpressionCodegen codegen, CallableMethod callableMethod)
	{
		if(thisObject.exists())
		{
			if(callableMethod != null)
				return TypeTransformer.toAsmType(codegen.bindingTrace, ((ClassDescriptor) descriptor.getContainingDeclaration()).getDefaultType(), codegen.classNode);
			else
				return TypeTransformer.toAsmType(codegen.bindingTrace, descriptor.getExpectedThisObject().getType(), codegen.classNode);
		}
		else
			return AsmConstants.NULL_TYPE;
	}

	@Override
	public void put(TypeNode type, InstructionAdapter v, PositionMarker positionMarker)
	{
		if(receiverDescriptor.exists())
			genReceiver(v, receiverDescriptor, type, 0);
	}

	private void genReceiver(InstructionAdapter v, ReceiverDescriptor receiverArgument, TypeNode type, int depth)
	{
		if(receiver == StackValue.none())
			codegen.generateFromResolvedCall(receiverArgument, type);
		else
			receiver.moveToTopOfStack(type, v, depth);
	}
}