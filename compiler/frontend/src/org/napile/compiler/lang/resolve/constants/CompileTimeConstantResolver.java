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

package org.napile.compiler.lang.resolve.constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.checker.JetTypeChecker;
import com.intellij.openapi.util.text.StringUtil;

/**
 * @author abreslav
 */
public class CompileTimeConstantResolver
{
	public static final ErrorValue OUT_OF_RANGE = new ErrorValue("The value is out of range");

	public CompileTimeConstantResolver()
	{

	}

	@NotNull
	public CompileTimeConstant<?> getIntegerValue(@NotNull String text, @NotNull JetType expectedType)
	{
		IntConstantFactory factory = null;

		if(!noExpectedType(expectedType))
		{
			TypeConstructor constructor = expectedType.getConstructor();

			if(TypeUtils.isEqualFqName(constructor, NapileLangPackage.INT))
				factory = IntConstantFactory.INT;
			else if(TypeUtils.isEqualFqName(constructor, NapileLangPackage.LONG))
				factory = IntConstantFactory.LONG;
			else if(TypeUtils.isEqualFqName(constructor, NapileLangPackage.SHORT))
				factory = IntConstantFactory.SHORT;
			else if(TypeUtils.isEqualFqName(constructor, NapileLangPackage.BYTE))
				factory = IntConstantFactory.BYTE;
		}

		if(factory == null)
		{
			Number value = parseLongValue(text, IntConstantFactory.INT);
			if(value != null)
				return IntConstantFactory.INT.createValue(value);

			value = parseLongValue(text, IntConstantFactory.LONG);
			if(value == null)
				return OUT_OF_RANGE;
			else
				return IntConstantFactory.LONG.createValue(value);
		}

		Number value = parseLongValue(text, factory);

		if(value != null)
			return factory.createValue(value);

		return new ErrorValue("An integer literal does not conform to the expected type " + expectedType);
	}

	@Nullable
	private static Number parseLongValue(String text, IntConstantFactory factory)
	{
		try
		{
			Number value;
			if(text.startsWith("0x") || text.startsWith("0X"))
			{
				String hexString = text.substring(2);
				value = factory.parse(hexString, 16);
			}
			else if(text.startsWith("0b") || text.startsWith("0B"))
			{
				String binString = text.substring(2);
				value = factory.parse(binString, 2);
			}
			else
			{
				value = factory.parse(text, 10);
			}
			return value;
		}
		catch(NumberFormatException e)
		{
			return null;
		}
	}

	@NotNull
	public CompileTimeConstant<?> getFloatValue(@NotNull String text, @NotNull JetType expectedType)
	{
		if(noExpectedType(expectedType) || JetTypeChecker.INSTANCE.isSubtypeOf(TypeUtils.getTypeOfClassOrErrorType(expectedType.getMemberScope(), NapileLangPackage.DOUBLE, false), expectedType))
		{
			try
			{
				return new DoubleValue(Double.parseDouble(text));
			}
			catch(NumberFormatException e)
			{
				return OUT_OF_RANGE;
			}
		}
		else if(JetTypeChecker.INSTANCE.isSubtypeOf(TypeUtils.getTypeOfClassOrErrorType(expectedType.getMemberScope(), NapileLangPackage.FLOAT, false), expectedType))
		{
			try
			{
				return new DoubleValue(Float.parseFloat(text));
			}
			catch(NumberFormatException e)
			{
				return OUT_OF_RANGE;
			}
		}
		else
		{
			return new ErrorValue("A floating-point literal does not conform to the expected type " + expectedType);
		}
	}

	@Nullable
	private CompileTimeConstant<?> checkNativeType(String text, JetType expectedType, String title, JetType nativeType)
	{
		if(!noExpectedType(expectedType) && !JetTypeChecker.INSTANCE.isSubtypeOf(nativeType, expectedType))
		{
			return new ErrorValue("A " + title + " literal " + text + " does not conform to the expected type " + expectedType);
		}
		return null;
	}

	@NotNull
	public CompileTimeConstant<?> getBooleanValue(@NotNull String text)
	{
		if("true".equals(text))
		{
			return BoolValue.TRUE;
		}
		else if("false".equals(text))
		{
			return BoolValue.FALSE;
		}
		throw new IllegalStateException("Must not happen. A boolean literal has text: " + text);
	}

	@NotNull
	public CompileTimeConstant<?> getCharValue(@NotNull String text, @NotNull JetType expectedType)
	{
		CompileTimeConstant<?> error = checkNativeType(text, expectedType, "character", TypeUtils.getTypeOfClassOrErrorType(expectedType.getMemberScope(), NapileLangPackage.CHAR, false));
		if(error != null)
		{
			return error;
		}

		// Strip the quotes
		if(text.length() < 2 || text.charAt(0) != '\'' || text.charAt(text.length() - 1) != '\'')
		{
			return new ErrorValue("Incorrect character literal");
		}
		text = text.substring(1, text.length() - 1); // now there're no quotes

		if(text.length() == 0)
		{
			return new ErrorValue("Empty character literal");
		}

		if(text.charAt(0) != '\\')
		{
			// No escape
			if(text.length() == 1)
			{
				return new CharValue(text.charAt(0));
			}
			return new ErrorValue("Too many characters in a character literal '" + text + "'");
		}
		return escapedStringToCharValue(text);
	}

	@NotNull
	public static CompileTimeConstant<?> escapedStringToCharValue(@NotNull String text)
	{
		assert text.length() > 0 && text.charAt(0) == '\\' : "Only escaped sequences must be passed to this routine: " + text;

		// Escape
		String escape = text.substring(1); // strip the slash
		switch(escape.length())
		{
			case 0:
				// bare slash
				return illegalEscape(text);
			case 1:
				// one-char escape
				Character escaped = translateEscape(escape.charAt(0));
				if(escaped == null)
				{
					return illegalEscape(text);
				}
				return new CharValue(escaped);
			case 5:
				// unicode escape
				if(escape.charAt(0) == 'u')
				{
					try
					{
						Integer intValue = Integer.valueOf(escape.substring(1), 16);
						return new CharValue((char) intValue.intValue());
					}
					catch(NumberFormatException e)
					{
						// Will be reported below
					}
				}
				break;
		}
		return illegalEscape(text);
	}

	private static ErrorValue illegalEscape(String text)
	{
		return new ErrorValue("Illegal escape: " + text);
	}

	@Nullable
	public static Character translateEscape(char c)
	{
		switch(c)
		{
			case 't':
				return '\t';
			case 'b':
				return '\b';
			case 'n':
				return '\n';
			case 'r':
				return '\r';
			case '\'':
				return '\'';
			case '\"':
				return '\"';
			case '\\':
				return '\\';
			case '$':
				return '$';
		}
		return null;
	}

	@NotNull
	public CompileTimeConstant<?> getStringValue(@NotNull String unescapedText, @NotNull JetType expectedType)
	{
		/*CompileTimeConstant<?> error = checkNativeType("\"...\"", expectedType, "string", TypeUtils.getTypeOfClassOrErrorType(expectedType.getMemberScope(), NapileLangPackage.STRING, false));
		if(error != null)
		{
			return error;
		}   */
		if(unescapedText.length() <= 1)
			return new ErrorValue("Invalid string");

		return new StringValue(StringUtil.unquoteString(unescapedText));
	}

	@NotNull
	public CompileTimeConstant<?> getNullValue(@NotNull JetType expectedType)
	{
		if(noExpectedType(expectedType) || expectedType.isNullable())
		{
			return NullValue.NULL;
		}
		return new ErrorValue("Null can not be a value of a non-null type " + expectedType);
	}

	private boolean noExpectedType(JetType expectedType)
	{
		return expectedType == TypeUtils.NO_EXPECTED_TYPE ||
				TypeUtils.isEqualFqName(expectedType, NapileLangPackage.NULL) ||
				ErrorUtils.isErrorType(expectedType);
	}
}
