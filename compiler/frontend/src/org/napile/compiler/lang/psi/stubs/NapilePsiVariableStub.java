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

import org.napile.compiler.lang.psi.NapileVariable;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.NamedStub;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class NapilePsiVariableStub extends StubBase<NapileVariable> implements NamedStub<NapileVariable>
{
	private final StringRef name;

	private final StringRef typeText;
	private final StringRef inferenceBodyText;

	public NapilePsiVariableStub(IStubElementType elementType, StubElement parent, StringRef name, StringRef typeText, StringRef inferenceBodyText)
	{
		super(parent, elementType);

		this.name = name;
		this.typeText = typeText;
		this.inferenceBodyText = inferenceBodyText;
	}

	public NapilePsiVariableStub(IStubElementType elementType, StubElement parent, String name, String typeText, String inferenceBodyText)
	{
		this(elementType, parent, StringRef.fromString(name), StringRef.fromString(typeText), StringRef.fromString(inferenceBodyText));
	}

	public String getTypeText()
	{
		return StringRef.toString(typeText);
	}

	public String getInferenceBodyText()
	{
		return StringRef.toString(inferenceBodyText);
	}

	public String getName()
	{
		return StringRef.toString(name);
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();

		builder.append("NapilePsiVariableStub[");
		builder.append("name=").append(getName());
		builder.append(" typeText=").append(getTypeText());
		builder.append(" bodyText=").append(getInferenceBodyText());

		builder.append("]");

		return builder.toString();
	}
}
