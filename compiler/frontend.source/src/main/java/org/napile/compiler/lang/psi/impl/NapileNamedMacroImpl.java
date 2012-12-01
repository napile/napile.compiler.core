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

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileNamedMacro;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.psi.stubs.NapilePsiMacroStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;

/**
 * @author VISTALL
 * @date 12:14/18.11.12
 */
public class NapileNamedMacroImpl extends NapileNamedMethodOrMacroImpl<NapilePsiMacroStub> implements NapileNamedMacro
{
	public NapileNamedMacroImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileNamedMacroImpl(@NotNull NapilePsiMacroStub stub)
	{
		super(stub, NapileStubElementTypes.MACRO);
	}

	@Override
	public String getName()
	{
		NapilePsiMacroStub stub = getStub();
		if(stub != null)
			return stub.getName();

		PsiElement identifier = getNameIdentifier();
		return identifier != null ? identifier.getText() : null;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitNamedMacro(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitNamedMacro(this, data);
	}

	@NotNull
	@Override
	public IStubElementType getElementType()
	{
		return NapileStubElementTypes.MACRO;
	}
}
