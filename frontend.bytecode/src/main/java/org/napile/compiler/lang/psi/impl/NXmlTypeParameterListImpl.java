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

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NXmlStubElementBase;
import org.napile.compiler.lang.psi.NapileTypeParameter;
import org.napile.compiler.lang.psi.NapileTypeParameterList;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.psi.stubs.NapilePsiTypeParameterListStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.stubs.IStubElementType;

/**
 * @author VISTALL
 * @since 13:56/16.02.13
 */
public class NXmlTypeParameterListImpl extends NXmlStubElementBase<NapilePsiTypeParameterListStub> implements NapileTypeParameterList
{
	public NXmlTypeParameterListImpl(NapilePsiTypeParameterListStub stub)
	{
		super(stub);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileTypeParameterList mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		setMirrors(getParameters(), mirror.getParameters());
	}

	@NotNull
	@Override
	public NapileTypeParameter[] getParameters()
	{
		return getStub().getChildrenByType(NapileStubElementTypes.TYPE_PARAMETER, NapileTypeParameter.ARRAY_FACTORY);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitTypeParameterList(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitTypeParameterList(this, data);
	}

	@Override
	public IStubElementType getElementType()
	{
		return NapileStubElementTypes.TYPE_PARAMETER_LIST;
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return getParameters();
	}
}
