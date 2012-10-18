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
import org.napile.compiler.codegen.processors.ExpressionGenerator;
import org.napile.compiler.lang.descriptors.ClassDescriptor;

public class ThisOuter extends StackValue
{
	private final ExpressionGenerator codegen;
	private final ClassDescriptor descriptor;
	private final boolean isSuper;

	public ThisOuter(ExpressionGenerator codegen, ClassDescriptor descriptor, boolean isSuper)
	{
		super(AsmConstants.ANY_TYPE);
		this.codegen = codegen;
		this.descriptor = descriptor;
		this.isSuper = isSuper;
	}

	@Override
	public void put(TypeNode type, InstructionAdapter v)
	{
		final StackValue stackValue = codegen.generateThisOrOuter(descriptor, isSuper);
		stackValue.put(stackValue.getType(), v);  // no coercion here
	}
}
