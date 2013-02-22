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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NapileCallParameterList;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileTypeParameter;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.psi.stubs.NapilePsiTypeParameterStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.stubs.IStubElementType;

/**
 * @author VISTALL
 * @date 14:05/16.02.13
 */
public class NXmlTypeParameterImpl extends NXmlNamedDeclarationImpl<NapilePsiTypeParameterStub> implements NapileTypeParameter
{
	private List<NXmlTypeReferenceImpl> superTypes;
	private NapileCallParameterList[] parameterLists;

	public NXmlTypeParameterImpl(NapilePsiTypeParameterStub stub)
	{
		super(stub);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileTypeParameter mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		nameIdentifier = new NXmlIdentifierImpl(this, mirror.getNameIdentifier());
		superTypes = NXmlMirrorUtil.mirrorTypes(this, mirror.getSuperTypes());

		final NapileCallParameterList[] mirrorParametersList = mirror.getConstructorParameterLists();
		parameterLists = new NapileCallParameterList[mirrorParametersList.length];
		for(int i = 0; i < mirrorParametersList.length; i++)
			parameterLists[i] = new NXmlCallParameterListImpl(this, mirrorParametersList[i]);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitTypeParameter(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitTypeParameter(this, data);
	}

	@Override
	public IStubElementType getElementType()
	{
		return NapileStubElementTypes.TYPE_PARAMETER;
	}

	@NotNull
	@Override
	public NapileCallParameterList[] getConstructorParameterLists()
	{
		return parameterLists;
	}

	@NotNull
	@Override
	public List<? extends NapileTypeReference> getSuperTypes()
	{
		return superTypes;
	}

	@Nullable
	@Override
	public NapileElement getSuperTypesElement()
	{
		return null;
	}

	@Override
	@NotNull
	public PsiElement[] getChildren()
	{
		return NXmlMirrorUtil.getAllToPsiArray(nameIdentifier, superTypes, parameterLists);
	}
}
