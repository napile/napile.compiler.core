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
import org.napile.compiler.lang.psi.NapileClassLike;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileDeclarationContainer;
import org.napile.compiler.lang.psi.NapileDelegationSpecifier;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.NapileTypeParameter;
import org.napile.compiler.lang.resolve.name.FqName;
import com.intellij.psi.PsiElement;

/**
 * @author abreslav
 */
public interface NapileClassLikeInfo extends NapileDeclarationContainer<NapileDeclaration>
{

	@NotNull
	FqName getContainingPackageFqName();

	@NotNull
	List<NapileDelegationSpecifier> getDelegationSpecifiers();

	//@Nullable
	//Name getNameAsName();

	@Nullable
	NapileModifierList getModifierList();

	// This element is used to identify resolution scope for the class
	@NotNull
	PsiElement getScopeAnchor();

	@Nullable
	NapileClassLike getCorrespondingClassOrObject();

	@NotNull
	List<NapileTypeParameter> getTypeParameters();

	@NotNull
	ClassKind getClassKind();
}
