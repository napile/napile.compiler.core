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

package org.napile.compiler.psi;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.tree.members.Node;
import com.intellij.psi.PsiManager;

/**
 * @author VISTALL
 * @date 9:31/10.10.12
 */
public abstract class NXmlNodeBasedElementImpl<N extends Node> extends NXmlElementImpl
{
	protected final N node;

	public NXmlNodeBasedElementImpl(@NotNull PsiManager psiManager, @NotNull N node)
	{
		super(psiManager);
		this.node = node;
	}
}
