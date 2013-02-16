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
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NXmlParentedElementBase;
import org.napile.compiler.lang.psi.NapileMethodType;
import org.napile.compiler.lang.psi.NapileMultiType;
import org.napile.compiler.lang.psi.NapileNullableType;
import org.napile.compiler.lang.psi.NapileSelfType;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileTypeElement;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileUserType;
import org.napile.compiler.lang.psi.impl.NXmlIdentifierImpl;
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
				for(PsiElement element : (PsiElement[]) o)
				{
					list.add(element);
				}
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

	@Nullable
	public static NXmlIdentifierImpl mirrorIdentifier(PsiElement parent, @NotNull PsiElement name)
	{
		NXmlIdentifierImpl e = new NXmlIdentifierImpl(parent);
		e.setMirror(SourceTreeToPsiMap.psiToTreeNotNull(name));
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
