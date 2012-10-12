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

package org.napile.compiler.lang.psi.stubs.elements;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.stubs.NapilePsiFileStub;
import org.napile.compiler.psi.NapileFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.stubs.DefaultStubBuilder;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;

/**
 * @author Nikolay Krasko
 */
public class NapileFileStubBuilder extends DefaultStubBuilder
{
	@Override
	protected StubElement createStubForFile(@NotNull PsiFile file)
	{
		if(!(file instanceof NapileFile))
			return super.createStubForFile(file);

		NapileFile jetFile = (NapileFile) file;
		return new NapilePsiFileStub(jetFile, StringRef.fromString(jetFile.getPackageName()));
	}
}
