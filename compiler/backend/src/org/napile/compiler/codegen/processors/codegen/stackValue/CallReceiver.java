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

import org.jetbrains.annotations.Nullable;
import org.napile.asm.adapters.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.ExpressionGenerator;
import org.napile.compiler.codegen.processors.TypeTransformer;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;

public class CallReceiver extends StackValue
{
	private final ResolvedCall<? extends CallableDescriptor> resolvedCall;
	final StackValue receiver;
	private final ExpressionGenerator codegen;
	private final CallableMethod callableMethod;

	public CallReceiver(ResolvedCall<? extends CallableDescriptor> resolvedCall, StackValue receiver, ExpressionGenerator codegen, CallableMethod callableMethod)
	{
		super(calcType(resolvedCall, codegen, callableMethod));
		this.resolvedCall = resolvedCall;
		this.receiver = receiver;
		this.codegen = codegen;
		this.callableMethod = callableMethod;
	}

	private static TypeNode calcType(ResolvedCall<? extends CallableDescriptor> resolvedCall, ExpressionGenerator codegen, CallableMethod callableMethod)
	{
		ReceiverDescriptor thisObject = resolvedCall.getThisObject();
		ReceiverDescriptor receiverArgument = resolvedCall.getReceiverArgument();

		CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();

		if(thisObject.exists())
		{
			if(callableMethod != null)
			{
				if(receiverArgument.exists())
				{
					throw new UnsupportedOperationException();
					//return callableMethod.getReceiverClass();
				}
				else
					return TypeTransformer.toAsmType(((ClassDescriptor) descriptor.getContainingDeclaration()).getDefaultType());
			}
			else
			{
				if(receiverArgument.exists())
				{
					throw new UnsupportedOperationException();
					//return codegen.typeMapper.mapType(descriptor.getReceiverParameter().getType(), JetTypeMapperMode.VALUE);
				}
				else
					return TypeTransformer.toAsmType(descriptor.getExpectedThisObject().getType());
			}
		}
		else
		{
			if(receiverArgument.exists())
			{
				/*if(callableMethod != null)
				{
					return callableMethod.getReceiverClass();
				}
				else
				{
					return codegen.typeMapper.mapType(descriptor.getReceiverParameter().getType(), JetTypeMapperMode.VALUE);
				}  */
				throw new UnsupportedOperationException();
			}
			else
				return TypeConstants.NULL;
		}
	}

	@Override
	public void put(TypeNode type, InstructionAdapter v)
	{
		CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();

		ReceiverDescriptor thisObject = resolvedCall.getThisObject();
		ReceiverDescriptor receiverArgument = resolvedCall.getReceiverArgument();
		if(thisObject.exists())
		{
			if(receiverArgument.exists())
			{
				/*if(callableMethod != null)
				{
					codegen.generateFromResolvedCall(thisObject, callableMethod.getOwner().getAsmType());
				}
				else
				{
					codegen.generateFromResolvedCall(thisObject, codegen.typeMapper.mapType(descriptor.getExpectedThisObject().getType(), JetTypeMapperMode.VALUE));
				}
				genReceiver(v, receiverArgument, type, descriptor.getReceiverParameter(), 1);    */
				throw new UnsupportedOperationException();
			}
			else
				genReceiver(v, thisObject, type, null, 0);
		}
		else
		{
			if(receiverArgument.exists())
				genReceiver(v, receiverArgument, type, descriptor.getReceiverParameter(), 0);
		}
	}

	private void genReceiver(InstructionAdapter v, ReceiverDescriptor receiverArgument, TypeNode type, @Nullable ReceiverDescriptor receiverParameter, int depth)
	{
		if(receiver == StackValue.none())
		{
			if(receiverParameter != null)
			{
				TypeNode receiverType = TypeTransformer.toAsmType(receiverParameter.getType());
				codegen.generateFromResolvedCall(receiverArgument, receiverType);
				StackValue.onStack(receiverType).put(type, v);
			}
			else
				codegen.generateFromResolvedCall(receiverArgument, type);
		}
		else
			receiver.moveToTopOfStack(type, v, depth);
	}
}