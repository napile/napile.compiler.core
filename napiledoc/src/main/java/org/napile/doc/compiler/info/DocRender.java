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

import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.PackageDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.render.DescriptorRenderer;
import org.napile.doc.compiler.Arguments;

/**
 * @author VISTALL
 * @since 10:27/02.02.13
 */
public class DocRender extends DescriptorRenderer.HtmlDescriptorRenderer
{
	public static DocRender DOC_RENDER = new DocRender();

	private static final String TEMPLATE = "<a href=\"${URL}/api/${packageName}/${className}.htm\">${className}</a>";

	@Override
	protected boolean shouldRenderDefinedIn()
	{
		return false;
	}

	@Override
	protected String renderDefaultType(NapileType type, boolean shortNamesOnly)
	{
		StringBuilder sb = new StringBuilder();
		ClassifierDescriptor cd = type.getConstructor().getDeclarationDescriptor();

		Object typeNameObject;

		if(cd == null || cd instanceof TypeParameterDescriptor)
		{
			typeNameObject = type.getConstructor();
		}
		else
		{
			//if(shortNamesOnly)
			{
				// for nested classes qualified name should be used
				typeNameObject = cd.getName();
				DeclarationDescriptor parent = cd.getContainingDeclaration();
				while(parent instanceof ClassDescriptor)
				{
					typeNameObject = parent.getName() + "." + typeNameObject;
					parent = parent.getContainingDeclaration();
				}

				if(parent instanceof PackageDescriptor)
				{
					FqName fqName = DescriptorUtils.getFQName(parent).toSafe();

					String str = TEMPLATE.replace("${URL}", Arguments.URL);
					str = str.replace("${packageName}", fqName.getFqName().replace(".", "/"));
					str = str.replace("${className}", typeNameObject.toString());
					typeNameObject = str;
				}
			}
			//else
			//	typeNameObject = DescriptorUtils.getFQName(cd);
		}

		sb.append(typeNameObject);
		if(!type.getArguments().isEmpty())
		{
			sb.append(escape("<"));
			appendTypes(sb, type.getArguments(), shortNamesOnly);
			sb.append(escape(">"));
		}
		if(type.isNullable())
		{
			sb.append("?");
		}
		return sb.toString();
	}
}
