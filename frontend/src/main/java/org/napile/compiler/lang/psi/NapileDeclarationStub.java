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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.lexer.NapileToken;
import org.napile.compiler.lang.psi.impl.NapileElementImplStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;

/**
 * @author Nikolay Krasko
 */
abstract class NapileDeclarationStub<T extends StubElement> extends NapileElementImplStub<T> implements NapileDeclaration
{
	public NapileDeclarationStub(@NotNull T stub, @NotNull IStubElementType nodeType)
	{
		super(stub, nodeType);
	}

	public NapileDeclarationStub(@NotNull ASTNode node)
	{
		super(node);
	}

	@Override
	@Nullable
	public NapileModifierList getModifierList()
	{
		return getStubOrPsiChild(NapileStubElementTypes.MODIFIER_LIST);
	}

	@Override
	public boolean hasModifier(NapileToken modifier)
	{
		NapileModifierList modifierList = getModifierList();
		return modifierList != null && modifierList.hasModifier(modifier);
	}

	@Nullable
	@Override
	public ASTNode getModifierNode(NapileToken napileToken)
	{
		NapileModifierList modifierList = getModifierList();
		return modifierList == null ? null : modifierList.getModifierNode(napileToken);
	}
}
