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

package org.napile.idea.plugin.refactoring;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.lexer.NapileLexer;
import org.napile.compiler.lang.psi.NapileCallExpression;
import org.napile.compiler.lang.psi.NapileQualifiedExpressionImpl;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.checker.JetTypeChecker;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.idea.plugin.module.Analyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;

/**
 * User: Alefas
 * Date: 31.01.12
 */
public class JetNameSuggester
{
	private JetNameSuggester()
	{
	}

	private static void addName(ArrayList<String> result, String name, JetNameValidator validator)
	{
		if(name == "class")
			name = "clazz";
		if(!isIdentifier(name))
			return;
		String newName = validator.validateName(name);
		if(newName == null)
			return;
		result.add(newName);
	}

	/**
	 * Name suggestion types:
	 * 1. According to type:
	 * 1a. Primitive types to some short name
	 * 1b. Class types according to class name camel humps: (AbCd => {abCd, cd})
	 * 1c. Arrays => arrayOfInnerType
	 * 2. Reference expressions according to reference name camel humps
	 * 3. Method call expression according to method callee expression
	 *
	 * @param expression to suggest name for variable
	 * @param validator  to check scope for such names
	 * @return possible names
	 */
	public static String[] suggestNames(NapileExpression expression, JetNameValidator validator)
	{
		ArrayList<String> result = new ArrayList<String>();

		BindingContext bindingContext = Analyzer.analyze((NapileFile) expression.getContainingFile()).getBindingContext();
		JetType jetType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression);
		if(jetType != null)
		{
			addNamesForType(result, jetType, validator);
		}
		addNamesForExpression(result, expression, validator);

		if(result.isEmpty())
			addName(result, "value", validator);
		return ArrayUtil.toStringArray(result);
	}

	private static void addNamesForType(ArrayList<String> result, JetType jetType, JetNameValidator validator)
	{
		JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;
		if(ErrorUtils.containsErrorType(jetType))
			return;
		if(typeChecker.equalTypes(TypeUtils.getTypeOfClassOrErrorType(jetType.getMemberScope(), NapileLangPackage.BOOL, false), jetType))
		{
			addName(result, "b", validator);
		}
		else if(typeChecker.equalTypes(TypeUtils.getTypeOfClassOrErrorType(jetType.getMemberScope(), NapileLangPackage.INT, false), jetType))
		{
			addName(result, "i", validator);
		}
		else if(typeChecker.equalTypes(TypeUtils.getTypeOfClassOrErrorType(jetType.getMemberScope(), NapileLangPackage.BYTE, false), jetType))
		{
			addName(result, "byte", validator);
		}
		else if(typeChecker.equalTypes(TypeUtils.getTypeOfClassOrErrorType(jetType.getMemberScope(), NapileLangPackage.LONG, false), jetType))
		{
			addName(result, "l", validator);
		}
		else if(typeChecker.equalTypes(TypeUtils.getTypeOfClassOrErrorType(jetType.getMemberScope(), NapileLangPackage.FLOAT, false), jetType))
		{
			addName(result, "fl", validator);
		}
		else if(typeChecker.equalTypes(TypeUtils.getTypeOfClassOrErrorType(jetType.getMemberScope(), NapileLangPackage.DOUBLE, false), jetType))
		{
			addName(result, "d", validator);
		}
		else if(typeChecker.equalTypes(TypeUtils.getTypeOfClassOrErrorType(jetType.getMemberScope(), NapileLangPackage.SHORT, false), jetType))
		{
			addName(result, "sh", validator);
		}
		else if(typeChecker.equalTypes(TypeUtils.getTypeOfClassOrErrorType(jetType.getMemberScope(), NapileLangPackage.CHAR, false), jetType))
		{
			addName(result, "c", validator);
		}
		else if(typeChecker.equalTypes(TypeUtils.getTypeOfClassOrErrorType(jetType.getMemberScope(), NapileLangPackage.STRING, false), jetType))
		{
			addName(result, "s", validator);
		}
		else
		{
			addForClassType(result, jetType, validator);
		}
	}

	private static void addForClassType(ArrayList<String> result, JetType jetType, JetNameValidator validator)
	{
		ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(jetType);
		if(classDescriptor != null)
		{
			Name className = classDescriptor.getName();
			addCamelNames(result, className.getName(), validator);
		}
	}

	private static void addCamelNames(ArrayList<String> result, String name, JetNameValidator validator)
	{
		if(name == "")
			return;
		String s = deleteNonLetterFromString(name);
		if(s.startsWith("get") || s.startsWith("set"))
		{
			s = s.substring(3);
		}
		else if(s.startsWith("is"))
			s = s.substring(2);
		for(int i = 0; i < s.length(); ++i)
		{
			if(i == 0)
			{
				addName(result, StringUtil.decapitalize(s), validator);
			}
			else if(s.charAt(i) >= 'A' && s.charAt(i) <= 'Z')
			{
				addName(result, StringUtil.decapitalize(s.substring(i)), validator);
			}
		}
	}

	private static String deleteNonLetterFromString(String s)
	{
		Pattern pattern = Pattern.compile("[^a-zA-Z]");
		Matcher matcher = pattern.matcher(s);
		return matcher.replaceAll("");
	}

	private static void addNamesForExpression(ArrayList<String> result, NapileExpression expression, JetNameValidator validator)
	{
		if(expression instanceof NapileQualifiedExpressionImpl)
		{
			NapileQualifiedExpressionImpl qualifiedExpression = (NapileQualifiedExpressionImpl) expression;
			NapileExpression selectorExpression = qualifiedExpression.getSelectorExpression();
			addNamesForExpression(result, selectorExpression, validator);
			if(selectorExpression != null && selectorExpression instanceof NapileCallExpression)
			{
				NapileExpression calleeExpression = ((NapileCallExpression) selectorExpression).getCalleeExpression();
				if(calleeExpression != null && calleeExpression instanceof NapileSimpleNameExpression)
				{
					String name = ((NapileSimpleNameExpression) calleeExpression).getReferencedName();
					if(name != null && name.equals("sure"))
					{
						addNamesForExpression(result, qualifiedExpression.getReceiverExpression(), validator);
					}
				}
			}
		}
		else if(expression instanceof NapileSimpleNameExpression)
		{
			NapileSimpleNameExpression reference = (NapileSimpleNameExpression) expression;
			String referenceName = reference.getReferencedName();
			if(referenceName == null)
				return;
			if(referenceName.equals(referenceName.toUpperCase()))
			{
				addName(result, referenceName, validator);
			}
			else
			{
				addCamelNames(result, referenceName, validator);
			}
		}
		else if(expression instanceof NapileCallExpression)
		{
			NapileCallExpression call = (NapileCallExpression) expression;
			addNamesForExpression(result, call.getCalleeExpression(), validator);
		}
	}

	public static boolean isIdentifier(String name)
	{
		ApplicationManager.getApplication().assertReadAccessAllowed();
		if(name == null || name.isEmpty())
			return false;

		NapileLexer lexer = new NapileLexer();
		lexer.start(name, 0, name.length());
		if(lexer.getTokenType() != NapileTokens.IDENTIFIER)
			return false;
		lexer.advance();
		return lexer.getTokenType() == null;
	}
}
