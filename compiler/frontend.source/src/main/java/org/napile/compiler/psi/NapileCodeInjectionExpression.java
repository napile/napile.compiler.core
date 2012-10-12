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
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.injection.CodeInjection;
import org.napile.compiler.injection.lexer.NapileInjectionKeywordToken;
import org.napile.compiler.lang.psi.NapileExpressionImpl;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @date 11:30/12.10.12
 */
public class NapileCodeInjectionExpression extends NapileExpressionImpl
{
	public NapileCodeInjectionExpression(@NotNull ASTNode node)
	{
		super(node);
	}

	@Nullable
	public CodeInjection getCodeInjection()
	{
		PsiElement element = getFirstChild().getNextSibling();
		if(element == null)
			return null;

		if(element.getNode().getElementType() instanceof NapileInjectionKeywordToken)
			return ((NapileInjectionKeywordToken) element.getNode().getElementType()).codeInjection;

		return null;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitCodeInjection(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitCodeInjection(this, data);
	}
}
