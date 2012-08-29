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

package org.napile.compiler.lang.resolve.lazy.data;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.ClassKind;
import org.napile.compiler.lang.psi.NapileLikeClass;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileDelegationSpecifier;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.NapileTypeParameter;
import org.napile.compiler.lang.resolve.name.FqName;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;

/**
 * @author abreslav
 */
public class FilteringClassLikeInfo implements NapileClassLikeInfo
{
	private final NapileClassLikeInfo delegate;
	private final Predicate<? super NapileDeclaration> declarationFilter;
	private List<NapileDeclaration> filteredDeclarations;

	public FilteringClassLikeInfo(@NotNull NapileClassLikeInfo delegate, @NotNull Predicate<? super NapileDeclaration> declarationFilter)
	{
		this.delegate = delegate;
		this.declarationFilter = declarationFilter;
	}

	@NotNull
	@Override
	public FqName getContainingPackageFqName()
	{
		return delegate.getContainingPackageFqName();
	}

	@Override
	@NotNull
	public List<NapileDelegationSpecifier> getDelegationSpecifiers()
	{
		return delegate.getDelegationSpecifiers();
	}

	@Override
	@Nullable
	public NapileModifierList getModifierList()
	{
		return delegate.getModifierList();
	}

	@Override
	@NotNull
	public PsiElement getScopeAnchor()
	{
		return delegate.getScopeAnchor();
	}

	@Override
	@Nullable
	public NapileLikeClass getCorrespondingClassOrObject()
	{
		return delegate.getCorrespondingClassOrObject();
	}

	@Override
	@NotNull
	public List<NapileTypeParameter> getTypeParameters()
	{
		return delegate.getTypeParameters();
	}


	@Override
	@NotNull
	public ClassKind getClassKind()
	{
		return delegate.getClassKind();
	}

	@Override
	@NotNull
	public List<NapileDeclaration> getDeclarations()
	{
		if(filteredDeclarations == null)
		{
			filteredDeclarations = Lists.newArrayList(Collections2.filter(delegate.getDeclarations(), declarationFilter));
		}
		return filteredDeclarations;
	}

	@Override
	public String toString()
	{
		return "filtering " + delegate.toString();
	}
}
