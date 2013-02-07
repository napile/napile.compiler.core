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

package org.napile.idea.plugin.editor.completion.lookup;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.AsmConstants;
import org.napile.asm.resolve.name.FqNameUnsafe;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.idea.plugin.NapileIconProvider2;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;

/**
 * @author VISTALL
 * @date 12:21/07.02.13
 */
public class DescriptionLookupBuilder
{
	public static void addElement(DeclarationDescriptor declarationDescriptor, CompletionResultSet resultSet)
	{
		Name name = declarationDescriptor.getName();
		if(name.getIdentifier().contains(AsmConstants.ANONYM_SPLITTER))
			return;

		LookupElementBuilder b = null;
		if(declarationDescriptor instanceof MethodDescriptor)
			b = buildMethodLookup(name, (MethodDescriptor) declarationDescriptor);
		else if(declarationDescriptor instanceof VariableDescriptor)
			b = buildVariableLookup(name, (VariableDescriptor) declarationDescriptor);
		else if(declarationDescriptor instanceof ClassDescriptor)
			b = buildClassLookup(name, (ClassDescriptor) declarationDescriptor);
		else if(declarationDescriptor instanceof TypeParameterDescriptor)
			b = buildTypeParameterLookup(name, (TypeParameterDescriptor) declarationDescriptor);
		else
			throw new UnsupportedOperationException("Unsupported descriptor " + declarationDescriptor);

		resultSet.addElement(b);
	}

	@NotNull
	private static LookupElementBuilder buildMethodLookup(Name name, MethodDescriptor methodDescriptor)
	{
		LookupElementBuilder elementBuilder = LookupElementBuilder.create(name.getIdentifier());

		elementBuilder = elementBuilder.appendTailText(CompletionRender.INSTANCE.renderFunctionParameters(methodDescriptor), true);
		elementBuilder = elementBuilder.withIcon(NapileIconProvider2.getIcon(methodDescriptor));
		elementBuilder = elementBuilder.withTypeText(CompletionRender.INSTANCE.renderType(methodDescriptor.getReturnType()));
		elementBuilder = elementBuilder.withInsertHandler(new MethodParenthesesInsertHandler(methodDescriptor));
		return elementBuilder;
	}

	@NotNull
	private static LookupElementBuilder buildVariableLookup(Name name, VariableDescriptor variableDescriptor)
	{
		LookupElementBuilder elementBuilder = LookupElementBuilder.create(name.getIdentifier());

		elementBuilder = elementBuilder.withIcon(NapileIconProvider2.getIcon(variableDescriptor));
		elementBuilder = elementBuilder.withTypeText(CompletionRender.INSTANCE.renderType(variableDescriptor.getReturnType()));
		return elementBuilder;
	}

	@NotNull
	private static LookupElementBuilder buildClassLookup(Name name, ClassDescriptor classDescriptor)
	{
		LookupElementBuilder elementBuilder = LookupElementBuilder.create(name.getIdentifier());

		FqNameUnsafe fqName = DescriptorUtils.getFQName(classDescriptor);

		elementBuilder = elementBuilder.withIcon(NapileIconProvider2.getIcon(classDescriptor));
		elementBuilder = elementBuilder.withTailText("(" + fqName.parent().getFqName() + ")", true);
		return elementBuilder;
	}

	@NotNull
	private static LookupElementBuilder buildTypeParameterLookup(Name name, TypeParameterDescriptor typeParameterDescriptor)
	{
		LookupElementBuilder elementBuilder = LookupElementBuilder.create(name.getIdentifier());

		elementBuilder = elementBuilder.withIcon(NapileIconProvider2.getIcon(typeParameterDescriptor));
		elementBuilder = elementBuilder.withTailText(" : " + StringUtil.join(typeParameterDescriptor.getUpperBounds(), new Function<JetType, String>()
		{
			@Override
			public String fun(JetType jetType)
			{
				return CompletionRender.INSTANCE.renderType(jetType);
			}
		}, " & "), true);

		return elementBuilder;
	}
}
