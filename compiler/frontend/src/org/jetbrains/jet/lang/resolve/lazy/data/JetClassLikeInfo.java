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

package org.jetbrains.jet.lang.resolve.lazy.data;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.psi.JetClassObject;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetDeclarationContainer;
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier;
import org.jetbrains.jet.lang.psi.JetModifierList;
import org.jetbrains.jet.lang.psi.JetTypeParameter;
import org.jetbrains.jet.lang.resolve.name.FqName;
import com.intellij.psi.PsiElement;

/**
 * @author abreslav
 */
public interface JetClassLikeInfo extends JetDeclarationContainer<JetDeclaration>
{

	@NotNull
	FqName getContainingPackageFqName();

	@NotNull
	List<JetDelegationSpecifier> getDelegationSpecifiers();

	//@Nullable
	//Name getNameAsName();

	@Nullable
	JetModifierList getModifierList();

	@Nullable
	JetClassObject getClassObject();

	// This element is used to identify resolution scope for the class
	@NotNull
	PsiElement getScopeAnchor();

	@Nullable
	JetClassOrObject getCorrespondingClassOrObject();

	@NotNull
	List<JetTypeParameter> getTypeParameters();

	@NotNull
	ClassKind getClassKind();
}
