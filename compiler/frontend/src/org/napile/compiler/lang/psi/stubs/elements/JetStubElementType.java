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

package org.napile.compiler.lang.psi.stubs.elements;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.napile.compiler.NapileLanguage;
import org.napile.compiler.lang.psi.NapileBlockExpression;
import org.napile.compiler.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileFunctionLiteral;
import org.napile.compiler.lang.psi.NapileWithExpressionInitializer;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author Nikolay Krasko
 */
public abstract class JetStubElementType<StubT extends StubElement, PsiT extends PsiElement> extends IStubElementType<StubT, PsiT>
{

	public JetStubElementType(@NotNull @NonNls String debugName)
	{
		super(debugName, NapileLanguage.INSTANCE);
	}

	public abstract PsiT createPsiFromAst(@NotNull ASTNode node);

	@Override
	public String getExternalId()
	{
		return "idea." + toString();
	}

	@Override
	public boolean shouldCreateStub(ASTNode node)
	{
		PsiElement psi = node.getPsi();

		// Do not create stubs inside function literals
		if(PsiTreeUtil.getParentOfType(psi, NapileFunctionLiteral.class) != null)
		{
			return false;
		}

		// Don't create stubs if declaration is inside function or property accessor with block
		NapileBlockExpression blockExpression = PsiTreeUtil.getParentOfType(psi, NapileBlockExpression.class);
		if(blockExpression != null)
		{
			return false;
		}

		// Don't create stubs if declaration is inside other declaration with expression initializer
		@SuppressWarnings("unchecked") NapileWithExpressionInitializer withInitializer = PsiTreeUtil.getParentOfType(psi, NapileWithExpressionInitializer.class, true, NapileBlockExpression.class);
		if(withInitializer != null)
		{
			NapileExpression initializer = withInitializer.getInitializer();
			if(PsiTreeUtil.isAncestor(initializer, psi, true))
			{
				return false;
			}
		}

		return super.shouldCreateStub(node);
	}
}
