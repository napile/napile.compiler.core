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

package org.napile.compiler.lang.psi;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.lexer.NapileTokens;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.NamedStub;
import com.intellij.util.IncorrectOperationException;

/**
 * @author Nikolay Krasko
 */
public abstract class NapileNamedDeclarationStub<T extends NamedStub> extends NapileDeclarationStub<T> implements NapileNamedDeclaration
{
	public NapileNamedDeclarationStub(@NotNull T stub, @NotNull IStubElementType nodeType)
	{
		super(stub, nodeType);
	}

	public NapileNamedDeclarationStub(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	public String getName()
	{
		T stub = getStub();
		if(stub != null)
		{
			return stub.getName();
		}

		PsiElement identifier = getNameIdentifier();
		return identifier != null ? identifier.getText() : null;
	}

	@Override
	public Name getNameAsName()
	{
		String name = getName();
		return name != null ? Name.identifier(name) : null;
	}

	@Override
	@NotNull
	public Name getNameAsSafeName()
	{
		return NapilePsiUtil.safeName(getName());
	}

	@Override
	public PsiElement getNameIdentifier()
	{
		return findChildByType(NapileTokens.IDENTIFIER);
	}

	@Override
	public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException
	{
		return getNameIdentifier().replace(NapilePsiFactory.createNameIdentifier(getProject(), name));
	}

	@Override
	public int getTextOffset()
	{
		PsiElement identifier = getNameIdentifier();
		return identifier != null ? identifier.getTextRange().getStartOffset() : getTextRange().getStartOffset();
	}
}
