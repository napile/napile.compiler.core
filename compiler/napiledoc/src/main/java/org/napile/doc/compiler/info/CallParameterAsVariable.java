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

package org.napile.doc.compiler.info;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileCallParameterAsVariable;
import org.napile.compiler.lang.resolve.BindingContext;

/**
 * @author VISTALL
 * @date 18:44/01.02.13
 */
public class CallParameterAsVariable extends NamedDocableInfo<NapileCallParameterAsVariable> implements CallParameter
{
	public CallParameterAsVariable(BindingContext bindingContext, NapileCallParameterAsVariable element)
	{
		super(bindingContext, element);
	}

	@NotNull
	@Override
	public String getDeclaration()
	{
		VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VALUE_PARAMETER, element);
		return variableDescriptor == null ? "null" : DocRender.DOC_RENDER.render(variableDescriptor);
	}

	@Override
	public String getReturnType()
	{
		VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VALUE_PARAMETER, element);
		return variableDescriptor == null ? "null" : DocRender.DOC_RENDER.renderTypeWithShortNames(variableDescriptor.getType());
	}

	@Override
	public String getReturnTypeNoHtml()
	{
		VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VALUE_PARAMETER, element);
		return variableDescriptor == null ? "null" : variableDescriptor.getType().toString();
	}

	@Override
	public String getKeyword()
	{
		return element.isMutable() ? "val" : "var";
	}
}
