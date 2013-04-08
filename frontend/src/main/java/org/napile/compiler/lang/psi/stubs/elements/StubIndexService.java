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

import org.napile.compiler.lang.psi.stubs.NapilePsiClassStub;
import org.napile.compiler.lang.psi.stubs.NapilePsiEnumValueStub;
import org.napile.compiler.lang.psi.stubs.NapilePsiMacroStub;
import org.napile.compiler.lang.psi.stubs.NapilePsiMethodStub;
import org.napile.compiler.lang.psi.stubs.NapilePsiVariableStub;
import com.intellij.psi.stubs.IndexSink;

/**
 * @author Nikolay Krasko
 */
public interface StubIndexService
{

	/**
	 * Default implementation with no indexing.
	 */
	StubIndexService NO_INDEX_SERVICE = new StubIndexService()
	{
		@Override
		public void indexClass(NapilePsiClassStub stub, IndexSink sink)
		{
		}

		@Override
		public void indexMethod(NapilePsiMethodStub stub, IndexSink sink)
		{
		}

		@Override
		public void indexMacro(NapilePsiMacroStub stub, IndexSink sink)
		{
		}

		@Override
		public void indexVariable(NapilePsiVariableStub stub, IndexSink sink)
		{
		}

		@Override
		public void indexEnumValue(NapilePsiEnumValueStub stub, IndexSink sink)
		{
		}
	};

	void indexClass(NapilePsiClassStub stub, IndexSink sink);

	void indexMethod(NapilePsiMethodStub stub, IndexSink sink);

	void indexMacro(NapilePsiMacroStub stub, IndexSink sink);

	void indexVariable(NapilePsiVariableStub stub, IndexSink sink);

	void indexEnumValue(NapilePsiEnumValueStub stub, IndexSink sink);
}
