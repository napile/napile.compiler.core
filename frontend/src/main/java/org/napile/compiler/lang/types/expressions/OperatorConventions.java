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

package org.napile.compiler.lang.types.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.lexer.NapileToken;
import org.napile.compiler.lang.lexer.NapileTokens;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * @author abreslav
 */
public class OperatorConventions
{

	public static final Name EQUALS = Name.identifier("equals");
	public static final Name COMPARE_TO = Name.identifier("compareTo");
	public static final Name CONTAINS = Name.identifier("contains");

	private OperatorConventions()
	{
	}

	// Names for primitive type conversion properties
	public static final Name DOUBLE = Name.identifier("toDouble");
	public static final Name FLOAT = Name.identifier("toFloat");
	public static final Name LONG = Name.identifier("toLong");
	public static final Name INT = Name.identifier("toInt");
	public static final Name CHAR = Name.identifier("toChar");
	public static final Name SHORT = Name.identifier("toShort");
	public static final Name BYTE = Name.identifier("toByte");


	public static final ImmutableSet<Name> NUMBER_CONVERSIONS = ImmutableSet.of(DOUBLE, FLOAT, LONG, INT, SHORT, BYTE, CHAR);

	public static final ImmutableBiMap<NapileToken, Name> UNARY_OPERATION_NAMES = ImmutableBiMap.<NapileToken, Name>builder()
			.put(NapileTokens.PLUSPLUS, Name.identifier("inc"))
			.put(NapileTokens.MINUSMINUS, Name.identifier("dec"))
			.put(NapileTokens.PLUS, Name.identifier("plus"))
			.put(NapileTokens.MINUS, Name.identifier("minus"))
			// bit operators
			.put(NapileTokens.TILDE, Name.identifier("bitNot"))

			.put(NapileTokens.EXCL, Name.identifier("not")).build();

	public static final ImmutableBiMap<NapileToken, Name> BINARY_OPERATION_NAMES = ImmutableBiMap.<NapileToken, Name>builder()
			.put(NapileTokens.MUL, Name.identifier("times"))
			.put(NapileTokens.PLUS, Name.identifier("plus"))
			.put(NapileTokens.MINUS, Name.identifier("minus")
			).put(NapileTokens.DIV, Name.identifier("div"))
			.put(NapileTokens.PERC, Name.identifier("mod"))
			.put(NapileTokens.ARROW, Name.identifier("arrow"))
			// bit operators
			.put(NapileTokens.XOR, Name.identifier("bitXor"))
			.put(NapileTokens.OR, Name.identifier("bitOr"))
			.put(NapileTokens.AND, Name.identifier("bitAnd"))
			.put(NapileTokens.LTLT, Name.identifier("bitShiftLeft"))
			.put(NapileTokens.GTGT, Name.identifier("bitShiftRight"))
			.put(NapileTokens.GTGTGT, Name.identifier("bitShiftRightZ"))

			.put(NapileTokens.RANGE, Name.identifier("rangeTo")).build();

	public static final ImmutableSet<NapileToken> COMPARISON_OPERATIONS = ImmutableSet.<NapileToken>of(NapileTokens.LT, NapileTokens.GT, NapileTokens.LTEQ, NapileTokens.GTEQ);

	public static final ImmutableSet<NapileToken> EQUALS_OPERATIONS = ImmutableSet.<NapileToken>of(NapileTokens.EQEQ, NapileTokens.EXCLEQ);

	public static final ImmutableSet<NapileToken> IN_OPERATIONS = ImmutableSet.<NapileToken>of(NapileTokens.IN_KEYWORD, NapileTokens.NOT_IN);

	public static final ImmutableMap<NapileToken, NapileToken> ASSIGNMENT_OPERATION_COUNTERPARTS = ImmutableMap.<NapileToken, NapileToken>builder()
			.put(NapileTokens.MULTEQ, NapileTokens.MUL)
			.put(NapileTokens.DIVEQ, NapileTokens.DIV)
			.put(NapileTokens.PERCEQ, NapileTokens.PERC)
			.put(NapileTokens.PLUSEQ, NapileTokens.PLUS)
			.put(NapileTokens.MINUSEQ, NapileTokens.MINUS)
			.put(NapileTokens.ANDEQ, NapileTokens.AND)
			.put(NapileTokens.OREQ, NapileTokens.OR)
			.put(NapileTokens.LTLTEQ, NapileTokens.LTLT)
			.put(NapileTokens.GTGTEQ, NapileTokens.GTGT)
			.put(NapileTokens.GTGTGTEQ, NapileTokens.GTGTGT)
			.put(NapileTokens.XOREQ, NapileTokens.XOR).build();

	public static final ImmutableSet<NapileToken> BOOLEAN_OPERATIONS = ImmutableSet.of(NapileTokens.ANDAND, NapileTokens.OROR);

	@Nullable
	public static Name getNameForOperationSymbol(@NotNull NapileToken token)
	{
		Name name = UNARY_OPERATION_NAMES.get(token);
		if(name != null)
			return name;
		name = BINARY_OPERATION_NAMES.get(token);
		if(name != null)
			return name;
		if(COMPARISON_OPERATIONS.contains(token))
			return COMPARE_TO;
		if(EQUALS_OPERATIONS.contains(token))
			return EQUALS;
		if(IN_OPERATIONS.contains(token))
			return CONTAINS;
		return null;
	}
}
