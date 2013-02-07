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

import org.jetbrains.annotations.Nullable;
import org.napile.asm.AsmConstants;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.idea.plugin.NapileIconProvider2;
import com.intellij.codeInsight.lookup.LookupElementBuilder;

/**
 * @author VISTALL
 * @date 12:21/07.02.13
 */
public class DescriptionLookupBuilder
{
	@Nullable
	public static LookupElementBuilder buildMethodLookup(MethodDescriptor methodDescriptor)
	{
		String identifier = methodDescriptor.getName().getIdentifier();
		if(identifier.contains(AsmConstants.ANONYM_SPLITTER))
			return null;

		LookupElementBuilder elementBuilder = LookupElementBuilder.create(identifier);

		elementBuilder = elementBuilder.appendTailText(CompletionRender.INSTANCE.renderFunctionParameters(methodDescriptor), true);
		elementBuilder = elementBuilder.withIcon(NapileIconProvider2.getIcon(methodDescriptor));
		elementBuilder = elementBuilder.withTypeText(CompletionRender.INSTANCE.renderType(methodDescriptor.getReturnType()));
		elementBuilder = elementBuilder.withInsertHandler(new MethodParenthesesInsertHandler(methodDescriptor));
		return elementBuilder;
	}

	@Nullable
	public static LookupElementBuilder buildVariableLookup(VariableDescriptor variableDescriptor)
	{
		String identifier = variableDescriptor.getName().getIdentifier();
		if(identifier.contains(AsmConstants.ANONYM_SPLITTER))
			return null;

		LookupElementBuilder elementBuilder = LookupElementBuilder.create(identifier);

		elementBuilder = elementBuilder.withIcon(NapileIconProvider2.getIcon(variableDescriptor));
		elementBuilder = elementBuilder.withTypeText(CompletionRender.INSTANCE.renderType(variableDescriptor.getReturnType()));
		return elementBuilder;
	}
}
