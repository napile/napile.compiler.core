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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.adapters.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.types.TypeNode;

/**
 * @author VISTALL
 * @date 14:51/18.09.12
 */
public class CallableMethod
{
	public static enum CallType
	{
		SPECIAL,
		STATIC,
		VIRTUAL
	}

	private final MethodRef methodRef;
	private final CallType callType;
	private final TypeNode returnType;
	private final List<TypeNode> parameters;

	public CallableMethod(@NotNull MethodRef methodRef, @NotNull CallType callType, TypeNode returnType, List<TypeNode> parameters)
	{
		this.methodRef = methodRef;
		this.callType = callType;
		this.returnType = returnType;
		this.parameters = parameters;
	}

	public void invoke(InstructionAdapter instructionAdapter)
	{
		switch(callType)
		{
			case SPECIAL:
				instructionAdapter.invokeSpecial(methodRef);
				break;
			case STATIC:
				instructionAdapter.invokeStatic(methodRef);
				break;
			case VIRTUAL:
				instructionAdapter.invokeVirtual(methodRef);
				break;
		}
	}

	public TypeNode getReturnType()
	{
		return returnType;
	}

	public List<TypeNode> getValueParameterTypes()
	{
		return parameters;
	}

	public void invokeWithDefault(InstructionAdapter instructionAdapter, int mask)
	{
		throw new UnsupportedOperationException();
	}

	public String getName()
	{
		return methodRef.method.shortName().getName();
	}

	public CallType getCallType()
	{
		return callType;
	}
}
