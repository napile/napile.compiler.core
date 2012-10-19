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

import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NapilePropertyParameter;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.psi.stubs.NamedStub;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class NapilePsiMethodParameterStub extends StubBase<NapilePropertyParameter> implements NamedStub<NapilePropertyParameter>
{
	private final StringRef name;
	private final boolean isVarArg;
	private final StringRef typeText;
	private final StringRef defaultValueText;

	public NapilePsiMethodParameterStub(StubElement parent, StringRef name, boolean isVarArg, StringRef typeText, StringRef defaultValueText)
	{
		super(parent, NapileStubElementTypes.VALUE_PARAMETER);
		this.name = name;
		this.isVarArg = isVarArg;
		this.typeText = typeText;
		this.defaultValueText = defaultValueText;
	}

	public NapilePsiMethodParameterStub(StubElement parent, String name, boolean isVarArg, String typeText, String defaultValueText)
	{
		this(parent, StringRef.fromString(name), isVarArg, StringRef.fromString(typeText), StringRef.fromString(defaultValueText));
	}

	@Override
	public String getName()
	{
		return StringRef.toString(name);
	}

	public boolean isVarArg()
	{
		return isVarArg;
	}

	@Nullable
	public String getTypeText()
	{
		return StringRef.toString(typeText);
	}

	public String getDefaultValueText()
	{
		return StringRef.toString(defaultValueText);
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("NapilePsiMethodParameterStub[");

		builder.append("var ");

		if(isVarArg())
		{
			builder.append("vararg ");
		}

		builder.append("name=").append(getName());
		builder.append(" typeText=").append(getTypeText());
		builder.append(" defaultValue=").append(getDefaultValueText());

		builder.append("]");

		return builder.toString();
	}
}
