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

package org.napile.idea.plugin.references;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapilePsiFactory;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;

/**
 * @author yole
 */
public class JetSimpleNameReference extends JetPsiReference
{

	@NotNull
	private final NapileSimpleNameExpression myExpression;

	public JetSimpleNameReference(@NotNull NapileSimpleNameExpression jetSimpleNameExpression)
	{
		super(jetSimpleNameExpression);
		myExpression = jetSimpleNameExpression;
	}

	@NotNull
	@Override
	public PsiElement getElement()
	{
		return myExpression.getReferencedNameElement();
	}

	@NotNull
	public NapileSimpleNameExpression getExpression()
	{
		return myExpression;
	}

	@NotNull
	@Override
	public TextRange getRangeInElement()
	{
		return new TextRange(0, getElement().getTextLength());
	}

	@NotNull
	@Override
	public Object[] getVariants()
	{
		return ArrayUtil.EMPTY_OBJECT_ARRAY;
	}

	@Override
	public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException
	{
		PsiElement element = NapilePsiFactory.createNameIdentifier(myExpression.getProject(), newElementName);
		return myExpression.getReferencedNameElement().replace(element);
	}
}
