/*
 * Copyright 2010-2013 napile.org
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
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.lang.descriptors.VariableDescriptor;

/**
 * @author VISTALL
 * @date 13:04/13.01.13
 */
public class WrappedVar extends StackValue
{
	private final StackValue stackValue;
	private final VariableDescriptor variableDescriptor;

	public WrappedVar(ExpressionCodegen gen, VariableDescriptor variableDescriptor)
	{
		super(null, gen.toAsmType(variableDescriptor.getType()));

		this.variableDescriptor = variableDescriptor;

		stackValue = StackValue.simpleVariableAccessor(gen, variableDescriptor, variableDescriptor.isStatic() ? CallableMethod.CallType.STATIC : CallableMethod.CallType.SPECIAL);
	}

	@Override
	public void put(TypeNode type, InstructionAdapter instructionAdapter, PositionMarker positionMarker)
	{
		stackValue.put(type, instructionAdapter, PositionMarker.EMPTY);
	}

	@Override
	public void store(TypeNode topOfStackType, InstructionAdapter instructionAdapter, PositionMarker positionMarker)
	{
		stackValue.store(stackValue.getType(), instructionAdapter, PositionMarker.EMPTY);
	}

	public void putReceiver(ExpressionCodegen gen)
	{
		if(stackValue.receiverSize() == 1)
			gen.generateFromResolvedCall(variableDescriptor.getExpectedThisObject(), AsmConstants.ANY_TYPE);
	}
}
