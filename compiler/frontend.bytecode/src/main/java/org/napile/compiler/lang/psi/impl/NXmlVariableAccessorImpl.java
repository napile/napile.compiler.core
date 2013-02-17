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

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.lexer.NapileToken;
import org.napile.compiler.lang.psi.NXmlParentedElementBase;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.NapileVariableAccessor;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.doc.lang.psi.NapileDoc;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;

/**
 * @author VISTALL
 * @date 21:25/16.02.13
 */
public class NXmlVariableAccessorImpl extends NXmlParentedElementBase implements NapileVariableAccessor
{
	private IElementType elementType;

	public NXmlVariableAccessorImpl(PsiElement parent)
	{
		super(parent);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileVariableAccessor mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		elementType = mirror.getAccessorElementType();
	}

	@Nullable
	@Override
	public PsiElement getAccessorElement()
	{
		return null;
	}

	@Nullable
	@Override
	public IElementType getAccessorElementType()
	{
		return elementType;
	}

	@Nullable
	@Override
	public NapileExpression getBodyExpression()
	{
		return null;
	}

	@NotNull
	@Override
	public Name getNameAsSafeName()
	{
		return null;
	}

	@Nullable
	@Override
	public NapileDoc getDocComment()
	{
		return null;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitVariableAccessor(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitVariableAccessor(this, data);
	}

	@Nullable
	@Override
	public NapileModifierList getModifierList()
	{
		return null;
	}

	@Override
	public boolean hasModifier(NapileToken modifier)
	{
		return false;
	}

	@Nullable
	@Override
	public ASTNode getModifierNode(NapileToken token)
	{
		return null;
	}

	@Nullable
	@Override
	public Name getNameAsName()
	{
		return null;
	}

	@Nullable
	@Override
	public PsiElement getNameIdentifier()
	{
		return null;
	}

	@Override
	public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException
	{
		return null;
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return PsiElement.EMPTY_ARRAY;
	}
}
