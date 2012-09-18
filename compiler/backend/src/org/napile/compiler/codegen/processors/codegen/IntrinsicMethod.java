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

import org.napile.asm.adapters.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.ExpressionGenerator;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.psi.NapileExpression;

/**
 * @author VISTALL
 * @date 15:10/18.09.12
 */
public class IntrinsicMethod implements Callable
{
	public StackValue generate(ExpressionGenerator expressionGenerator, InstructionAdapter instructs, TypeNode typeNode, NapileExpression expression, List<NapileExpression> napileExpressions, StackValue receiver)
	{
		throw new UnsupportedOperationException();
	}
}
