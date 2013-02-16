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

package org.napile.compiler.lang.psi.impl;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NXmlParentedElementBase;
import org.napile.compiler.lang.psi.NapileCallParameterList;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileMethodType;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.TreeElement;

/**
 * @author VISTALL
 * @date 18:52/16.02.13
 */
public class NXmlMethodTypeImpl extends NXmlParentedElementBase implements NapileMethodType
{
	public NXmlMethodTypeImpl(PsiElement parent)
	{
		super(parent);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
	}

	@Nullable
	@Override
	public NapileCallParameterList getParameterList()
	{
		return null;
	}

	@NotNull
	@Override
	public NapileElement[] getParameters()
	{
		return new NapileElement[0];
	}

	@Nullable
	@Override
	public NapileTypeReference getReturnTypeRef()
	{
		return null;
	}

	@NotNull
	@Override
	public ASTNode getOpenBraceNode()
	{
		return null;
	}

	@Nullable
	@Override
	public ASTNode getClosingBraceNode()
	{
		return null;
	}

	@Nullable
	@Override
	public ASTNode getArrowNode()
	{
		return null;
	}

	@NotNull
	@Override
	public List<? extends NapileTypeReference> getTypeArguments()
	{
		return Collections.emptyList();
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitMethodType(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitMethodType(this, data);
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return new PsiElement[0];
	}
}
