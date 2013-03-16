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

package org.napile.compiler.injection.text.lang.lexer;

import org.napile.compiler.injection.text.lang.TextLanguage;
import org.napile.compiler.lang.NapileLanguage;
import org.napile.compiler.lang.parsing.JetParsing;
import org.napile.compiler.lang.parsing.SemanticWhitespaceAwarePsiBuilderImpl;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IReparseableElementType;

/**
 * @author VISTALL
 * @since 21:18/09.11.12
 */
public interface TextTokens
{
	IElementType TEXT_PART = new IElementType("TEXT_PART", TextLanguage.INSTANCE);

	IElementType HASH = new IElementType("HASH", TextLanguage.INSTANCE);
	IElementType LBRACE = new IElementType("LBRACE", TextLanguage.INSTANCE);
	IElementType RBRACE = new IElementType("RBRACE", TextLanguage.INSTANCE);

	IElementType NAPILE_EXPRESSION = new IReparseableElementType("NAPILE_EXPRESSION")
	{
		@Override
		public ASTNode parseContents(final ASTNode chameleon)
		{
			final Project project = chameleon.getPsi().getProject();

			final PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, NapileLanguage.INSTANCE, chameleon.getChars());
			JetParsing jetParsing = JetParsing.createForTopLevel(new SemanticWhitespaceAwarePsiBuilderImpl(builder));

			jetParsing.getExpressionParser().parseExpression();

			return builder.getTreeBuilt();
		}

		@Override
		public ASTNode createNode(final CharSequence text)
		{
			return new LazyParseablePsiElement(this, text);
		}
	};
}
