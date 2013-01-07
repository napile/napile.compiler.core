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

import com.intellij.psi.*;

/**
 * @author VISTALL
 * @date 16:18/04.01.13
 */
public class ExpressionConverter extends JavaElementVisitor
{
	private final StringBuilder builder;
	private final ConverterVisitor converterVisitor;

	public ExpressionConverter(StringBuilder builder, ConverterVisitor v)
	{
		this.builder = builder;
		this.converterVisitor = v;
	}

	private void tabs(int val)
	{
		converterVisitor.tabs(builder, val + 1);
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
	public void visitNewExpression(PsiNewExpression expression)
	{
		PsiJavaCodeReferenceElement codeReferenceElement = expression.getClassOrAnonymousClassReference();
		if(codeReferenceElement != null)
		{
			builder.append(codeReferenceElement.getQualifiedName());
			PsiType[] typeParameters = codeReferenceElement.getTypeParameters();
			if(typeParameters.length > 0)
			{
				builder.append("<");
				for(int i = 0; i < typeParameters.length; i++)
				{
					if(i != 0)
						builder.append(", ");
					builder.append(TypeConverter.convertType(typeParameters[i], true));
				}
				builder.append(">");
			}

			PsiExpressionList argumentList = expression.getArgumentList();
			if(argumentList != null)
			{
				builder.append("(");
				PsiExpression[] exps = argumentList.getExpressions();
				for(int i = 0; i < exps.length; i++)
				{
					if(i != 0)
						builder.append(", ");
					exps[i].accept(this);
				}
				builder.append(")");
			}
		}
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

	@Override
	public void visitBinaryExpression(PsiBinaryExpression expression)
	{
		expression.getLOperand().accept(this);
		builder.append(" ").append(expression.getOperationSign().getText()).append(" ");
		PsiExpression right = expression.getROperand();
		if(right != null)
			right.accept(this);
	}

	@Override
	public void visitLocalVariable(PsiLocalVariable variable)
	{
		builder.append(MemberConverter.convertVariableDecl(variable, converterVisitor));
	}

	@Override
	public void visitCodeBlock(PsiCodeBlock block)
	{
		builder.append("\n");
		tabs(0);
		builder.append("{\n");

		for(PsiStatement statement : block.getStatements())
		{
			tabs(1);
			statement.accept(this);
			builder.append("\n");
		}

		tabs(0);
		builder.append("}\n");
		tabs(0);

		super.visitCodeBlock(block);
	}

	@Override
	public void visitTryStatement(PsiTryStatement statement)
	{
		builder.append("try");
		PsiCodeBlock tryBlock = statement.getTryBlock();
		if(tryBlock != null)
			tryBlock.accept(this);
	}

	@Override
	public void visitMethodCallExpression(PsiMethodCallExpression expression)
	{
		expression.getMethodExpression().accept(this);

		PsiType[] typeParameters = expression.getTypeArguments();
		if(typeParameters.length > 0)
		{
			builder.append("<");
			for(int i = 0; i < typeParameters.length; i++)
			{
				if(i != 0)
					builder.append(", ");
				builder.append(TypeConverter.convertType(typeParameters[i], true));
			}
			builder.append(">");
		}

		PsiExpressionList argumentList = expression.getArgumentList();
		builder.append("(");
		PsiExpression[] exps = argumentList.getExpressions();
		for(int i = 0; i < exps.length; i++)
		{
			if(i != 0)
				builder.append(", ");
			exps[i].accept(this);
		}
		builder.append(")");
	}

	@Override
	public void visitReferenceExpression(PsiReferenceExpression expression)
	{
		builder.append(expression.getText());
	}

	@Override
	public void visitIfStatement(PsiIfStatement statement)
	{
		builder.append("if");
		builder.append("(");
		PsiExpression cond = statement.getCondition();
		if(cond != null)
			cond.accept(this);
		builder.append(")");

		PsiStatement thenBranch = statement.getThenBranch();
		if(thenBranch != null)
			thenBranch.accept(this);

		PsiStatement elseBranch = statement.getElseBranch();
		if(elseBranch != null)
		{
			builder.append("else");
			elseBranch.accept(this);
		}
	}

	@Override
	public void visitThrowStatement(PsiThrowStatement statement)
	{
		builder.append("throw ");
		PsiExpression exp = statement.getException();
		if(exp != null)
			exp.accept(this);
	}
}
