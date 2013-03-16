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

import java.util.List;

import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.ExpressionCodegen;
import org.napile.compiler.codegen.processors.PositionMarker;
import org.napile.compiler.codegen.processors.codegen.CallTransformer;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;

/**
 * @author VISTALL
 * @since 2:39/20.09.12
 */
public class CollectionElement extends StackValue
{
	private ResolvedCall<MethodDescriptor> getCall;
	private ResolvedCall<MethodDescriptor> setCall;
	private ExpressionCodegen expressionCodegen;

	private CallableMethod getCallableMethod;
	private CallableMethod setCallableMethod;

	public CollectionElement(TypeNode type, ResolvedCall<MethodDescriptor> getCall, ResolvedCall<MethodDescriptor> setCall, ExpressionCodegen gen)
	{
		super(null, type);
		this.getCall = getCall;
		this.setCall = setCall;
		this.expressionCodegen = gen;

		getCallableMethod = getCall == null ? null : CallTransformer.transformToCallable(gen, getCall, false, false, false);
		setCallableMethod = setCall == null ? null : CallTransformer.transformToCallable(gen, setCall, false, false, false);
	}

	@Override
	public void put(TypeNode type, InstructionAdapter instructionAdapter, PositionMarker positionMarker)
	{
		assert getCallableMethod != null;

		getCallableMethod.invoke(instructionAdapter, PositionMarker.EMPTY, null);

		castTo(type, instructionAdapter);
	}

	@Override
	public void store(TypeNode topOfStackType, InstructionAdapter v, PositionMarker positionMarker) {

		assert setCallableMethod != null;

		List<TypeNode> argumentTypes = setCallableMethod.getValueParameterTypes();
		castTo(topOfStackType, argumentTypes.get(argumentTypes.size() - 1), v);

		setCallableMethod.invoke(v, PositionMarker.EMPTY, null);

		//Type returnType = asmMethod.getReturnType();
		//if (returnType != Type.VOID_TYPE)
		//	pop(returnType, v);
	}
}
