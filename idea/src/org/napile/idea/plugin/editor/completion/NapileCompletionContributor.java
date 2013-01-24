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

package org.napile.idea.plugin.editor.completion;

import static com.intellij.patterns.StandardPatterns.or;
import static org.napile.idea.plugin.editor.completion.patterns.NapilePsiElementPattern.element;

import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileBlockExpression;
import org.napile.compiler.lang.psi.NapileCallParameterList;
import org.napile.compiler.lang.psi.NapileClassBody;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @date 17:16/24.01.13
 */
public class NapileCompletionContributor extends CompletionContributor
{
	private static final ElementPattern<? extends PsiElement> MEMBERS_IN_CLASS_BODY = element().withSuperParent(2, NapileClassBody.class);
	private static final ElementPattern<? extends PsiElement> MEMBERS_IN_BODY_EXPRESSION = or(element().withSuperParent(2, NapileBlockExpression.class), element().withSuperParent(3, NapileCallParameterList.class));

	private static final ElementPattern<? extends PsiElement> MODIFIER_LIST = or(MEMBERS_IN_CLASS_BODY);

	public NapileCompletionContributor()
	{
		extend(CompletionType.BASIC, MEMBERS_IN_CLASS_BODY, new NapileKeywordCompletionProvider(NapileTokens.CLASS_KEYWORD, NapileTokens.MACRO_KEYWORD, NapileTokens.METH_KEYWORD, NapileTokens.VAR_KEYWORD, NapileTokens.VAL_KEYWORD, NapileTokens.THIS_KEYWORD));
		extend(CompletionType.BASIC, MEMBERS_IN_BODY_EXPRESSION, new NapileKeywordCompletionProvider(NapileTokens.VAR_KEYWORD, NapileTokens.VAL_KEYWORD));

		extend(CompletionType.BASIC, MODIFIER_LIST, new NapileKeywordCompletionProvider(NapileTokens.MODIFIER_KEYWORDS));
	}
}
