/*
 * Copyright 2010-2012 napile.org
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

package org.napile.compiler.codegen.processors.injection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @date 18:44/12.11.12
 */
public class InjectionCodegenManager implements Iterable<InjectionCodegen<?>>
{
	public static final InjectionCodegenManager INSTANCE = new InjectionCodegenManager();

	private final List<InjectionCodegen<?>> list = new ArrayList<InjectionCodegen<?>>();

	private InjectionCodegenManager()
	{
		//TODO [VISTALL] rework it
		for(String className : new String[]
		{
			"org.napile.compiler.injection.text.TextInjectionCodegen"
		})
			try
			{
				Class<?> clazz = Class.forName(className);

				InjectionCodegen injection = (InjectionCodegen) clazz.newInstance();

				list.add(injection);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
	}

	@Override
	public Iterator<InjectionCodegen<?>> iterator()
	{
		return list.iterator();
	}
}
