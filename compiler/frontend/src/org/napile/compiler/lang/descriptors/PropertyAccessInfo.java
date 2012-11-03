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

package org.napile.compiler.lang.descriptors;

/**
 * @author VISTALL
 * @date 10:08/03.11.12
 */
public class PropertyAccessInfo
{
	private MethodDescriptor set;
	private MethodDescriptor get;
	private MethodDescriptor lazy;

	public MethodDescriptor getSet()
	{
		return set;
	}

	public void setSet(MethodDescriptor set)
	{
		this.set = set;
	}

	public MethodDescriptor getGet()
	{
		return get;
	}

	public void setGet(MethodDescriptor get)
	{
		this.get = get;
	}

	public MethodDescriptor getLazy()
	{
		return lazy;
	}

	public void setLazy(MethodDescriptor lazy)
	{
		this.lazy = lazy;
	}
}
