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

package org.napile.compiler.lang.psi.util;

import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.resolve.BindingReader;

/**
 * @author VISTALL
 * @date 12:28/27.02.13
 */
public class ConstantUtil
{
	public static Constant getConstant(BindingReader trace, NapileExpression e)
	{
		ConstantVisitor visitor = new ConstantVisitor(trace);

		return e.accept(visitor, null);
	}
}
