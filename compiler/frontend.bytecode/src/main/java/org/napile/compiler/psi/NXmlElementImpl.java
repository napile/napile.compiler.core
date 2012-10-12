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
import org.napile.compiler.lang.psi.NapileTreeVisitor;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiManager;

/**
 * @author VISTALL
 * @date 22:03/10.10.12
 */
public abstract class NXmlElementImpl extends NXmlElementBase implements NapileElement
{
	public NXmlElementImpl(@NotNull PsiManager psiManager)
	{
		super(psiManager);
	}

	@Override
	public final void accept(@NotNull PsiElementVisitor visitor)
	{
		if(visitor instanceof NapileVisitorVoid)
			accept((NapileVisitorVoid) visitor);
		else
			visitor.visitElement(this);
	}

	@Override
	public final <D> void acceptChildren(@NotNull NapileTreeVisitor<D> visitor, D data)
	{
		PsiElement child = getFirstChild();
		while(child != null)
		{
			if(child instanceof NapileElement)
				((NapileElement) child).accept(visitor, data);
			child = child.getNextSibling();
		}
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitJetElement(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitJetElement(this, data);
	}

	@Override
	public final NapileFile getContainingFile() throws PsiInvalidElementAccessException
	{
		return (NapileFile) super.getContainingFile();
	}
}