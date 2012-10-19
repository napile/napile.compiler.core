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

package org.napile.compiler.lang.psi.impl;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.lexer.NapileToken;
import org.napile.compiler.lang.psi.NXmlElementBase;
import org.napile.compiler.lang.psi.NapileAnnotation;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.NapileTreeVisitor;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiManager;

/**
 * @author VISTALL
 * @date 15:50/19.10.12
 */
public class NXmlModifierListImpl extends NXmlElementBase implements NapileModifierList
{
	public NXmlModifierListImpl(PsiManager psiManager)
	{
		super(psiManager);
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return new PsiElement[0];
	}

	@Override
	public PsiElement getParent()
	{
		return null;
	}

	@NotNull
	@Override
	public List<NapileAnnotation> getAnnotations()
	{
		return null;
	}

	@Override
	public boolean hasModifier(NapileToken token)
	{
		return false;
	}

	@Nullable
	@Override
	public ASTNode getModifierNode(NapileToken token)
	{
		return null;
	}

	@Override
	public <D> void acceptChildren(@NotNull NapileTreeVisitor<D> visitor, D data)
	{
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return null;
	}

	@Override
	public NapileFile getContainingFile() throws PsiInvalidElementAccessException
	{
		return null;
	}
}
