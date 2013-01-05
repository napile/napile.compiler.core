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
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiEllipsisType;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.util.Function;

/**
 * @author VISTALL
 * @date 15:31/04.01.13
 */
public class TypeConverter
{
	public static String convertType(final PsiType type, boolean nullable)
	{

		final StringBuilder builder = new StringBuilder();
		if(type instanceof PsiClassType)
		{
			PsiClassType classType = (PsiClassType) type;

			builder.append(classType.getClassName());
			PsiType[] psiTypes = classType.getParameters();
			if(psiTypes.length > 0)
			{
				builder.append("<");
				builder.append(StringUtil.join(psiTypes, new Function<PsiType, String>()
				{
					@Override
					public String fun(PsiType type)
					{
						return convertType(type, true);
					}
				}, ", "));
				builder.append(">");
			}
		}
		else if(type instanceof PsiPrimitiveType)
		{
			nullable = false;
			if(type.getPresentableText().equals("void"))
				return null;
			else if(type.getPresentableText().equals("long"))
				builder.append("Long");
			else if(type.getPresentableText().equals("int"))
				builder.append("Int");
			else if(type.getPresentableText().equals("short"))
				builder.append("Short");
			else if(type.getPresentableText().equals("byte"))
				builder.append("Byte");
			else if(type.getPresentableText().equals("boolean"))
				builder.append("Bool");
			else
				throw new UnsupportedOperationException(type.getPresentableText());
		}
		else if(type instanceof PsiEllipsisType)
		{
			builder.append("@napile.annotation.VarArgs").append(ConverterVisitor.SPACE);

			builder.append("Array<").append(convertType(((PsiArrayType) type).getComponentType(), nullable)).append(">");
		}
		else if(type instanceof PsiArrayType)
			builder.append("Array<").append(convertType(((PsiArrayType) type).getComponentType(), nullable)).append(">");
		else
			throw new UnsupportedOperationException(type.getClass().getName());

		if(nullable)
			builder.append("?");
		return builder.toString();
	}
}
