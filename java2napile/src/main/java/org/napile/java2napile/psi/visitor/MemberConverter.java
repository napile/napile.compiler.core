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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.util.Function;

/**
 * @author VISTALL
 * @date 15:54/04.01.13
 */
public class MemberConverter
{
	public static String convertVariableDecl(PsiVariable variable, ConverterVisitor visitor)
	{
		StringBuilder b = new StringBuilder();

		ModifierConverter.modifiers(variable, b);

		if(variable.hasModifierProperty(PsiModifier.FINAL))
			b.append("val").append(ConverterVisitor.SPACE);
		else
			b.append("var").append(ConverterVisitor.SPACE);

		b.append(variable.getName());

		String renderType = TypeConverter.convertType(variable.getType(), true);
		if(renderType != null)
		{
			b.append(ConverterVisitor.SPACE);
			b.append(":");
			b.append(ConverterVisitor.SPACE);
			b.append(renderType);
		}

		PsiExpression expression = variable.getInitializer();
		if(expression != null)
		{
			ExpressionConverter e = new ExpressionConverter(b, visitor);

			b.append(ConverterVisitor.SPACE);
			b.append("=");
			b.append(ConverterVisitor.SPACE);

			expression.accept(e);
		}

		return b.toString();
	}

	public static String convertConstructorDecl(PsiMethod method, final ConverterVisitor visitor)
	{
		StringBuilder b = new StringBuilder();

		ModifierConverter.modifiers(method, b);

		b.append("this");

		renderParameters(b, method, visitor);

		PsiCodeBlock codeBlock = method.getBody();
		PsiStatement[] statements = codeBlock == null ? null : codeBlock.getStatements();
		if(statements != null)
		{
			PsiStatement stmt = statements.length == 0 ? null : statements[0];
			if(stmt instanceof PsiExpressionStatement && ((PsiExpressionStatement) stmt).getExpression() instanceof PsiMethodCallExpression)
			{
				PsiMethodCallExpression methodCallExpression = (PsiMethodCallExpression) ((PsiExpressionStatement) stmt).getExpression();

				PsiMethod resolvedMethod = methodCallExpression.resolveMethod();
				if(resolvedMethod != null && resolvedMethod.isConstructor())
				{
					b.append(" : ");
					if(resolvedMethod.getParent() == method.getParent())
						b.append("this");
					else
						b.append(((PsiClass) resolvedMethod.getParent()).getName());
					b.append("(");

					b.append(StringUtil.join(methodCallExpression.getArgumentList().getExpressions(), new Function<PsiExpression, String>()
					{
						@Override
						public String fun(PsiExpression psiExpression)
						{
							StringBuilder b = new StringBuilder();
							ExpressionConverter converter = new ExpressionConverter(b, visitor);
							psiExpression.accept(converter);
							return b.toString();
						}
					}, ", "));
					b.append(")");
				}
			}
		}

		return b.toString();
	}

	public static String convertMethodDecl(PsiMethod method, ConverterVisitor visitor)
	{
		StringBuilder b = new StringBuilder();

		ModifierConverter.modifiers(method, b);

		b.append("meth").append(ConverterVisitor.SPACE).append(method.getName());

		renderParameters(b, method, visitor);

		String renderType = TypeConverter.convertType(method.getReturnType(), true);
		if(renderType != null)
			b.append(ConverterVisitor.SPACE).append(":").append(ConverterVisitor.SPACE).append(renderType);

		PsiCodeBlock codeBlock = method.getBody();
		if(codeBlock != null)
		{
			b.append(ConverterVisitor.LINE);
			visitor.tabs(b, 0);
			b.append("{").append(ConverterVisitor.LINE);

			ExpressionConverter converter = new ExpressionConverter(b, visitor);
			for(PsiStatement statement : codeBlock.getStatements())
			{
				visitor.tabs(b, 1);

				statement.accept(converter);

				b.append(ConverterVisitor.LINE);
			}

			visitor.tabs(b, 0);
			b.append("}");
		}

		return b.toString();
	}

	private static void renderParameters(StringBuilder builder, PsiMethod method, final ConverterVisitor visitor)
	{
		builder.append("(");
		builder.append(StringUtil.join(method.getParameterList().getParameters(), new Function<PsiParameter, String>()
		{
			@Override
			public String fun(PsiParameter psiParameter)
			{
				return convertVariableDecl(psiParameter, visitor);
			}
		}, ", "));
		builder.append(")");
	}
}
