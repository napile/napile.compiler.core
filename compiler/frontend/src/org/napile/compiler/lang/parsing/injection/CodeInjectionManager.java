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

package org.napile.compiler.lang.parsing.injection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.injection.CodeInjection;

/**
 * @author VISTALL
 * @date 10:38/28.09.12
 */
public class CodeInjectionManager
{
	public static final CodeInjectionManager INSTANCE = new CodeInjectionManager();

	private Map<String, CodeInjection> codeInjections = new HashMap<String, CodeInjection>();

	private CodeInjectionManager()
	{
		try
		{
			Class<?> clazz = Class.forName("org.napile.compiler.injection.protobuf.ProtobufCodeInjection");

			CodeInjection injection = (CodeInjection) clazz.newInstance();

			codeInjections.put(injection.getName(), injection);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public CodeInjection getInjection(@NotNull String name)
	{
		return codeInjections.get(name);
	}

	public Collection<CodeInjection> getCodeInjections()
	{
		return codeInjections.values();
	}
}
