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

package org.napile.idea.plugin.editor.highlight;

import java.awt.Color;
import java.awt.Font;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.NapileLanguage;
import org.napile.compiler.lang.descriptors.CallParameterAsVariableDescriptorImpl;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.LocalVariableDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;

public class NapileHighlightingColors
{
	public final static TextAttributesKey KEYWORD = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.KEYWORD);

	public static final TextAttributesKey NUMBER = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.NUMBER);

	public static final TextAttributesKey STRING = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.STRING);

	public static final TextAttributesKey VALID_STRING_ESCAPE = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);

	public static final TextAttributesKey INVALID_STRING_ESCAPE = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE);

	public static final TextAttributesKey OPERATOR_SIGN = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.OPERATION_SIGN);

	public static final TextAttributesKey PARENTHESIS = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.PARENTHESES);

	public static final TextAttributesKey BRACES = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.BRACES);

	public static final TextAttributesKey BRACKETS = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.BRACKETS);

	public static final TextAttributesKey METHOD_LITERAL_BRACES_AND_ARROW = TextAttributesKey.createTextAttributesKey("NAPILE_FUNCTION_LITERAL_BRACES_AND_ARROW", new TextAttributes(null, null, null, null, Font.BOLD));

	public static final TextAttributesKey COMMA = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.COMMA);

	public static final TextAttributesKey SEMICOLON = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.SEMICOLON);

	public static final TextAttributesKey DOT = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.DOT);

	public static final TextAttributesKey SAFE_ACCESS = createKey(NapileLanguage.INSTANCE, "SAFE_ACCESS", DefaultLanguageHighlighterColors.DOT);

	public static final TextAttributesKey ARROW = createKey(NapileLanguage.INSTANCE, "ARROW", DefaultLanguageHighlighterColors.PARENTHESES);

	public static final TextAttributesKey LINE_COMMENT = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.LINE_COMMENT);

	public static final TextAttributesKey BLOCK_COMMENT = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.BLOCK_COMMENT);

	public static final TextAttributesKey DOC_COMMENT = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.DOC_COMMENT);

	public static final TextAttributesKey CLASS = createKey(NapileLanguage.INSTANCE, "CLASS", DefaultLanguageHighlighterColors.CLASS_NAME);

	public static final TextAttributesKey TYPE_PARAMETER = createKey(NapileLanguage.INSTANCE, CodeInsightColors.TYPE_PARAMETER_NAME_ATTRIBUTES);

	public static final TextAttributesKey ABSTRACT_CLASS = createKey(NapileLanguage.INSTANCE, "ABSTRACT_CLASS", DefaultLanguageHighlighterColors.CLASS_NAME);

	public static final TextAttributesKey ANNOTATION = createKey(NapileLanguage.INSTANCE, "ANNOTATION", DefaultLanguageHighlighterColors.METADATA);

	public static final TextAttributesKey MUTABLE_VARIABLE = TextAttributesKey.createTextAttributesKey("NAPILE_MUTABLE_VARIABLE", new TextAttributes(null, null, Color.BLACK, EffectType.LINE_UNDERSCORE, 0));

	public static final TextAttributesKey LOCAL_VARIABLE = createKey(NapileLanguage.INSTANCE, CodeInsightColors.LOCAL_VARIABLE_ATTRIBUTES);

	public static final TextAttributesKey PARAMETER = createKey(NapileLanguage.INSTANCE, CodeInsightColors.PARAMETER_ATTRIBUTES);

	public static final TextAttributesKey WRAPPED_INTO_REF = createKey(NapileLanguage.INSTANCE, "WRAPPED_INTO_REF", CodeInsightColors.IMPLICIT_ANONYMOUS_CLASS_PARAMETER_ATTRIBUTES);

	public static final TextAttributesKey INSTANCE_VARIABLE = createKey(NapileLanguage.INSTANCE, "INSTANCE_VARIABLE", DefaultLanguageHighlighterColors.INSTANCE_FIELD);

	public static final TextAttributesKey STATIC_VARIABLE = createKey(NapileLanguage.INSTANCE, "STATIC_VARIABLE", DefaultLanguageHighlighterColors.STATIC_FIELD);

	public static final TextAttributesKey AUTO_GENERATED_VAR = TextAttributesKey.createTextAttributesKey("NAPILE_CLOSURE_DEFAULT_PARAMETER", new TextAttributes(null, new Color(0xdbffdb), null, null, Font.PLAIN));

	public static final TextAttributesKey METHOD_DECLARATION = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.INSTANCE_METHOD);

	public static final TextAttributesKey METHOD_CALL = createKey(NapileLanguage.INSTANCE, CodeInsightColors.METHOD_CALL_ATTRIBUTES);

	public static final TextAttributesKey STATIC_METHOD_CALL = createKey(NapileLanguage.INSTANCE, DefaultLanguageHighlighterColors.STATIC_METHOD);

	public static final TextAttributesKey EXTENSION_METHOD_CALL = TextAttributesKey.createTextAttributesKey("NAPILE_EXTENSION_FUNCTION_CALL", new TextAttributes());

	public static final TextAttributesKey CONSTRUCTOR_CALL = createKey(NapileLanguage.INSTANCE, CodeInsightColors.CONSTRUCTOR_CALL_ATTRIBUTES);

	public static final TextAttributesKey BAD_CHARACTER = createKey(NapileLanguage.INSTANCE, HighlighterColors.BAD_CHARACTER);

	public static final TextAttributesKey MACRO_CALL = TextAttributesKey.createTextAttributesKey("NAPILE_MACRO_CALL", new TextAttributes(null, new Color(0xb4c2ff), null, null, Font.PLAIN));

	public static final TextAttributesKey AUTO_CASTED_VALUE = TextAttributesKey.createTextAttributesKey("NAPILE_AUTO_CASTED_VALUE", new TextAttributes(null, new Color(0xdbffdb), null, null, Font.PLAIN));

	public static final TextAttributesKey LABEL = TextAttributesKey.createTextAttributesKey("NAPILE_LABEL", new TextAttributes(new Color(0x4a86e8), null, null, null, Font.PLAIN));

	public static final TextAttributesKey DEBUG_INFO = TextAttributesKey.createTextAttributesKey("NAPILE_DEBUG_INFO", new TextAttributes(null, null, Color.BLACK, EffectType.ROUNDED_BOX, Font.PLAIN));

	public static final TextAttributesKey RESOLVED_TO_ERROR = TextAttributesKey.createTextAttributesKey("NAPILE_RESOLVED_TO_ERROR", new TextAttributes(null, null, Color.RED, EffectType.ROUNDED_BOX, Font.PLAIN));

	private NapileHighlightingColors()
	{
	}

	@NotNull
	public static TextAttributesKey createKey(@NotNull Language language, @NotNull TextAttributesKey defaultKey)
	{
		return createKey(language, defaultKey.getExternalName(), defaultKey);
	}

	@NotNull
	public static TextAttributesKey createKey(@NotNull Language language, @NotNull String name, @NotNull TextAttributesKey defaultKey)
	{
		return TextAttributesKey.createTextAttributesKey(language.getID() + "." + name, defaultKey);
	}

	@NotNull
	public static TextAttributesKey getAttributes(DeclarationDescriptor declarationDescriptor)
	{
		if(declarationDescriptor instanceof LocalVariableDescriptor)
			return LOCAL_VARIABLE;
		if(declarationDescriptor instanceof CallParameterAsVariableDescriptorImpl)
			return PARAMETER;
		if(declarationDescriptor instanceof VariableDescriptor)
			return ((VariableDescriptor) declarationDescriptor).isStatic() ? STATIC_VARIABLE : INSTANCE_VARIABLE;
		throw new IllegalArgumentException("invalid : " + declarationDescriptor);
	}
}
