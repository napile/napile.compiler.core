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

package org.napile.compiler.codegen.processors.visitors.loopCodegen;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileLabelExpression;

/**
 * @author VISTALL
 * @since 16:19/03.10.12
 */
public class LabelLoopCodegen extends LoopCodegen<NapileLabelExpression>
{
	public LabelLoopCodegen(@NotNull NapileLabelExpression expression)
	{
		super(expression);
	}

	@Override
	public String getName()
	{
		return expression.getLabelName();
	}
}
