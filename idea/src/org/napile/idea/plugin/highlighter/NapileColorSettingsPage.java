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

package org.napile.idea.plugin.highlighter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.NapileFileType;
import org.napile.compiler.lang.NapileLanguage;
import org.napile.idea.plugin.JetBundle;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.OptionsBundle;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;

public class NapileColorSettingsPage implements ColorSettingsPage
{
	@Override
	public Icon getIcon()
	{
		return NapileFileType.INSTANCE.getIcon();
	}

	@NotNull
	@Override
	public SyntaxHighlighter getHighlighter()
	{
		return new NapileHighlighter();
	}

	@NotNull
	@Override
	public String getDemoText()
	{
		return "/* Block comment */\n" +
				"<KEYWORD>package</KEYWORD> hello\n" +
				"\n" +
				"<KEYWORD>import</KEYWORD> napile.io.Console // line comment\n" +
				"\n" +
				"/**\n" +
				" * Doc comment here for `MyClass`\n" +
				" * @see Iterator#next()\n" +
				" */\n" +
				"<ANNOTATION>@Deprecated</ANNOTATION>\n" +
				"<KEYWORD>covered</KEYWORD> class <CLASS>MyClass</CLASS><<TYPE_PARAMETER>T</TYPE_PARAMETER>> : <CLASS>Iterable</CLASS><<TYPE_PARAMETER>T</TYPE_PARAMETER>>\n" +
				"{" +
				"\n" +
				"    <KEYWORD>static</KEYWORD> val <STATIC_PROPERTY>MY_VALUE</STATIC_PROPERTY> : Int = 1" +
				"\n" +
				"\n" +
				"    var <INSTANCE_PROPERTY>myVar</INSTANCE_PROPERTY> : Int" +
				"\n" +
				"    {" +
				"\n" +
				"        <KEYWORD>local</KEYWORD> <KEYWORD>set</KEYWORD>" +
				"\n" +
				"    } = 0" +
				"\n" +
				"\n" +
				"    meth <FUNCTION_DECLARATION>test</FUNCTION_DECLARATION>(<PARAMETER>nullable</PARAMETER> : <CLASS>String</CLASS>?, <PARAMETER>f</PARAMETER> : <FUNCTION_LITERAL_BRACES_AND_ARROW>{</FUNCTION_LITERAL_BRACES_AND_ARROW>(<PARAMETER>el</PARAMETER> : <CLASS>Int</CLASS>) <FUNCTION_LITERAL_BRACES_AND_ARROW>-></FUNCTION_LITERAL_BRACES_AND_ARROW> <CLASS>Int</CLASS><FUNCTION_LITERAL_BRACES_AND_ARROW>}</FUNCTION_LITERAL_BRACES_AND_ARROW>)" +
				"\n" +
				"    {\n" +
				"        <CLASS>Console</CLASS>.<METHOD_CALL><STATIC_METHOD_CALL>writeLine</STATIC_METHOD_CALL></METHOD_CALL>(\"Hello world !!!. <INVALID_STRING_ESCAPE><STRING_ESCAPE>\\e</STRING_ESCAPE></INVALID_STRING_ESCAPE>\")\n" +
				"        val <LOCAL_VARIABLE>ints</LOCAL_VARIABLE> = napile.collection.<CLASS>ArrayList</CLASS><<CLASS>Int</CLASS>?>(2)\n" +
				"        <LOCAL_VARIABLE>ints</LOCAL_VARIABLE>[0] = 102 + <PARAMETER>f</PARAMETER>()\n" +
				"        var <LOCAL_VARIABLE>testIt</LOCAL_VARIABLE> = <METHOD_CALL>test</METHOD_CALL>(<KEYWORD>null</KEYWORD>, <FUNCTION_LITERAL_BRACES_AND_ARROW>{<AUTO_GENERATED_VAR>value</AUTO_GENERATED_VAR>}</FUNCTION_LITERAL_BRACES_AND_ARROW>)" +
				"\n" +
				//"        var <LOCAL_VARIABLE><MUTABLE_VARIABLE><WRAPPED_INTO_REF>ref</WRAPPED_INTO_REF></MUTABLE_VARIABLE></LOCAL_VARIABLE> = <LOCAL_VARIABLE>ints</LOCAL_VARIABLE>.<METHOD_CALL>size</METHOD_CALL>()\n" +
				"    }" +
				"\n" +
				"\n" +
				"    meth <FUNCTION_DECLARATION>test2</FUNCTION_DECLARATION>()" +
				"\n" +
				"    {" +
				"\n" +
				"        var obj : <CLASS>String</CLASS> = /<KEYWORD>text</KEYWORD>/ {<INJECTION_BLOCK>Hello my friends.</INJECTION_BLOCK>}" +
				"\n" +
				"        <MACRO_CALL>myMacro</MACRO_CALL>()" +
				"\n" +
				"    }" +
				"\n" +
				"\n" +
				"    <KEYWORD>local</KEYWORD> macro <FUNCTION_DECLARATION>myMacro</FUNCTION_DECLARATION>()" +
				"\n" +
				"    {" +
				"\n" +
				"       return" +
				"\n" +
				"    }" +
				"\n" +
				"}" +
				"\n" +
				"\n" +
				"<KEYWORD>heritable</KEYWORD> <KEYWORD>abstract</KEYWORD> class <ABSTRACT_CLASS>Abstract</ABSTRACT_CLASS>" +
				"\n" +
				"{" +
				"\n" +
				"}" +
				"\n" +
				"\n" +
				"               Bad character: \\n\n";
	}

	@Override
	public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap()
	{
		Map<String, TextAttributesKey> map = new HashMap<String, TextAttributesKey>();
		for(Field field : NapileHighlightingColors.class.getFields())
		{
			if(Modifier.isStatic(field.getModifiers()))
			{
				try
				{
					map.put(field.getName(), (TextAttributesKey) field.get(null));
				}
				catch(IllegalAccessException e)
				{
					assert false;
				}
			}
		}
		return map;
	}

	@NotNull
	@Override
	public AttributesDescriptor[] getAttributeDescriptors()
	{
		return new AttributesDescriptor[]
		{
				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.keyword"), NapileHighlightingColors.KEYWORD),
				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.number"), NapileHighlightingColors.NUMBER),

				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.string"), NapileHighlightingColors.STRING),
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.string.escape"), NapileHighlightingColors.STRING_ESCAPE),
				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.invalid.escape.in.string"), NapileHighlightingColors.INVALID_STRING_ESCAPE),

				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.operator.sign"), NapileHighlightingColors.OPERATOR_SIGN),
				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.parentheses"), NapileHighlightingColors.PARENTHESIS),
				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.braces"), NapileHighlightingColors.BRACES),
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.closure.braces"), NapileHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW),
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.arrow"), NapileHighlightingColors.ARROW),
				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.brackets"), NapileHighlightingColors.BRACKETS),
				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.comma"), NapileHighlightingColors.COMMA),
				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.semicolon"), NapileHighlightingColors.SEMICOLON),
				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.dot"), NapileHighlightingColors.DOT),
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.safe.access"), NapileHighlightingColors.SAFE_ACCESS),

				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.line.comment"), NapileHighlightingColors.LINE_COMMENT),
				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.block.comment"), NapileHighlightingColors.BLOCK_COMMENT),

				// KDoc highlighting options are temporarily disabled, until actual highlighting and parsing of them is implemented
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.kdoc.comment"), NapileHighlightingColors.DOC_COMMENT),
				//new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.kdoc.tag"), JetHighlightingColors.DOC_COMMENT_TAG),
				//new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.kdoc.tag.value"), JetHighlightingColors.DOC_COMMENT_TAG_VALUE),
				//new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.kdoc.markup"), JetHighlightingColors.DOC_COMMENT_MARKUP),

				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.class"), NapileHighlightingColors.CLASS),
				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.type.parameter"), NapileHighlightingColors.TYPE_PARAMETER),
				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.abstract.class"), NapileHighlightingColors.ABSTRACT_CLASS),
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.annotation"), NapileHighlightingColors.ANNOTATION),

				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.var"), NapileHighlightingColors.MUTABLE_VARIABLE),

				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.local.variable"), NapileHighlightingColors.LOCAL_VARIABLE),
				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.parameter"), NapileHighlightingColors.PARAMETER),
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.captured.variable"), NapileHighlightingColors.WRAPPED_INTO_REF),

				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.instance.property"), NapileHighlightingColors.INSTANCE_PROPERTY),
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.namespace.property"), NapileHighlightingColors.STATIC_PROPERTY),
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.extension.property"), NapileHighlightingColors.EXTENSION_PROPERTY),
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.injection.block"), NapileHighlightingColors.INJECTION_BLOCK),

				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.it"), NapileHighlightingColors.AUTO_GENERATED_VAR),
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.fun"), NapileHighlightingColors.FUNCTION_DECLARATION),
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.method.call"), NapileHighlightingColors.METHOD_CALL),
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.macro.call"), NapileHighlightingColors.MACRO_CALL),
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.namespace.fun.call"), NapileHighlightingColors.STATIC_METHOD_CALL),
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.extension.fun.call"), NapileHighlightingColors.EXTENSION_FUNCTION_CALL),
				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.constructor.call"), NapileHighlightingColors.CONSTRUCTOR_CALL),

				new AttributesDescriptor(OptionsBundle.message("options.java.attribute.descriptor.bad.character"), NapileHighlightingColors.BAD_CHARACTER),

				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.auto.casted"), NapileHighlightingColors.AUTO_CASTED_VALUE),

				new AttributesDescriptor(JetBundle.message("options.idea.attribute.descriptor.label"), NapileHighlightingColors.LABEL),
		};
	}

	@NotNull
	@Override
	public ColorDescriptor[] getColorDescriptors()
	{
		return ColorDescriptor.EMPTY_ARRAY;
	}

	@NotNull
	@Override
	public String getDisplayName()
	{
		return NapileLanguage.NAME;
	}
}
