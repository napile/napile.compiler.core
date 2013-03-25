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
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.types.JetType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;

/**
 * @author VISTALL
 * @since 14:56/01.02.13
 */
public class ClassInfo extends NamedDocableInfo<NapileClass>
{
	private final String packageName;

	private final List<VariableInfo> variables = new ArrayList<VariableInfo>();
	private final List<MethodInfo> methods = new ArrayList<MethodInfo>();
	private final List<ConstructorInfo> constructors = new ArrayList<ConstructorInfo>();

	public ClassInfo(BindingTrace bindingContext, NapileClass element, String p)
	{
		super(bindingContext, element);
		this.packageName = p;

		for(NapileDeclaration declaration : element.getDeclarations())
		{
			if(declaration instanceof NapileConstructor)
				constructors.add(new ConstructorInfo(bindingContext, (NapileConstructor) declaration));
			if(declaration instanceof NapileNamedMethodOrMacro)
				methods.add(new MethodInfo(bindingContext, (NapileNamedMethodOrMacro) declaration));
			else if(declaration instanceof NapileVariable)
				variables.add(new VariableInfo(bindingContext, (NapileVariable) declaration));
		}
	}

	public String getSupers()
	{
		ClassDescriptor classDescriptor = bindingTrace.get(BindingTraceKeys.CLASS, element);

		return StringUtil.join(classDescriptor.getSupertypes(), new Function<JetType, String>()
		{
			@Override
			public String fun(JetType type)
			{
				return DocRender.DOC_RENDER.renderTypeWithShortNames(type);
			}
		}, "<br>");
	}

	@NotNull
	@Override
	public String getDeclaration()
	{
		ClassDescriptor classDescriptor = bindingTrace.get(BindingTraceKeys.CLASS, element);
		return classDescriptor == null ? null : DocRender.DOC_RENDER.render(classDescriptor);
	}

	public String getPackageName()
	{
		return packageName;
	}

	public List<MethodInfo> getMethods()
	{
		return methods;
	}

	public List<VariableInfo> getVariables()
	{
		return variables;
	}

	public List<ConstructorInfo> getConstructors()
	{
		return constructors;
	}
}
