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

package org.napile.compiler.lang.psi;

import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @date 16:19/16.02.13
 */
public abstract class NXmlParentedElementBase extends NXmlElementBase
{
	private final PsiElement parent;

	public NXmlParentedElementBase(PsiElement parent)
	{
		this.parent = parent;
	}

	@Override
	public PsiElement getParent()
	{
		return parent;
	}
}
