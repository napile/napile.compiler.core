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

package org.napile.compiler.lang.psi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.descriptors.ClassKind;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.psi.stubs.NapilePsiClassStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileTypeReference;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;

/**
 * @author max
 */
public class NapileClassImpl extends NapileTypeParameterListOwnerStub<NapilePsiClassStub> implements NapileClass
{
	private static final TokenSet CLASS_DECL_KEYWORDS = TokenSet.create(NapileTokens.CLASS_KEYWORD, NapileTokens.ENUM_KEYWORD, NapileTokens.RETELL_KEYWORD);

	public NapileClassImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileClassImpl(@NotNull final NapilePsiClassStub stub)
	{
		super(stub, NapileStubElementTypes.CLASS);
	}

	@NotNull
	@Override
	public ClassKind getKind()
	{
		NapilePsiClassStub stub = getStub();
		if(stub != null)
			return stub.getKind();

		PsiElement element = findNotNullChildByType(CLASS_DECL_KEYWORDS);
		IElementType elementType = element.getNode().getElementType();
		if(elementType == NapileTokens.RETELL_KEYWORD)
			return ClassKind.RETELL_CLASS;
		else if(elementType == NapileTokens.ENUM_KEYWORD)
			return ClassKind.ENUM_CLASS;
		else
			return ClassKind.CLASS;
	}

	@NotNull
	@Override
	public NapileDeclaration[] getDeclarations()
	{
		NapileClassBody body = (NapileClassBody) findChildByType(NapileNodes.CLASS_BODY);
		if(body == null)
			return NapileDeclaration.EMPTY_ARRAY;

		return body.getDeclarations();
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitClass(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitClass(this, data);
	}

	@Override
	@NotNull
	public NapileConstructor[] getConstructors()
	{
		NapileClassBody body = (NapileClassBody) findChildByType(NapileNodes.CLASS_BODY);
		if(body == null)
			return NapileConstructor.EMPTY_ARRAY;

		return body.getConstructors();
	}

	@Override
	@NotNull
	public NapileStaticConstructor[] getStaticConstructors()
	{
		NapileClassBody body = (NapileClassBody) findChildByType(NapileNodes.CLASS_BODY);
		if(body == null)
			return NapileStaticConstructor.EMPTY_ARRAY;

		return body.getStaticConstructors();
	}

	@NotNull
	@Override
	public List<NapileTypeReference> getExtendTypeList()
	{
		NapileTypeListImpl ex = (NapileTypeListImpl)findChildByType(NapileNodes.EXTEND_TYPE_LIST);
		return ex == null ? Collections.<NapileTypeReference>emptyList() : ex.getTypeList();
	}

	@Override
	public FqName getFqName()
	{
		return NapilePsiUtil.getFQName(this);
	}

	@Override
	public NapileElement getExtendTypeListElement()
	{
		return (NapileElement)findChildByType(NapileNodes.EXTEND_TYPE_LIST);
	}

	@Override
	public NapileClassBody getBody()
	{
		return (NapileClassBody) findChildByType(NapileNodes.CLASS_BODY);
	}

	@NotNull
	@Override
	public IStubElementType getElementType()
	{
		return NapileStubElementTypes.CLASS;
	}

	@Override
	public void delete() throws IncorrectOperationException
	{
		NapilePsiUtil.deleteClass(this);
	}

	@Override
	public boolean isEquivalentTo(PsiElement another)
	{
		if(super.isEquivalentTo(another))
		{
			return true;
		}
		if(another instanceof NapileClass)
		{
			String fq1 = getQualifiedName();
			String fq2 = ((NapileClass) another).getQualifiedName();
			return fq1 != null && fq2 != null && fq1.equals(fq2);
		}
		return false;
	}

	@Nullable
	@Override
	public String getQualifiedName()
	{
		NapilePsiClassStub stub = getStub();
		if(stub != null)
		{
			return stub.getQualifiedName();
		}

		List<String> parts = new ArrayList<String>();
		NapileClassLike current = this;
		while(current != null)
		{
			parts.add(current.getName());
			current = PsiTreeUtil.getParentOfType(current, NapileClassLike.class);
		}
		NapileFile file = getContainingFile();
		String fileQualifiedName = file.getNamespaceHeader().getQualifiedName();
		if(!fileQualifiedName.isEmpty())
		{
			parts.add(fileQualifiedName);
		}
		Collections.reverse(parts);
		return StringUtil.join(parts, ".");
	}

	/**
	 * Returns the list of unqualified names that are indexed as the superclass names of this class. For the names that might be imported
	 * via an aliased import, includes both the original and the aliased name (reference resolution during inheritor search will sort this out).
	 *
	 * @return the list of possible superclass names
	 */
	@Override
	@NotNull
	public List<String> getSuperNames()
	{
		NapilePsiClassStub stub = getStub();
		if(stub != null)
		{
			return stub.getSuperNames();
		}

		final List<NapileTypeReference> types = getExtendTypeList();
		if(types.size() == 0)
			return Collections.emptyList();
		List<String> result = new ArrayList<String>();
		for(NapileTypeReference specifier : types)
		{
			final NapileTypeElement superType = specifier.getTypeElement();
			if(superType instanceof NapileUserType)
			{
				final String referencedName = ((NapileUserType)superType).getReferencedName();
				if(referencedName != null)
				{
					addSuperName(result, referencedName);
				}
			}
		}
		return result;
	}

	private void addSuperName(List<String> result, String referencedName)
	{
		result.add(referencedName);
		final NapileImportDirective directive = getContainingFile().findImportByAlias(referencedName);
		if(directive != null)
		{
			NapileExpression reference = directive.getImportedReference();
			while(reference instanceof NapileDotQualifiedExpression)
			{
				reference = ((NapileDotQualifiedExpression) reference).getSelectorExpression();
			}
			if(reference instanceof NapileSimpleNameExpression)
			{
				result.add(((NapileSimpleNameExpression) reference).getReferencedName());
			}
		}
	}

	@Override
	public ItemPresentation getPresentation()
	{
		return ItemPresentationProviders.getItemPresentation(this);
	}
}
