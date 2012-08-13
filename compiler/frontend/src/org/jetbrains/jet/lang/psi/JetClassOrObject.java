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

package org.jetbrains.jet.lang.psi;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import com.intellij.psi.PsiNameIdentifierOwner;

/**
 * @author max
 */
public interface JetClassOrObject extends PsiNameIdentifierOwner, JetDeclarationContainer, JetModifierListOwner
{
	JetClassOrObject[] EMPTY_ARRAY = new JetClassOrObject[0];

	@Nullable
	JetDelegationSpecifierList getDelegationSpecifierList();

	@NotNull
	List<JetDelegationSpecifier> getDelegationSpecifiers();

	@NotNull
	List<JetClassInitializer> getAnonymousInitializers();

	@Nullable
	Name getNameAsName();

	FqName getFqName();

	@Nullable
	JetObjectDeclarationName getNameAsDeclaration();

	@Nullable
	JetClassBody getBody();
}
