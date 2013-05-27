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

/*
 * @author max
 */
package org.napile.compiler.lang.parsing;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.NapileLanguage;
import org.napile.compiler.lang.lexer.NapileNode;
import org.napile.compiler.lang.lexer.NapileLexer;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import org.napile.compiler.lang.psi.impl.NapileFileImpl;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementType;
import org.napile.compiler.lang.lexer.NapileTokens;
import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;

public class NapileParserDefinition implements ParserDefinition
{
	public NapileParserDefinition()
	{}

	@NotNull
	public static NapileParserDefinition getInstance()
	{
		return (NapileParserDefinition) LanguageParserDefinitions.INSTANCE.forLanguage(NapileLanguage.INSTANCE);
	}

	@Override
	@NotNull
	public Lexer createLexer(Project project, Module module)
	{
		return new NapileLexer();
	}

	@Override
	public PsiParser createParser(Project project)
	{
		return new NapileParser();
	}

	@Override
	public IFileElementType getFileNodeType()
	{
		return NapileStubElementTypes.FILE;
	}

	@Override
	@NotNull
	public TokenSet getWhitespaceTokens()
	{
		return NapileTokens.WHITESPACES;
	}

	@Override
	@NotNull
	public TokenSet getCommentTokens()
	{
		return NapileTokens.COMMENTS;
	}

	@Override
	@NotNull
	public TokenSet getStringLiteralElements()
	{
		return NapileTokens.STRINGS;
	}

	@Override
	@NotNull
	public PsiElement createElement(ASTNode astNode)
	{
		if(astNode.getElementType() instanceof NapileStubElementType)
		{
			return ((NapileStubElementType) astNode.getElementType()).createPsiFromAst(astNode);
		}

		return ((NapileNode) astNode.getElementType()).createPsi(astNode);
	}

	@Override
	public PsiFile createFile(FileViewProvider fileViewProvider)
	{
		return new NapileFileImpl(fileViewProvider);
	}

	@Override
	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1)
	{
		return SpaceRequirements.MAY;
	}
}
