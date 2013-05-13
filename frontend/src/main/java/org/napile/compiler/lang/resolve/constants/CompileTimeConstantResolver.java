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
import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.constants.stringLexer.StringEscapesTokenTypes;
import org.napile.compiler.lang.resolve.constants.stringLexer.StringLiteralLexer;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.checker.NapileTypeChecker;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

/**
 * @author abreslav
 */
public class CompileTimeConstantResolver
{
	public static final ErrorValue OUT_OF_RANGE = new ErrorValue("The value is out of range");

	@NotNull
	private final BindingTrace bindingTrace;

	public CompileTimeConstantResolver(@NotNull BindingTrace bindingTrace)
	{

		this.bindingTrace = bindingTrace;
	}

	@NotNull
	public CompileTimeConstant<?> getIntegerValue(@NotNull String text, @NotNull NapileType expectedType)
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
	public CompileTimeConstant<?> getFloatValue(@NotNull String text, @NotNull NapileType expectedType)
	{
		if(noExpectedType(expectedType) || NapileTypeChecker.INSTANCE.isSubtypeOf(TypeUtils.getTypeOfClassOrErrorType(expectedType.getMemberScope(), NapileLangPackage.DOUBLE, false), expectedType))
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
		else if(NapileTypeChecker.INSTANCE.isSubtypeOf(TypeUtils.getTypeOfClassOrErrorType(expectedType.getMemberScope(), NapileLangPackage.FLOAT, false), expectedType))
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
	public CompileTimeConstant<?> getStringValue(PsiElement psiElement, @NotNull String text, @NotNull NapileType expectedType)
	{
		markEscapes(text, psiElement);

		FqName typeFqName = NapileLangPackage.STRING;
		if(TypeUtils.isEqualFqName(expectedType, NapileLangPackage.CHAR))
		{
			typeFqName = NapileLangPackage.CHAR;
			if(text.length() == 0 || text.length() > 1)
			{
				typeFqName = NapileLangPackage.STRING;
			}
		}

		if(typeFqName == NapileLangPackage.CHAR)
		{
			return new CharValue(text.charAt(0));
		}
		else
		{
			return new StringValue(StringUtil.unescapeStringCharacters(StringUtil.unquoteString(text)));
		}
	}

	private void markEscapes(String text, PsiElement psiElement)
	{
		StringLiteralLexer literalLexer = new StringLiteralLexer('\'', NapileTokens.STRING_LITERAL);
		literalLexer.start(text);

		while(true)
		{
			final IElementType tokenType = literalLexer.getTokenType();
			if(tokenType == null)
			{
				break;
			}

			try
			{
				if(StringEscapesTokenTypes.STRING_LITERAL_ESCAPES.contains(tokenType))
				{
					final TextRange textRange = new TextRange(psiElement.getTextOffset() + literalLexer.getTokenStart(), psiElement.getTextOffset() + literalLexer.getTokenEnd());

					if(tokenType == StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN)
					{
						bindingTrace.report(Errors.VALID_STRING_ESCAPE.on(psiElement, textRange));
					}
					else
					{
						bindingTrace.report(Errors.INVALID_STRING_ESCAPE.on(psiElement, textRange, literalLexer.getTokenText()));
					}
				}
			}
			finally
			{
				literalLexer.advance();
			}
		}
	}

	@NotNull
	public CompileTimeConstant<?> getNullValue(@NotNull NapileType expectedType)
	{
		if(noExpectedType(expectedType) || expectedType.isNullable())
		{
			return NullValue.NULL;
		}
		return new ErrorValue("Null can not be a value of a non-null type " + expectedType);
	}

	private boolean noExpectedType(NapileType expectedType)
	{
		return expectedType == TypeUtils.NO_EXPECTED_TYPE ||
				TypeUtils.isEqualFqName(expectedType, NapileLangPackage.NULL) ||
				ErrorUtils.isErrorType(expectedType);
	}
}
