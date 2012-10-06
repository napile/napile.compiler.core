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

package org.napile.compiler.codegen.processors.codegen.stackValue;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;

/**
 * @author VISTALL
 * @date 14:04/27.09.12
 */
@Deprecated
public class Bool extends StackValue
{
	public static final Bool TRUE = new Bool("TRUE");
	public static final Bool FALSE = new Bool("FALSE");

	private final Property propertyStackValue;

	public Bool(@NotNull String type)
	{
		super(TypeConstants.BOOL);

		propertyStackValue = new Property(NapileLangPackage.BOOL.child(Name.identifier(type)), getType(), true);
	}

	@Override
	public void put(TypeNode type, InstructionAdapter instructionAdapter)
	{
		propertyStackValue.put(type, instructionAdapter);
	}
}
