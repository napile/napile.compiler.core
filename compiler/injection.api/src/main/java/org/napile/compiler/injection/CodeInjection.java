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

package org.napile.compiler.injection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.JetType;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * @author VISTALL
 * @date 21:50/27.09.12
 */
public abstract class CodeInjection implements ParserDefinition
{
	/**
	 * Returning name of injection
	 * Case-sensitive
	 *   xml
	 *   {
	 *
	 *   }
	 *
	 * `xml` - is name
	 * @return
	 */
	@NotNull
	public abstract String getName();

	public abstract void parse(@NotNull PsiBuilder builder);

	@NotNull
	public abstract JetType getReturnType(@Nullable JetType expectType, @NotNull BindingTrace bindingTrace, @NotNull JetScope jetScope);

	@Override
	public final PsiParser createParser(Project project)
	{
		throw new UnsupportedOperationException();
	}

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

	@Override
	public PsiFile createFile(FileViewProvider fileViewProvider)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1)
	{
		return SpaceRequirements.MAY;
	}
}
