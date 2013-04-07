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

package org.napile.compiler.injection.text.gen;

import org.jetbrains.annotations.Nullable;
import org.napile.compiler.injection.text.lang.lexer.TextTokens;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 19:00/12.11.12
 */
public class OnlyStringCheckVisitor extends NapileVisitorVoid
{
	private StringBuilder builder = new StringBuilder();

	@Override
	public void visitElement(PsiElement element)
	{
		if(builder == null)
			return;
		if(element.getNode().getElementType() == TextTokens.TEXT_PART)
			builder.append(element.getText());
		else
			element.acceptChildren(this);  //FIXME [VISTALL] need this?
	}

	@Override
	public void visitExpression(NapileExpression expression)
	{
		builder = null;
	}

	@Nullable
	public String getText()
	{
		return builder == null ? null : builder.toString();
	}
}
