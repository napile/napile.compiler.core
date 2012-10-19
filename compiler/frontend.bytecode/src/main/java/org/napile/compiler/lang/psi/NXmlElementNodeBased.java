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

package org.napile.compiler.lang.psi;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.tree.members.AbstractMemberNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author VISTALL
 * @date 15:37/19.10.12
 */
public abstract class NXmlElementNodeBased<N extends AbstractMemberNode<?>, E extends NapileDeclaration> extends NXmlElementBase implements NapileDeclarationContainer<NapileDeclaration>
{
	private final PsiElement parent;
	protected final N asmNode;

	public NXmlElementNodeBased(@NotNull PsiElement parent, @NotNull N node)
	{
		super(parent.getManager());

		this.parent = parent;
		this.asmNode = node;
	}

	protected abstract void collectNodes(List<PsiElement> result, N node);

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return PsiElement.EMPTY_ARRAY; //TODO [VISTALL]
	}

	@Override
	public PsiElement getParent()
	{
		return parent;
	}

	@NotNull
	@Override
	public List<NapileDeclaration> getDeclarations()
	{
		return PsiTreeUtil.getChildrenOfTypeAsList(this, NapileDeclaration.class);
	}
}
