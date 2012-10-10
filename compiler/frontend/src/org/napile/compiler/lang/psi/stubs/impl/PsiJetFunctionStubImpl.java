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

package org.napile.compiler.lang.psi.stubs.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NapileNamedMethod;
import org.napile.compiler.lang.psi.stubs.PsiJetFunctionStub;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.ArrayUtil;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class PsiJetFunctionStubImpl extends StubBase<NapileNamedMethod> implements PsiJetFunctionStub
{
	private final StringRef nameRef;

	public PsiJetFunctionStubImpl(@NotNull IStubElementType elementType, @NotNull StubElement parent, @Nullable String name)
	{
		this(elementType, parent, StringRef.fromString(name));
	}

	public PsiJetFunctionStubImpl(@NotNull IStubElementType elementType, @NotNull StubElement parent, @Nullable StringRef nameRef)
	{
		super(parent, elementType);

		this.nameRef = nameRef;
	}

	@Override
	public String getName()
	{
		return StringRef.toString(nameRef);
	}

	@NotNull
	@Override
	public String[] getAnnotations()
	{
		// TODO (stubs)
		return ArrayUtil.EMPTY_STRING_ARRAY;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("PsiJetFunctionStubImpl[");

		builder.append("name=").append(getName());

		builder.append("]");

		return builder.toString();
	}
}
