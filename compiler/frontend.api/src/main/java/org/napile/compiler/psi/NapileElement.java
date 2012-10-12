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

package org.napile.compiler.psi;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileTreeVisitor;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.util.ArrayFactory;

/**
 * @author Nikolay Krasko
 */
public interface NapileElement extends NavigatablePsiElement
{
	NapileElement[] EMPTY_ARRAY = new NapileElement[0];

	ArrayFactory<NapileElement> ARRAY_FACTORY = new ArrayFactory<NapileElement>()
	{
		@Override
		public NapileElement[] create(final int count)
		{
			return count == 0 ? EMPTY_ARRAY : new NapileElement[count];
		}
	};

	<D> void acceptChildren(@NotNull NapileTreeVisitor<D> visitor, D data);

	void accept(@NotNull NapileVisitorVoid visitor);

	<R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data);

	@Override
	NapileFile getContainingFile() throws PsiInvalidElementAccessException;
}
