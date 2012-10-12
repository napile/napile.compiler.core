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

/*
 * @author max
 */
package org.napile.compiler;

import java.lang.reflect.Constructor;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.napile.compiler.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileElementImpl;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;

public class NapileNodeType extends IElementType
{
	private Constructor<? extends NapileElement> myPsiFactory;

	public NapileNodeType(@NotNull @NonNls String debugName)
	{
		this(debugName, null);
	}

	public NapileNodeType(@NotNull @NonNls String debugName, Class<? extends NapileElement> psiClass)
	{
		super(debugName, NapileLanguage.INSTANCE);
		try
		{
			myPsiFactory = psiClass != null ? psiClass.getConstructor(ASTNode.class) : null;
		}
		catch(NoSuchMethodException e)
		{
			throw new RuntimeException("Must have a constructor with ASTNode");
		}
	}

	public NapileElement createPsi(ASTNode node)
	{
		assert node.getElementType() == this;

		try
		{
			if(myPsiFactory == null)
			{
				return new NapileElementImpl(node);
			}
			return myPsiFactory.newInstance(node);
		}
		catch(Exception e)
		{
			throw new RuntimeException("Error creating psi element for node", e);
		}
	}
}
