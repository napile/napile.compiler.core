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

/*
 * @author max
 */
package org.napile.compiler.lang.parsing;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.NapileNodeType;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.stubs.elements.JetStubElementType;
import org.napile.compiler.lang.psi.stubs.elements.JetStubElementTypes;
import org.napile.compiler.lexer.JetLexer;
import org.napile.compiler.lexer.JetTokens;
import org.napile.compiler.plugin.JetLanguage;
import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;

public class JetParserDefinition implements ParserDefinition
{
	public static final String KTSCRIPT_FILE_SUFFIX = "ktscript";

	public JetParserDefinition()
	{
		//todo: ApplicationManager.getApplication() is null during JetParsingTest setting up

        /*if (!ApplicationManager.getApplication().isCommandLine()) {
        }*/
	}

	@NotNull
	public static JetParserDefinition getInstance()
	{
		return (JetParserDefinition) LanguageParserDefinitions.INSTANCE.forLanguage(JetLanguage.INSTANCE);
	}

	@Override
	@NotNull
	public Lexer createLexer(Project project)
	{
		return new JetLexer();
	}

	@Override
	public PsiParser createParser(Project project)
	{
		return new PsiParser()
		{
			@NotNull
			@Override
			public ASTNode parse(IElementType root, PsiBuilder builder)
			{
				JetParsing jetParsing = JetParsing.createForTopLevel(new SemanticWhitespaceAwarePsiBuilderImpl(builder));
				jetParsing.parseFile();
				return builder.getTreeBuilt();
			}
		};
	}

	@Override
	public IFileElementType getFileNodeType()
	{
		return JetStubElementTypes.FILE;
	}

	@Override
	@NotNull
	public TokenSet getWhitespaceTokens()
	{
		return JetTokens.WHITESPACES;
	}

	@Override
	@NotNull
	public TokenSet getCommentTokens()
	{
		return JetTokens.COMMENTS;
	}

	@Override
	@NotNull
	public TokenSet getStringLiteralElements()
	{
		return JetTokens.STRINGS;
	}

	@Override
	@NotNull
	public PsiElement createElement(ASTNode astNode)
	{
		if(astNode.getElementType() instanceof JetStubElementType)
		{
			return ((JetStubElementType) astNode.getElementType()).createPsiFromAst(astNode);
		}

		return ((NapileNodeType) astNode.getElementType()).createPsi(astNode);
	}

	@Override
	public PsiFile createFile(FileViewProvider fileViewProvider)
	{
		return new NapileFile(fileViewProvider);
	}

	@Override
	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1)
	{
		return SpaceRequirements.MAY;
	}
}
