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
import org.napile.compiler.lang.psi.NapileClassLike;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileDelegationSpecifier;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.resolve.name.FqName;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

/**
 * @author abreslav
 */
public abstract class NapileClassOrObjectInfo<E extends NapileClassLike> implements NapileClassLikeInfo
{
	protected final E element;

	protected NapileClassOrObjectInfo(@NotNull E element)
	{
		this.element = element;
	}

	@Override
	public NapileClassLike getCorrespondingClassOrObject()
	{
		return element;
	}

	@Override
	@NotNull
	public List<NapileDelegationSpecifier> getDelegationSpecifiers()
	{
		return element.getDelegationSpecifiers();
	}

	//@Override
	//@Nullable
	//public Name getNameAsName() {
	//    return element.getNameAsName();
	//}

	@Override
	@Nullable
	public NapileModifierList getModifierList()
	{
		return element.getModifierList();
	}

	@Override
	@NotNull
	public List<NapileDeclaration> getDeclarations()
	{
		return element.getDeclarations();
	}

	@NotNull
	@Override
	public PsiElement getScopeAnchor()
	{
		return element;
	}

	@NotNull
	@Override
	public FqName getContainingPackageFqName()
	{
		PsiFile file = element.getContainingFile();
		if(file instanceof NapileFile)
		{
			NapileFile jetFile = (NapileFile) file;
			return new FqName(jetFile.getNamespaceHeader().getQualifiedName());
		}
		throw new IllegalArgumentException("Not in a NapileFile: " + element);
	}

	@Override
	public String toString()
	{
		return "info for " + element.getText();
	}
}
