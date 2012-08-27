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

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileClassOrObject;
import org.napile.compiler.lang.psi.NapileObjectDeclaration;

/**
 * @author abreslav
 */
public class JetClassInfoUtil
{

	public static NapileClassLikeInfo createClassLikeInfo(@NotNull NapileClassOrObject classOrObject)
	{
		if(classOrObject instanceof NapileClass)
		{
			NapileClass napileClass = (NapileClass) classOrObject;
			return new NapileClassInfo(napileClass);
		}
		if(classOrObject instanceof NapileObjectDeclaration)
		{
			NapileObjectDeclaration objectDeclaration = (NapileObjectDeclaration) classOrObject;
			return new NapileObjectInfo(objectDeclaration);
		}
		throw new IllegalArgumentException("Unknown declaration type: " + classOrObject + classOrObject.getText());
	}
}