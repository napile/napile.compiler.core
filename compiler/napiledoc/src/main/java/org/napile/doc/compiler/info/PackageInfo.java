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

import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.BindingContext;

/**
 * @author VISTALL
 * @since 14:54/01.02.13
 */
public class PackageInfo extends DocableInfo<NapileFile>
{
	private static final String STATIC_DOC = "no information (<a href=\"https://github.com/napile-lang/napile.compiler/issues/61\">issue</a>)";

	private final Map<String, ClassInfo> classes = new TreeMap<String, ClassInfo>();

	public PackageInfo(BindingContext bindingContext, NapileFile element)
	{
		super(bindingContext, element);
	}

	@NotNull
	@Override
	public String getName()
	{
		return element.getPackageName();
	}

	@NotNull
	@Override
	public String getDoc()
	{
		return STATIC_DOC;
	}

	public Map<String, ClassInfo> getClasses()
	{
		return classes;
	}
}
