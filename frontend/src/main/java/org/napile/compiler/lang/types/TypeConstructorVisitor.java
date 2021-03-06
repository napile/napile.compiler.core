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

package org.napile.compiler.lang.types;

/**
 * @author VISTALL
 * @since 10:56/28.12.12
 */
public abstract class TypeConstructorVisitor<A, R>
{
	public R visitType(NapileType type, TypeConstructor t, A arg)
	{
		throw new UnsupportedOperationException(t + " " + arg);
	}

	public R visitSelfType(NapileType type, SelfTypeConstructor t, A arg)
	{
		return visitType(type, t, arg);
	}

	public R visitMethodType(NapileType type, MethodTypeConstructor t, A arg)
	{
		return visitType(type, t, arg);
	}

	public R visitMultiType(NapileType type, MultiTypeConstructor t, A arg)
	{
		return visitType(type, t, arg);
	}
}
