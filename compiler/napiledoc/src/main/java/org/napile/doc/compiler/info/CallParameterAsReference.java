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
import org.napile.compiler.lang.psi.NapileCallParameterAsReference;
import org.napile.compiler.lang.resolve.BindingTrace;

/**
 * @author VISTALL
 * @since 18:47/01.02.13
 */
public class CallParameterAsReference extends DocableInfo<NapileCallParameterAsReference> implements CallParameter
{
	public CallParameterAsReference(BindingTrace bindingContext, NapileCallParameterAsReference element)
	{
		super(bindingContext, element);
	}

	@NotNull
	@Override
	public String getName()
	{
		return element.getText();
	}

	@Override
	public String getReturnType()
	{
		return "ref to variable";
	}

	@Override
	public String getReturnTypeNoHtml()
	{
		return getReturnType();
	}

	@NotNull
	@Override
	public String getDoc()
	{
		return "";
	}

	@Override
	public String getKeyword()
	{
		return "val";
	}
}
