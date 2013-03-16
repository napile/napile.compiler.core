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

package org.napile.compiler.codegen.processors.visitors;

import org.napile.compiler.codegen.processors.ExpressionCodegen;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.psi.NapileVisitor;

/**
 * @author VISTALL
 * @since 15:20/21.01.13
 */
public class CodegenVisitor extends NapileVisitor<StackValue, StackValue>
{
	public final ExpressionCodegen gen;

	public CodegenVisitor(ExpressionCodegen gen)
	{
		this.gen = gen;
	}
}
