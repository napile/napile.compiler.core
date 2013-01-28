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

import java.util.Arrays;
import java.util.Collections;

import org.napile.asm.AsmConstants;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.AsmNodeUtil;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;

/**
 * @author VISTALL
 * @date 18:55/05.01.13
 */
public class MultiVariable extends StackValue
{
	public static final MethodRef SET_VALUE = new MethodRef(NapileLangPackage.MULTI.child(Name.identifier("set")), Arrays.asList(AsmNodeUtil.parameterNode("index", AsmConstants.INT_TYPE), AsmNodeUtil.parameterNode("object", TypeConstants.ANY_NULLABLE)), Collections.<TypeNode>emptyList(), AsmConstants.NULL_TYPE);
	public static final MethodRef GET_VALUE = new MethodRef(NapileLangPackage.MULTI.child(Name.identifier("get")), Collections.<MethodParameterNode>singletonList(AsmNodeUtil.parameterNode("index", AsmConstants.INT_TYPE)), Collections.<TypeNode>emptyList(), TypeConstants.ANY_NULLABLE);

	private final int index;

	public MultiVariable(TypeNode type, int index)
	{
		super(type);
		this.index = index;
	}

	@Override
	public void put(TypeNode type, InstructionAdapter instructionAdapter)
	{
		instructionAdapter.newInt(index);

		instructionAdapter.invokeVirtual(GET_VALUE, false);
	}

	@Override
	public void store(TypeNode topOfStackType, InstructionAdapter instructionAdapter)
	{
		instructionAdapter.newInt(index);

		// VALUE2
		// VALUE1
		// FIXME [VISTALL] very stupied, but it works :D for now
		instructionAdapter.swap();

		instructionAdapter.invokeVirtual(SET_VALUE, false);
	}
}
