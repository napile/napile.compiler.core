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

/*
 * @author max
 */
package org.napile.compiler.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.resolve.NapileFilesProvider;
import org.napile.compiler.lang.psi.NapileFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;

public class CompilerFilesProvider extends NapileFilesProvider
{
	private final JetCoreEnvironment environment;
	private final Function<NapileFile, Collection<NapileFile>> files = new Function<NapileFile, Collection<NapileFile>>()
	{
		@Override
		public Collection<NapileFile> fun(NapileFile file)
		{
			return environment.getSourceFiles();
		}
	};

	public CompilerFilesProvider(JetCoreEnvironment environment)
	{
		this.environment = environment;
	}

	@NotNull
	@Override
	public Function<NapileFile, Collection<NapileFile>> sampleToAllFilesInModule()
	{
		return files;
	}

	@NotNull
	@Override
	public List<NapileFile> allInScope(GlobalSearchScope scope)
	{
		List<NapileFile> answer = new ArrayList<NapileFile>();
		for(NapileFile file : environment.getSourceFiles())
		{
			if(scope.contains(file.getVirtualFile()))
				answer.add(file);
		}
		return answer;
	}
}