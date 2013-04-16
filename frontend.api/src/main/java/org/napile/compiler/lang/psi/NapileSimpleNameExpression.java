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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @since 8:13/20.10.12
 */
public interface NapileSimpleNameExpression extends NapileReferenceExpression
{
	@Nullable
	NapileExpression getReceiverExpression();

	boolean isImportDirectiveExpression();

	@Nullable
	@IfNotParsed
	String getReferencedName();

	Name getReferencedNameAsName();

	@NotNull
	PsiElement getReferencedNameElement();

	@Nullable
	@IfNotParsed
	IElementType getReferencedNameElementType();
}