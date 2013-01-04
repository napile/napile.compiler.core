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

import org.jetbrains.annotations.NotNull;
import org.napile.asm.AsmConstants;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;

public class SimpleVariableAccessor extends StackValue
{
	private final MethodRef setter;
	private final MethodRef getter;
	private final CallableMethod.CallType callType;

	public SimpleVariableAccessor(@NotNull FqName fqName, @NotNull TypeNode type, CallableMethod.CallType s)
	{
		super(type);
		callType = s;
		// convert 'A.var' -> A + var$set -> A.var$set
		FqName setterFq = fqName.parent().child(Name.identifier(fqName.shortName() + "$set"));
		setter = new MethodRef(setterFq, Collections.singletonList(getType()), Collections.<TypeNode>emptyList(), AsmConstants.NULL_TYPE);
		// convert 'A.var' -> A + var$get -> A.var$get
		FqName getterFq = fqName.parent().child(Name.identifier(fqName.shortName() + "$get"));
		getter = new MethodRef(getterFq, Collections.<TypeNode>emptyList(), Collections.<TypeNode>emptyList(), getType());
	}

	@Override
	public void put(TypeNode type, InstructionAdapter instructionAdapter)
	{
		switch(callType)
		{
			case SPECIAL:
				instructionAdapter.invokeSpecial(getter, false);
				break;
			case STATIC:
				instructionAdapter.invokeStatic(getter, false);
				break;
			case VIRTUAL:
				instructionAdapter.invokeVirtual(getter, false);
				break;
		}

		castTo(type, instructionAdapter);
	}

	@Override
	public void store(TypeNode topOfStackType, InstructionAdapter instructionAdapter)
	{
		switch(callType)
		{
			case SPECIAL:
				instructionAdapter.invokeSpecial(setter, false);
				break;
			case STATIC:
				instructionAdapter.invokeStatic(setter, false);
				break;
			case VIRTUAL:
				instructionAdapter.invokeVirtual(setter, false);
				break;
		}
		instructionAdapter.pop();
	}

	@Override
	public int receiverSize()
	{
		return callType == CallableMethod.CallType.VIRTUAL || callType == CallableMethod.CallType.SPECIAL ? 1 : 0;
	}

	@Override
	public void dupReceiver(InstructionAdapter v)
	{
		if(callType == CallableMethod.CallType.VIRTUAL)
			v.dup();
	}
}
