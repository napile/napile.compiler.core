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
import org.napile.compiler.lang.psi.NapileAnonymClass;
import org.napile.compiler.lang.psi.stubs.PsiJetObjectStub;
import org.napile.compiler.lang.resolve.name.FqName;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class PsiJetObjectStubImpl extends StubBase<NapileAnonymClass> implements PsiJetObjectStub
{
	private final StringRef name;
	private final FqName fqName;

	public PsiJetObjectStubImpl(@NotNull IStubElementType elementType, @NotNull StubElement parent, @NotNull String name, @Nullable FqName fqName)
	{
		this(elementType, parent, StringRef.fromString(name), fqName);
	}

	public PsiJetObjectStubImpl(@NotNull IStubElementType elementType, @NotNull StubElement parent, @NotNull StringRef name, @Nullable FqName fqName)
	{
		super(parent, elementType);

		this.name = name;
		this.fqName = fqName;
	}

	@Override
	public String getName()
	{
		return StringRef.toString(name);
	}

	@Nullable
	@Override
	public FqName getFQName()
	{
		return fqName;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();

		builder.append("PsiJetObjectStubImpl[");
		builder.append("name=").append(getName());

		builder.append(" fqName=").append(fqName != null ? fqName.toString() : "null");
		builder.append("]");

		return builder.toString();
	}
}
