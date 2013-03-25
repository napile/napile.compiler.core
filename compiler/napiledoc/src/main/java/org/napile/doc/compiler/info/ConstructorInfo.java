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

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.psi.NapileCallParameter;
import org.napile.compiler.lang.psi.NapileCallParameterAsReference;
import org.napile.compiler.lang.psi.NapileCallParameterAsVariable;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BindingTraceKeys;

/**
 * @author VISTALL
 * @since 18:55/01.02.13
 */
public class ConstructorInfo extends NamedDocableInfo<NapileConstructor>
{
	private final List<CallParameter> parameters;

	public ConstructorInfo(BindingTrace bindingContext, NapileConstructor element)
	{
		super(bindingContext, element);
		NapileCallParameter[] ps = element.getCallParameters();
		parameters = new ArrayList<CallParameter>(ps.length);

		for(NapileCallParameter callParameter : ps)
		{
			if(callParameter instanceof NapileCallParameterAsReference)
			{
				parameters.add(new CallParameterAsReference(bindingContext, (NapileCallParameterAsReference) callParameter));
			}
			else if(callParameter instanceof NapileCallParameterAsVariable)
			{
				parameters.add(new CallParameterAsVariable(bindingContext, (NapileCallParameterAsVariable) callParameter));
			}
		}
	}

	@NotNull
	@Override
	public String getDeclaration()
	{
		ConstructorDescriptor constructorDescriptor = bindingTrace.get(BindingTraceKeys.CONSTRUCTOR, element);
		return constructorDescriptor == null ? "null" : DocRender.DOC_RENDER.render(constructorDescriptor);
	}

	public List<CallParameter> getParameters()
	{
		return parameters;
	}
}
