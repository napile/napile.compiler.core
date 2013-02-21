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

package org.napile.compiler.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.psi.impl.NXmlMethodTypeImpl;
import org.napile.compiler.lang.psi.impl.NXmlMultiTypeImpl;
import org.napile.compiler.lang.psi.impl.NXmlNullableTypeImpl;
import org.napile.compiler.lang.psi.impl.NXmlSelfTypeImpl;
import org.napile.compiler.lang.psi.impl.NXmlSimpleNameExpressionImpl;
import org.napile.compiler.lang.psi.impl.NXmlTypeReferenceImpl;
import org.napile.compiler.lang.psi.impl.NXmlUserTypeImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;

/**
 * @author VISTALL
 * @date 16:16/16.02.13
 */
public class NXmlMirrorUtil
{
	private static final NXmlExpressionMirrorVisitor EXPRESSION_MIRROR_VISITOR = new NXmlExpressionMirrorVisitor();

	public static NapileExpression mirrorExpression(NXmlElementBase parent, NapileExpression expression)
	{
		NXmlElementBase mirror = expression.accept(EXPRESSION_MIRROR_VISITOR, parent);
		if(mirror == null)
		{
			throw new UnsupportedOperationException("this expression is not supported for mirroring " + expression.getClass().getName());
		}
		mirror.setMirror(expression);
		return (NapileExpression) mirror;
	}

	@NotNull
	public static PsiElement[] getAllToPsiArray(Object... arg)
	{
		List<PsiElement> list = new ArrayList<PsiElement>();
		for(Object o : arg)
		{
			if(o instanceof PsiElement)
				list.add((PsiElement) o);
			else if(o instanceof Iterable)
			{
				for(PsiElement o1 : ((Iterable<PsiElement>) o))
				{
					list.add(o1);
				}
			}
			else if(o instanceof PsiElement[])
			{
				Collections.addAll(list, (PsiElement[]) o);
			}
		}

		return list.toArray(new PsiElement[list.size()]);
	}

	@Nullable
	public static NapileSimpleNameExpression mirrorSimpleNameExpression(PsiElement parent, @Nullable NapileSimpleNameExpression expression)
	{
		if(expression == null)
			return null;
		NXmlSimpleNameExpressionImpl e = new NXmlSimpleNameExpressionImpl(parent);
		e.setMirror(SourceTreeToPsiMap.psiToTreeNotNull(expression));
		return e;
	}

	public static List<NXmlTypeReferenceImpl> mirrorTypes(PsiElement parent, Collection<? extends NapileTypeReference> typeReferences)
	{
		List<NXmlTypeReferenceImpl> list = new ArrayList<NXmlTypeReferenceImpl>(typeReferences.size());
		for(NapileTypeReference typeReference : typeReferences)
		{
			list.add(mirrorType(parent, typeReference));
		}
		return list;
	}

	public static NXmlTypeReferenceImpl mirrorType(PsiElement parent, NapileTypeReference typeReference)
	{
		NXmlTypeReferenceImpl nXmlTypeReference = new NXmlTypeReferenceImpl(parent);
		nXmlTypeReference.setMirror(SourceTreeToPsiMap.psiToTreeNotNull(typeReference));
		return nXmlTypeReference;
	}

	public static NapileTypeElement mirrorTypeElement(PsiElement parent, @Nullable NapileTypeElement typeElement)
	{
		if(typeElement == null)
			return null;

		NXmlParentedElementBase type = null;
		if(typeElement instanceof NapileUserType)
		{
			type = new NXmlUserTypeImpl(parent);
		}
		else if(typeElement instanceof NapileNullableType)
		{
			type = new NXmlNullableTypeImpl(parent);
		}
		else if(typeElement instanceof NapileSelfType)
		{
			type = new NXmlSelfTypeImpl(parent);
		}
		else if(typeElement instanceof NapileMultiType)
		{
			type = new NXmlMultiTypeImpl(parent);
		}
		else if(typeElement instanceof NapileMethodType)
		{
			type = new NXmlMethodTypeImpl(parent);
		}
		else
		{
			throw new UnsupportedOperationException(typeElement.getClass().getName());
		}

		type.setMirror(SourceTreeToPsiMap.psiToTreeNotNull(typeElement));

		return (NapileTypeElement) type;
	}
}
