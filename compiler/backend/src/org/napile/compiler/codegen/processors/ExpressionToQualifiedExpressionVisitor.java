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

package org.napile.compiler.codegen.processors;

import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileVisitorVoid;

/**
 * @author VISTALL
 * @date 12:50/19.02.13
 * TODO [VISTALL] make converted from ArrayUtil.copy() to napile.lang.ArrayUtil.copy()
 */
public class ExpressionToQualifiedExpressionVisitor extends NapileVisitorVoid
{
	@Nullable
	public static String convert(@Nullable NapileExpression e)
	{
		if(e == null)
			return null;
		ExpressionToQualifiedExpressionVisitor expressionToQualifiedExpressionVisitor = new ExpressionToQualifiedExpressionVisitor();
		e.accept(expressionToQualifiedExpressionVisitor);
		return expressionToQualifiedExpressionVisitor.getText();
	}

	private final StringBuilder builder = new StringBuilder();

	public ExpressionToQualifiedExpressionVisitor()
	{

	}

	@Override
	public void visitExpression(NapileExpression expression)
	{
		builder.append(expression.getText());
	}

	public String getText()
	{
		return builder.toString();
	}
}
