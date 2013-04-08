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

import org.napile.compiler.lang.psi.NapileCallParameterAsVariable;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.psi.stubs.NamedStub;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class NapilePsiCallParameterAsVariableStub extends StubBase<NapileCallParameterAsVariable> implements NamedStub<NapileCallParameterAsVariable>
{
	private final StringRef name;

	public NapilePsiCallParameterAsVariableStub(StubElement parent, StringRef name)
	{
		super(parent, NapileStubElementTypes.CALL_PARAMETER_AS_VARIABLE);
		this.name = name;
	}

	public NapilePsiCallParameterAsVariableStub(StubElement parent, String name)
	{
		this(parent, StringRef.fromString(name));
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
		builder.append("NapilePsiMethodParameterStub[");

		builder.append("var ");

		builder.append("name=").append(getName());
		builder.append("]");

		return builder.toString();
	}
}
