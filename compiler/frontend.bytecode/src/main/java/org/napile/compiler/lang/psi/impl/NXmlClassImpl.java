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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.psi.stubs.NapilePsiClassStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author VISTALL
 * @date 15:05/19.10.12
 */
public class NXmlClassImpl extends NXmlTypeParameterOwnerStub<NapilePsiClassStub> implements NapileClass
{
	private NXmlTypeListImpl superTypeList;

	public NXmlClassImpl(NapilePsiClassStub stub)
	{
		super(stub);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileClass mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		setMirrorIfPresent(getTypeParameterList(), mirror.getTypeParameterList());

		try
		{
			setMirrors(getDeclarations(), mirror.getDeclarations());
		}
		catch(InvalidMirrorException e)
		{
			System.out.println("NXmlClassImpl mirroring failed: " + getContainingFile().getVirtualFile() + ", " + e.getMessage());
		}

		PsiElement t = mirror.getSuperTypesElement();
		if(t != null)
		{
			superTypeList = new NXmlTypeListImpl(this);
			superTypeList.setMirror(SourceTreeToPsiMap.psiToTreeNotNull(t));
		}

		nameIdentifier = NXmlMirrorUtil.mirrorIdentifier(this, mirror.getNameIdentifier());
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
		String fileQualifiedName = file.getPackage().getQualifiedName();
		if(!fileQualifiedName.isEmpty())
		{
			parts.add(fileQualifiedName);
		}
		Collections.reverse(parts);
		return StringUtil.join(parts, ".");
	}

	@NotNull
	@Override
	public NapileConstructor[] getConstructors()
	{
		return getStub().getChildrenByType(NapileStubElementTypes.CONSTRUCTOR, NapileConstructor.ARRAY_FACTORY);
	}

	@Override
	public FqName getFqName()
	{
		return NapilePsiUtil.getFQName(this);
	}

	@Nullable
	@Override
	public NapileClassBody getBody()
	{
		return null;
	}

	@NotNull
	@Override
	public NapileDeclaration[] getDeclarations()
	{
		return getStub().getChildrenByType(NapileStubElementTypes.CLASS_MEMBERS, NapileDeclaration.ARRAY_FACTORY);
	}

	@NotNull
	@Override
	public List<? extends NapileTypeReference> getSuperTypes()
	{
		return superTypeList == null ? Collections.<NapileTypeReference>emptyList() : superTypeList.getTypeList();
	}

	@Nullable
	@Override
	public NapileElement getSuperTypesElement()
	{
		return superTypeList;
	}

	@Override
	public IStubElementType getElementType()
	{
		return NapileStubElementTypes.CLASS;
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

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return NXmlMirrorUtil.getAllToPsiArray(superTypeList, nameIdentifier, getDeclarations());
	}

	@Override
	public ItemPresentation getPresentation()
	{
		return ItemPresentationProviders.getItemPresentation(this);
	}
}
