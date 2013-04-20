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

package org.napile.compiler.lang.psi.stubs;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.psi.stubs.NamedStub;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class NapilePsiClassStub extends StubBase<NapileClass> implements NamedStub<NapileClass>
{
	private final FqName fqName;
	private final StringRef name;

	public NapilePsiClassStub(StubElement parent, @NotNull final FqName fqName, String name)
	{
		this(parent, fqName, StringRef.fromString(name));
	}

	public NapilePsiClassStub(StubElement parent, @NotNull FqName fqName, StringRef name)
	{
		super(parent, NapileStubElementTypes.CLASS);
		this.fqName = fqName;
		this.name = name;
	}

	@NotNull
	public FqName getFqName()
	{
		return fqName;
	}

	@Override
	public String getName()
	{
		return StringRef.toString(name);
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("NapilePsiClassStub[");

		builder.append("name=").append(getName());
		builder.append(" fqn=").append(getFqName());
		builder.append("]");

		return builder.toString();
	}

	public NapileDeclaration[] getDeclarations()
	{
		return getChildrenByType(NapileStubElementTypes.CLASS_MEMBERS, NapileDeclaration.ARRAY_FACTORY);
	}
}
