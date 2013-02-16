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
import org.napile.compiler.lang.psi.NXmlParentedElementBase;
import org.napile.compiler.lang.psi.NapileAnnotation;
import org.napile.compiler.lang.psi.NapileTypeElement;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;

/**
 * @author VISTALL
 * @date 16:18/16.02.13
 */
public class NXmlTypeReferenceImpl extends NXmlParentedElementBase implements NapileTypeReference
{
	private PsiElement[] children;

	public NXmlTypeReferenceImpl(PsiElement parent)
	{
		super(parent);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileTypeReference mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		children = new PsiElement[] {NXmlMirrorUtil.mirrorTypeElement(this, mirror.getTypeElement())};

		setMirror(getTypeElement(), mirror.getTypeElement());
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return children;
	}

	@Nullable
	@Override
	public NapileTypeElement getTypeElement()
	{
		return findChildByClass(NapileTypeElement.class);
	}

	@Override
	public List<NapileAnnotation> getAnnotations()
	{
		return findChildrenByClassAsList(NapileAnnotation.class);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitTypeReference(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitTypeReference(this, data);
	}
}
