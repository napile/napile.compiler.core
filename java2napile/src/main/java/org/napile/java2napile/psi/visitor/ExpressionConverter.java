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

package org.napile.java2napile.psi.visitor;

import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReturnStatement;

/**
 * @author VISTALL
 * @date 16:18/04.01.13
 */
public class ExpressionConverter extends JavaElementVisitor
{
	private final StringBuilder builder;

	public ExpressionConverter(StringBuilder builder)
	{
		this.builder = builder;

	}

	@Override
	public void visitReturnStatement(PsiReturnStatement statement)
	{
		builder.append("return");
		PsiExpression returnVal = statement.getReturnValue();
		if(returnVal != null)
		{
			builder.append(ConverterVisitor.SPACE);
			returnVal.accept(this);
		}
	}

	@Override
	public void visitElement(PsiElement element)
	{
		element.acceptChildren(this);
	}

	@Override
	public void visitLiteralExpression(PsiLiteralExpression expression)
	{
		String text = expression.getText();
		if(text.equals("null"))
			builder.append(text);
		else if(text.endsWith("L") || text.endsWith("l") || text.endsWith("f") || text.endsWith("F") || text.endsWith("d") || text.endsWith("D"))
			builder.append(text.substring(0, text.length() - 1));
		else
			builder.append(text);
	}
}
