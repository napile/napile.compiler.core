/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.napile.compiler.lang.resolve.calls;

import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.ValueArgument;
import com.google.common.collect.Lists;

/**
 * @author abreslav
 */
public class VarargValueArgument implements ResolvedValueArgument
{
	private final List<ValueArgument> arguments = Lists.newArrayList();

	public void addArgument(@NotNull ValueArgument argument)
	{
		arguments.add(argument);
	}

	@Override
	@NotNull
	public List<ValueArgument> getArguments()
	{
		return arguments;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder("vararg:{");
		for(Iterator<ValueArgument> iterator = arguments.iterator(); iterator.hasNext(); )
		{
			ValueArgument valueArgument = iterator.next();
			NapileExpression expression = valueArgument.getArgumentExpression();
			builder.append(expression == null ? "no expression" : expression.getText());
			if(iterator.hasNext())
			{
				builder.append(", ");
			}
		}
		return builder.append("}").toString();
	}
}
