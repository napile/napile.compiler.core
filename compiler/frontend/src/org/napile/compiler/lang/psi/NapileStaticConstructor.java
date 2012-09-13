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

package org.napile.compiler.lang.psi;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lexer.JetTokens;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;

/**
 * @author VISTALL
 * @date 22:20/13.09.12
 */
public class NapileStaticConstructor extends NapileDeclarationImpl implements NapileDeclarationWithBody, NapileStatementExpression, NapileNamedDeclaration
{
	public static final NapileStaticConstructor[] EMPTY_ARRAY = new NapileStaticConstructor[0];

	public NapileStaticConstructor(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitStaticConstructor(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitStaticConstructor(this, data);
	}

	@Override
	public NapileExpression getBodyExpression()
	{
		return findChildByClass(NapileExpression.class);
	}

	@Override
	public boolean hasBlockBody()
	{
		return true;
	}

	@Override
	public boolean hasDeclaredReturnType()
	{
		return false;
	}

	@NotNull
	@Override
	public NapileElement asElement()
	{
		return this;
	}

	@NotNull
	@Override
	public List<NapileElement> getValueParameters()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public Name getNameAsSafeName()
	{
		return Name.identifier(getName());
	}

	@Nullable
	@Override
	public Name getNameAsName()
	{
		return Name.identifier(getName());
	}

	@NotNull
	@Override
	public PsiElement getNameIdentifier()
	{
		return findNotNullChildByType(JetTokens.STATIC_KEYWORD);
	}

	@Override
	public PsiElement setName(@NonNls @NotNull String s) throws IncorrectOperationException
	{
		throw new IncorrectOperationException();
	}

	@Override
	@NotNull
	public String getName()
	{
		return "static";
	}
}
