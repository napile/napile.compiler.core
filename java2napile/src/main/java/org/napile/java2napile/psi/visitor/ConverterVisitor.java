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
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;

/**
 * @author VISTALL
 * @since 14:07/04.01.13
 */
public class ConverterVisitor extends JavaElementVisitor
{
	private final StringBuilder builder = new StringBuilder();

	public static final char SPACE = ' ';
	public static final char LINE = '\n';
	public static final char LBRACE = '{';
	public static final char RBRACE = '}';
	public static final char TAB = '\t';

	private int indent = 0;

	@Override
	public void visitClass(PsiClass aClass)
	{
		StringUtil.repeatSymbol(builder, TAB, indent);

		PsiDocComment comment = aClass.getDocComment();
		if(comment != null)
		{
			comment.accept(this);
			builder.append(LINE);

			StringUtil.repeatSymbol(builder, TAB, indent);
		}

		ModifierConverter.modifiers(aClass, builder);
		builder.append("class").append(SPACE).append(aClass.getName());

		PsiClassType[] superTypes = aClass.getExtendsListTypes();
		PsiClassType[] implTypes = aClass.getImplementsListTypes();
		if(superTypes.length != 0 || implTypes.length != 0)
		{
			builder.append(SPACE).append(":").append(SPACE);

			builder.append(StringUtil.join(superTypes, new Function<PsiClassType, String>()
			{
				@Override
				public String fun(PsiClassType classType)
				{
					return TypeConverter.convertType(classType, false);
				}
			}, " & "));

			builder.append(StringUtil.join(implTypes, new Function<PsiClassType, String>()
			{
				@Override
				public String fun(PsiClassType classType)
				{
					return TypeConverter.convertType(classType, false);
				}
			}, " & "));
		}

		builder.append(LINE);

		StringUtil.repeatSymbol(builder, TAB, indent);
		builder.append(LBRACE).append(LINE);

		indent ++;

		for(PsiMember member : PsiTreeUtil.getChildrenOfTypeAsList(aClass, PsiMember.class))
			member.accept(this);

		indent --;

		StringUtil.repeatSymbol(builder, TAB, indent);
		builder.append(RBRACE);
	}

	@Override
	public void visitMethod(PsiMethod method)
	{
		builder.append(LINE);
		StringUtil.repeatSymbol(builder, TAB, indent);

		PsiDocComment comment = method.getDocComment();
		if(comment != null)
		{
			comment.accept(this);

			builder.append(LINE);

			StringUtil.repeatSymbol(builder, TAB, indent);
		}


		if(method.isConstructor())
			builder.append(MemberConverter.convertConstructorDecl(method));
		else
			builder.append(MemberConverter.convertMethodDecl(method, this));

		builder.append(LINE);
	}

	public void tabs(StringBuilder b, int plus)
	{
		StringUtil.repeatSymbol(b, TAB, indent + plus);
	}

	@Override
	public void visitField(PsiField field)
	{
		builder.append(LINE);
		StringUtil.repeatSymbol(builder, TAB, indent);

		PsiDocComment comment = field.getDocComment();
		if(comment != null)
		{
			comment.accept(this);
			builder.append(LINE);

			StringUtil.repeatSymbol(builder, TAB, indent);
		}

		builder.append(MemberConverter.convertVariableDecl(field));
		builder.append(LINE);
	}

	@Override
	public void visitElement(PsiElement element)
	{
		element.acceptChildren(this);
	}

	@Override
	public void visitWhiteSpace(PsiWhiteSpace space)
	{
		builder.append(space.getText());
	}

	@Override
	public void visitDocComment(PsiDocComment comment)
	{
		builder.append(comment.getText());
	}

	@Override
	public void visitImportStatement(PsiImportStatement statement)
	{
		builder.append("import").append(SPACE).append(statement.getQualifiedName());
	}

	@Override
	public void visitPackageStatement(PsiPackageStatement statement)
	{
		builder.append("package").append(SPACE).append(statement.getPackageName());
	}

	public StringBuilder getBuilder()
	{
		return builder;
	}
}
