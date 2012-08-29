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

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.NapileNodeTypes;
import org.napile.compiler.lang.psi.stubs.PsiJetObjectStub;
import org.napile.compiler.lang.psi.stubs.elements.JetStubElementTypes;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lexer.JetTokens;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.IncorrectOperationException;

/**
 * @author abreslav
 */
public class NapileAnonymClass extends NapileNamedDeclarationStub<PsiJetObjectStub> implements NapileLikeClass
{
	public NapileAnonymClass(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileAnonymClass(@NotNull PsiJetObjectStub stub)
	{
		super(stub, JetStubElementTypes.OBJECT_DECLARATION);
	}

	@NotNull
	@Override
	public IStubElementType getElementType()
	{
		return JetStubElementTypes.OBJECT_DECLARATION;
	}

	@Override
	public String getName()
	{
		PsiJetObjectStub stub = getStub();
		if(stub != null)
		{
			return stub.getName();
		}

		NapileObjectDeclarationName nameAsDeclaration = getNameAsDeclaration();
		return nameAsDeclaration == null ? null : nameAsDeclaration.getName();
	}

	/**
	 * Could be null for anonymous objects and object declared inside functions
	 *
	 * @return
	 */
	@Override
	public FqName getFqName()
	{
		PsiJetObjectStub stub = getStub();
		if(stub != null)
		{
			return stub.getFQName();
		}

		return NapilePsiUtil.getFQName(this);
	}

	@Override
	public PsiElement getNameIdentifier()
	{
		NapileObjectDeclarationName nameAsDeclaration = getNameAsDeclaration();
		return nameAsDeclaration == null ? null : nameAsDeclaration.getNameIdentifier();
	}

	@Override
	public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException
	{
		NapileObjectDeclarationName nameAsDeclaration = getNameAsDeclaration();
		return nameAsDeclaration == null ? null : nameAsDeclaration.setName(name);
	}

	@Override
	@Nullable
	public NapileObjectDeclarationName getNameAsDeclaration()
	{
		return (NapileObjectDeclarationName) findChildByType(NapileNodeTypes.OBJECT_DECLARATION_NAME);
	}

	@Override
	@Nullable
	public NapileModifierList getModifierList()
	{
		PsiElement parent = getParent();
		if(isClassObject(parent))
		{
			assert parent instanceof NapileDeclaration;
			return ((NapileDeclaration) parent).getModifierList();
		}
		return (NapileModifierList) findChildByType(NapileNodeTypes.MODIFIER_LIST);
	}

	@Deprecated
	private static boolean isClassObject(@NotNull PsiElement parent)
	{
		return false;
	}

	@Override
	@Nullable
	public NapileDelegationSpecifierList getDelegationSpecifierList()
	{
		return (NapileDelegationSpecifierList) findChildByType(NapileNodeTypes.DELEGATION_SPECIFIER_LIST);
	}

	@Override
	@NotNull
	public List<NapileDelegationSpecifier> getDelegationSpecifiers()
	{
		NapileDelegationSpecifierList list = getDelegationSpecifierList();
		return list != null ? list.getDelegationSpecifiers() : Collections.<NapileDelegationSpecifier>emptyList();
	}

	@Override
	@NotNull
	public List<NapileClassInitializer> getAnonymousInitializers()
	{
		NapileClassBody body = (NapileClassBody) findChildByType(NapileNodeTypes.CLASS_BODY);
		if(body == null)
			return Collections.emptyList();

		return body.getAnonymousInitializers();
	}

	@Override
	public NapileClassBody getBody()
	{
		return (NapileClassBody) findChildByType(NapileNodeTypes.CLASS_BODY);
	}

	@Override
	@NotNull
	public List<NapileDeclaration> getDeclarations()
	{
		NapileClassBody body = getBody();
		if(body == null)
			return Collections.emptyList();

		return body.getDeclarations();
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitObjectDeclaration(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitObjectDeclaration(this, data);
	}

	@NotNull
	public PsiElement getObjectKeyword()
	{
		return findChildByType(JetTokens.ANONYM_KEYWORD);
	}

	@Override
	public void delete() throws IncorrectOperationException
	{
		NapilePsiUtil.deleteClass(this);
	}

	//@Override
	//public ItemPresentation getPresentation() {
	//    return ItemPresentationProviders.getItemPresentation(this);
	//}
}
