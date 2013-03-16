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
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.resolve.BindingContext;

/**
 * @author VISTALL
 * @since 18:19/01.02.13
 */
public class VariableInfo extends NamedDocableInfo<NapileVariable>
{
	public VariableInfo(BindingContext bindingContext, NapileVariable element)
	{
		super(bindingContext, element);
	}

	@Override
	@NotNull
	public String getDeclaration()
	{
		VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, element);
		return variableDescriptor == null ? "null" : DocRender.DOC_RENDER.render(variableDescriptor);
	}

	public String getKeyword()
	{
		return element.isMutable() ? "val" : "var";
	}

	public String getReturnType()
	{
		VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, element);
		return variableDescriptor == null ? "null" : DocRender.DOC_RENDER.renderTypeWithShortNames(variableDescriptor.getType());
	}
}
