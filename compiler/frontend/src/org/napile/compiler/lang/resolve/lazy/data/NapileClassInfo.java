/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.napile.compiler.lang.resolve.lazy.data;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ClassKind;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileClassObject;
import org.napile.compiler.lang.psi.NapileEnumEntry;
import org.napile.compiler.lang.psi.NapileTypeParameter;
import org.napile.compiler.lexer.JetTokens;

/**
 * @author abreslav
 */
public class NapileClassInfo extends NapileClassOrObjectInfo<NapileClass>
{

	protected NapileClassInfo(@NotNull NapileClass classOrObject)
	{
		super(classOrObject);
	}

	@Override
	public NapileClassObject getClassObject()
	{
		return element.getClassObject();
	}

	@NotNull
	@Override
	public List<NapileTypeParameter> getTypeParameters()
	{
		return element.getTypeParameters();
	}

	@NotNull
	@Override
	public ClassKind getClassKind()
	{
		if(element instanceof NapileEnumEntry)
			return ClassKind.ENUM_ENTRY;
		if(element.hasModifier(JetTokens.ENUM_KEYWORD))
			return ClassKind.ENUM_CLASS;
		return ClassKind.CLASS;
	}
}
