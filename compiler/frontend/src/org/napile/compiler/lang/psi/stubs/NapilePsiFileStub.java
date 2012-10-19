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

import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import org.napile.compiler.lang.psi.NapileFile;
import com.intellij.psi.stubs.PsiFileStubImpl;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class NapilePsiFileStub extends PsiFileStubImpl<NapileFile>
{
	private final StringRef packageName;
	private final boolean compiled;
	private final NapilePsiFromStubFactory stubFactory;

	public NapilePsiFileStub(NapileFile jetFile, StringRef packageName, boolean compiled)
	{
		super(jetFile);
		this.packageName = packageName;
		this.compiled = compiled;
		this.stubFactory = compiled ? new NXmlPsiFromStubFactory() : new SourcePsiFromStubFactory();
	}

	public String getPackageName()
	{
		return StringRef.toString(packageName);
	}

	@Override
	public IStubFileElementType getType()
	{
		return NapileStubElementTypes.FILE;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();

		builder.append("NapilePsiFileStub[");


		builder.append("package=").append(getPackageName());
		builder.append("compiled=").append(isCompiled());
		builder.append("]");

		return builder.toString();
	}

	public boolean isCompiled()
	{
		return compiled;
	}

	public NapilePsiFromStubFactory getStubFactory()
	{
		return stubFactory;
	}
}
