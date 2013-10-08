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

package org.napile.doc.lang.lexer;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import com.intellij.util.LanguageVersionUtil;
import org.napile.compiler.lang.NapileLanguage;
import org.napile.compiler.lang.parsing.NapileParsing;
import org.napile.compiler.lang.parsing.SemanticWhitespaceAwarePsiBuilderImpl;
import org.napile.doc.lang.psi.impl.NapileDocLineImpl;

/**
 * @author VISTALL
 * @since 9:12/31.01.13
 */
public interface NapileDocNodes
{
	IElementType DOC_LINE = new NapileDocNode("DOC_LINE", NapileDocLineImpl.class);

	IElementType CODE_BLOCK = new ILazyParseableElementType("CODE_BLOCK")
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
