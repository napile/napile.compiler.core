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

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NXmlParentedElementBase;
import org.napile.compiler.lang.psi.NapileDelegationSpecifierList;
import org.napile.compiler.lang.psi.NapileDelegationToSuperCall;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;

/**
 * @author VISTALL
 * @date 16:22/21.02.13
 */
public class NXmlDelegationSpecifierListImpl extends NXmlParentedElementBase implements NapileDelegationSpecifierList
{
	private List<NapileDelegationToSuperCall> delegationToSuperCalls;

	public NXmlDelegationSpecifierListImpl(PsiElement parent, PsiElement mirror)
	{
		super(parent, mirror);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileDelegationSpecifierList mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		final List<NapileDelegationToSuperCall> delegationSpecifiers = mirror.getDelegationSpecifiers();
		delegationToSuperCalls = new ArrayList<NapileDelegationToSuperCall>(delegationSpecifiers.size());
		for(NapileDelegationToSuperCall call : delegationSpecifiers)
		{
			delegationToSuperCalls.add(new NXmlDelegationToSuperCallImpl(this, call));
		}
	}

	@Override
	public List<NapileDelegationToSuperCall> getDelegationSpecifiers()
	{
		return delegationToSuperCalls;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitDelegationSpecifierList(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitDelegationSpecifierList(this, data);
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return delegationToSuperCalls.toArray(new PsiElement[delegationToSuperCalls.size()]);
	}
}
