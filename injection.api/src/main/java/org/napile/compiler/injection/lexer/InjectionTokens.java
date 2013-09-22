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

package org.napile.compiler.injection.lexer;

import com.intellij.util.LanguageVersionUtil;
import org.napile.compiler.lang.NapileLanguage;
import org.napile.compiler.lang.parsing.NapileParsing;
import org.napile.compiler.lang.parsing.SemanticWhitespaceAwarePsiBuilderImpl;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IReparseableElementType;

/**
 * @author VISTALL
 * @since 13:05/07.04.13
 */
public interface InjectionTokens
{
	IElementType INNER_EXPRESSION_START = new IElementType("INNER_EXPRESSION_START", Language.ANY);
	IElementType INNER_EXPRESSION_STOP = new IElementType("INNER_EXPRESSION_STOP", Language.ANY);

	IElementType INNER_EXPRESSION = new IReparseableElementType("INNER_EXPRESSION")
	{
		@Override
		public ASTNode parseContents(final ASTNode chameleon)
		{
			final Project project = chameleon.getPsi().getProject();

			final PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, NapileLanguage.INSTANCE, LanguageVersionUtil.findDefaultVersion(NapileLanguage.INSTANCE), chameleon.getChars());
			NapileParsing jetParsing = NapileParsing.createForTopLevel(new SemanticWhitespaceAwarePsiBuilderImpl(builder));

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
