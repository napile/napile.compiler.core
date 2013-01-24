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

package org.napile.idea.plugin;

import javax.swing.Icon;

import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;

/**
 * @author VISTALL
 * @date 20:35/24.01.13
 */
public class NapileIconProvider2
{
	public static Icon getIcon(DeclarationDescriptor declarationDescriptor)
	{
		if(declarationDescriptor instanceof ClassDescriptor)
			return NapileIcons.CLASS;
		if(declarationDescriptor instanceof VariableDescriptor)
			return NapileIcons.VARIABLE;
		if(declarationDescriptor instanceof MethodDescriptor)
			return NapileIcons.METHOD;
		if(declarationDescriptor instanceof TypeParameterDescriptor)
			return NapileIcons.TYPE_PARAMETER;
		return null;
	}
}
