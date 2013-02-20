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

package org.napile.compiler.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.psi.NapileTreeVisitor;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;

/**
 * This class is used to wrap an expression that occurs in a reference position, such as a function literal, into a reference expression
 *
 * @author abreslav
 */
public class NapileFakeReferenceImpl extends LeafPsiElement implements NapileReferenceExpression
{
	private final NapileElement actualElement;

	public NapileFakeReferenceImpl(@NotNull NapileElement actualElement)
	{
		super(NapileNodes.REFERENCE_EXPRESSION, actualElement.getText());
		this.actualElement = actualElement;
	}

	public NapileElement getActualElement()
	{
		return actualElement;
	}

	@Override
	public TextRange getTextRange()
	{
		return actualElement.getTextRange();
	}

	@Override
	public <D> void acceptChildren(@NotNull NapileTreeVisitor<D> visitor, D data)
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
		visitor.visitReferenceExpression(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitReferenceExpression(this, data);
	}

	@Override
	public PsiElement getParent()
	{
		return actualElement.getParent();
	}

	@Override
	public NapileFile getContainingFile()
	{
		return actualElement.getContainingFile();
	}
}
