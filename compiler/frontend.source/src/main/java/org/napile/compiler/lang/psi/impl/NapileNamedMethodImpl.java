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
import org.jetbrains.annotations.Nullable;
import org.napile.asm.AsmConstants;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileNamedMethod;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.psi.stubs.NapilePsiMethodStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @date 12:09/18.11.12
 */
public class NapileNamedMethodImpl extends NapileNamedMethodOrMacroImpl<NapilePsiMethodStub> implements NapileNamedMethod
{
	public NapileNamedMethodImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileNamedMethodImpl(@NotNull NapilePsiMethodStub stub)
	{
		super(stub, NapileStubElementTypes.METHOD);
	}

	@Override
	public String getName()
	{
		NapilePsiMethodStub stub = getStub();
		if(stub != null)
			return stub.getName();

		PsiElement psiElement = findChildByType(NapileTokens.PROPERTY_KEYWORDS);
		if(psiElement != null)
		{
			NapileSimpleNameExpression ref = getVariableRef();
			assert ref != null;
			return ref.getReferencedName() + AsmConstants.ANONYM_SPLITTER + psiElement.getText();
		}
		else
		{
			PsiElement identifier = getNameIdentifier();
			return identifier != null ? identifier.getText() : null;
		}
	}

	@Nullable
	@Override
	public NapileSimpleNameExpression getVariableRef()
	{
		return (NapileSimpleNameExpression) findChildByType(NapileNodes.VARIABLE_REFERENCE);
	}

	@Nullable
	@Override
	public IElementType getPropertyAccessType()
	{
		PsiElement element = getPropertyAccessElement();
		return element == null ? TokenType.ERROR_ELEMENT : element.getNode().getElementType();
	}

	@Nullable
	@Override
	public PsiElement getPropertyAccessElement()
	{
		return findChildByType(NapileTokens.PROPERTY_KEYWORDS);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitNamedMethod(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitNamedMethod(this, data);
	}

	@NotNull
	@Override
	public IStubElementType getElementType()
	{
		return NapileStubElementTypes.METHOD;
	}
}
