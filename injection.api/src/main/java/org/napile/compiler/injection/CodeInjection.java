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

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.injection.lexer.InjectionLexer;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.types.NapileType;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.ParserDefinition;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;

/**
 * @author VISTALL
 * @since 21:50/27.09.12
 */
public abstract class CodeInjection implements ParserDefinition, UserDataHolder
{
	private final Map<Key, Object> userData = new HashMap<Key, Object>();

	/**
	 * Returning name of injection
	 * Case-sensitive
	 *   /xml/
	 *   {
	 *
	 *   }
	 *
	 * `xml` - is name
	 * @return
	 */
	@NotNull
	public abstract String getName();

	@NotNull
	public abstract Language getLanguage();

	@NotNull
	protected abstract Lexer getBaseLexer();

	@Nullable
	protected IElementType getSharpElementType()
	{
		return null;
	}

	@Nullable
	protected IElementType getLbraceElementTypeInfo()
	{
		return null;
	}

	@Nullable
	protected IElementType getRbraceElementTypeInfo()
	{
		return null;
	}

	@NotNull
	public abstract NapileType getReturnType(@Nullable NapileType expectType, @NotNull BindingTrace bindingTrace, @NotNull NapileScope napileScope);

	@NotNull
	@Override
	public Lexer createLexer(Project project, Module module)
	{
		final IElementType sharpElementTypeInfo = getSharpElementType();
		if(sharpElementTypeInfo == null)
		{
			return getBaseLexer();
		}
		return new InjectionLexer(getBaseLexer(), sharpElementTypeInfo, getLbraceElementTypeInfo(), getRbraceElementTypeInfo());
	}

	@Override
	public final IFileElementType getFileNodeType()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public final PsiFile createFile(FileViewProvider fileViewProvider)
	{
		throw new IllegalArgumentException();
	}

	@Override
	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1)
	{
		return SpaceRequirements.MAY;
	}

	@Nullable
	@Override
	public <T> T getUserData(@NotNull Key<T> key)
	{
		return key.get(userData);
	}

	@Override
	public <T> void putUserData(@NotNull Key<T> key, @Nullable T value)
	{
		key.set(userData, value);
	}
}
