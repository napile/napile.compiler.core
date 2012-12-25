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

package org.napile.compiler.lang.resolve.constants;

import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @date 21:50/25.12.12
 */
public enum IntConstantFactory
{
	BYTE
	{
		@Override
		public CompileTimeConstant<? extends Number> createValue(@NotNull Number o)
		{
			return new ByteValue(o.byteValue());
		}

		@Override
		public Number parse(@NotNull String text, int radix) throws NumberFormatException
		{
			return Byte.parseByte(text, radix);
		}
	},
	SHORT
	{
		@Override
		public CompileTimeConstant<? extends Number> createValue(@NotNull Number o)
		{
			return new ShortValue(o.shortValue());
		}

		@Override
		public Number parse(@NotNull String text, int radix) throws NumberFormatException
		{
			return Short.parseShort(text, radix);
		}
	},
	INT
	{
		@Override
		public CompileTimeConstant<? extends Number> createValue(@NotNull Number o)
		{
			return new IntValue(o.intValue());
		}

		@Override
		public Number parse(@NotNull String text, int radix) throws NumberFormatException
		{
			return (int) Long.parseLong(text, radix);
		}
	},
	LONG
	{
		@Override
		public CompileTimeConstant<? extends Number> createValue(@NotNull Number o)
		{
			return new LongValue(o.longValue());
		}

		@Override
		public Number parse(@NotNull String text, int radix) throws NumberFormatException
		{
			return Long.parseLong(text, radix);
		}
	}
	;

	public abstract CompileTimeConstant<? extends Number> createValue(@NotNull Number o);

	public abstract Number parse(@NotNull String text, int radix) throws NumberFormatException;

}
