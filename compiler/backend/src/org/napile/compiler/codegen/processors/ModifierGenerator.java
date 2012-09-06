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

package org.napile.compiler.codegen.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.Modifier;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.MemberDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.descriptors.PropertyKind;

/**
 * @author VISTALL
 * @date 12:35/04.09.12
 */
public class ModifierGenerator
{
	@NotNull
	public static Modifier[] toModifiers(@NotNull ParameterDescriptor parameterDescriptor)
	{
		return parameterDescriptor.getPropertyKind() == PropertyKind.VAR ? Modifier.EMPTY : new Modifier[]{Modifier.FINAL};
	}

	@NotNull
	public static Modifier[] toModifiers(@NotNull ClassDescriptor memberDescriptor)
	{
		List<Modifier> list = new ArrayList<Modifier>(Arrays.asList(toModifiers((MemberDescriptor) memberDescriptor)));
		switch(memberDescriptor.getKind())
		{
			case RETELL:
				list.add(Modifier.RETELL);
				break;
			case ENUM_CLASS:
				list.add(Modifier.ENUM);
				break;
			default:
				break;
		}

		return list.isEmpty() ? Modifier.EMPTY : list.toArray(new Modifier[list.size()]);
	}

	@NotNull
	public static Modifier[] toModifiers(@NotNull MemberDescriptor memberDescriptor)
	{
		List<Modifier> list = new ArrayList<Modifier>(3);
		switch(memberDescriptor.getVisibility())
		{
			case COVERED:
				list.add(Modifier.COVERED);
				break;
			case HERITABLE:
				list.add(Modifier.HERITABLE);
				break;
			case LOCAL:
				list.add(Modifier.LOCAL);
				break;
			default:
				break;
		}

		switch(memberDescriptor.getModality())
		{
			case FINAL:
				list.add(Modifier.FINAL);
				break;
			case ABSTRACT:
				list.add(Modifier.ABSTRACT);
				break;
			default:
				break;
		}

		if(memberDescriptor.isStatic())
			list.add(Modifier.STATIC);

		return list.isEmpty() ? Modifier.EMPTY : list.toArray(new Modifier[list.size()]);
	}
}
