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

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NXmlParentedElementBase;
import org.napile.compiler.lang.psi.NapileCallParameter;
import org.napile.compiler.lang.psi.NapileCallParameterList;
import org.napile.compiler.lang.psi.NapileMethodType;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;

/**
 * @author VISTALL
 * @date 18:52/16.02.13
 */
public class NXmlMethodTypeImpl extends NXmlParentedElementBase implements NapileMethodType
{
	private NXmlTypeReferenceImpl returnType;
	private NXmlCallParameterListImpl callParameterList;

	public NXmlMethodTypeImpl(PsiElement parent, PsiElement mirror)
	{
		super(parent, mirror);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileMethodType mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		returnType = new NXmlTypeReferenceImpl(this, mirror.getReturnTypeRef());
		callParameterList = new NXmlCallParameterListImpl(this, mirror.getParameterList());
	}

	@Nullable
	@Override
	public NapileCallParameterList getParameterList()
	{
		return callParameterList;
	}

	@NotNull
	@Override
	public NapileCallParameter[] getParameters()
	{
		NapileCallParameterList list = getParameterList();
		return list != null ? list.getParameters() : NapileCallParameter.EMPTY_ARRAY;
	}

	@Nullable
	@Override
	public NapileTypeReference getReturnTypeRef()
	{
		return returnType;
	}

	@NotNull
	@Override
	public ASTNode getOpenBraceNode()
	{
		return null;
	}

	@Nullable
	@Override
	public ASTNode getClosingBraceNode()
	{
		return null;
	}

	@Nullable
	@Override
	public ASTNode getArrowNode()
	{
		return null;
	}

	@NotNull
	@Override
	public List<? extends NapileTypeReference> getTypeArguments()
	{
		return Collections.emptyList();
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitMethodType(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitMethodType(this, data);
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return NXmlMirrorUtil.getAllToPsiArray(returnType, callParameterList);
	}
}
