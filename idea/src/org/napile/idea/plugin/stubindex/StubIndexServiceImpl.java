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

package org.napile.idea.plugin.stubindex;

import org.napile.compiler.lang.psi.stubs.NapilePsiClassStub;
import org.napile.compiler.lang.psi.stubs.NapilePsiMethodStub;
import org.napile.compiler.lang.psi.stubs.NapilePsiVariableStub;
import org.napile.compiler.lang.psi.stubs.elements.StubIndexService;
import com.intellij.psi.stubs.IndexSink;

/**
 * @author Nikolay Krasko
 */
public class StubIndexServiceImpl implements StubIndexService
{
	@Override
	public void indexClass(NapilePsiClassStub stub, IndexSink sink)
	{
		String name = stub.getName();
		if(name != null)
			sink.occurrence(JetIndexKeys.CLASSES_SHORT_NAME_KEY, name);

		String fqn = stub.getQualifiedName();
		if(fqn != null)
			sink.occurrence(JetIndexKeys.FQN_KEY, fqn);
	}

	@Override
	public void indexMethod(NapilePsiMethodStub stub, IndexSink sink)
	{
		String name = stub.getName();
		if(name != null)
			sink.occurrence(JetIndexKeys.METHODS_SHORT_NAME_KEY, name);
	}

	@Override
	public void indexVariable(NapilePsiVariableStub stub, IndexSink sink)
	{
		String name = stub.getName();
		if(name != null)
			sink.occurrence(JetIndexKeys.VARIABLES_SHORT_NAME_KEY, name);
	}
}
