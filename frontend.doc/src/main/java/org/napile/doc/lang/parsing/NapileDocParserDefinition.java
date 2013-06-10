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

package org.napile.doc.lang.parsing;

import java.io.Reader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.doc.lang.lexer.NapileDocNode;
import org.napile.doc.lang.lexer._NapileDocLexer;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageVersion;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * @author VISTALL
 * @since 23:09/30.01.13
 */
public class NapileDocParserDefinition implements ParserDefinition
{
	@NotNull
	@Override
	public Lexer createLexer(@Nullable Project project, @NotNull LanguageVersion languageVersion)
	{
		return new FlexAdapter(new _NapileDocLexer((Reader)null));
	}

	@NotNull
	@Override
	public PsiParser createParser(Project project, @NotNull LanguageVersion languageVersion)
	{
		return new NapileDocParser();
	}

	@NotNull
	@Override
	public IFileElementType getFileNodeType()
	{
		throw new UnsupportedOperationException();
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
		if(node.getElementType() instanceof NapileDocNode)
			return ((NapileDocNode) node.getElementType()).createPsi(node);
		return new ASTWrapperPsiElement(node);
	}

	@Override
	public PsiFile createFile(FileViewProvider viewProvider)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right)
	{
		return SpaceRequirements.MAY;
	}
}
