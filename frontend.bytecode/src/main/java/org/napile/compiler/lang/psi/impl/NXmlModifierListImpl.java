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
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NXmlStubElementBase;
import org.napile.compiler.lang.psi.NapileAnnotation;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.psi.stubs.NapilePsiModifierListStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @since 14:03/17.02.13
 */
public class NXmlModifierListImpl extends NXmlStubElementBase<NapilePsiModifierListStub> implements NapileModifierList
{
	private List<NapileAnnotation> annotations = Collections.emptyList();

	public NXmlModifierListImpl(NapilePsiModifierListStub stub)
	{
		super(stub);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		final NapileModifierList mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		final List<NapileAnnotation> mirrorAnnotations = mirror.getAnnotations();
		annotations = new ArrayList<NapileAnnotation>(mirrorAnnotations.size());
		for(NapileAnnotation annotation : mirrorAnnotations)
		{
			annotations.add(new NXmlAnnotationImpl(this, annotation));
		}
	}

	@Override
	public boolean hasModifier(IElementType token)
	{
		return getStub().hasModifier(token);
	}

	@Nullable
	@Override
	public ASTNode getModifierNode(IElementType token)
	{
		return null;
	}

	@Override
	public List<NapileAnnotation> getAnnotations()
	{
		return annotations;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitModifierList(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitModifierList(this, data);
	}

	@Override
	public IStubElementType getElementType()
	{
		return NapileStubElementTypes.MODIFIER_LIST;
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return NXmlMirrorUtil.getAllToPsiArray(annotations);
	}
}
