/*
 * Copyright 2010-2012 napile.org
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

package org.napile.compiler.lang.psi;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.psi.impl.PsiFileEx;
import com.intellij.psi.impl.source.PsiFileWithStubSupport;

/**
 * @author VISTALL
 * @date 20:02/09.10.12
 */
public interface NapileFile extends NapileDeclarationContainer<NapileClass>, PsiFileWithStubSupport, PsiFileEx, NapileElement
{
	@NotNull
	NapilePackage getPackage();

	@Nullable
	String getPackageName();

	@Nullable
	NapileImportDirective findImportByAlias(@NotNull String name);

	@NotNull
	List<NapileImportDirective> getImportDirectives();

	boolean isCompiled();
}
