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

package org.napile.compiler.util;

import org.napile.asm.tree.members.types.TypeNode;

/**
 * @author VISTALL
 * @since 12:07/16.02.13
 */
public class AsmToStringUtil
{
	public static String typeToString(TypeNode typeNode)
	{
		final StringBuilder builder = new StringBuilder();
		NodeToStringBuilder.appendType(typeNode, builder);
		return builder.toString();
	}
}
