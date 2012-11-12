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

package org.napile.compiler.injection.text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.injection.CodeInjection;
import org.napile.compiler.injection.text.lang.TextLanguage;
import org.napile.compiler.injection.text.lang.lexer.TextLexer;
import org.napile.compiler.injection.text.lang.lexer.TextParser;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeUtils;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;

/**
 * @author VISTALL
 * @date 20:19/09.11.12
 */
public class TextCodeInjection extends CodeInjection
{
	@NotNull
	@Override
	public String getName()
	{
		return "text";
	}

	@NotNull
	@Override
	public Language getLanguage()
	{
		return TextLanguage.INSTANCE;
	}

	@NotNull
	@Override
	public JetType getReturnType(@Nullable JetType expectType, @NotNull BindingTrace bindingTrace, @NotNull JetScope jetScope)
	{
		return TypeUtils.getTypeOfClassOrErrorType(jetScope, NapileLangPackage.STRING);
	}

	@NotNull
	@Override
	public Lexer createLexer(Project project)
	{
		return new TextLexer();
	}

	@Override
	public PsiParser createParser(Project project)
	{
		return new TextParser();
	}

	@NotNull
	@Override
	public TokenSet getWhitespaceTokens()
	{
		return TokenSet.EMPTY;
	}

	@NotNull
	@Override
	public TokenSet getCommentTokens()
	{
		return TokenSet.EMPTY;
	}

	@NotNull
	@Override
	public TokenSet getStringLiteralElements()
	{
		return TokenSet.EMPTY;
	}

	@NotNull
	@Override
	public PsiElement createElement(ASTNode node)
	{
		return new ASTWrapperPsiElement(node);
	}
}
