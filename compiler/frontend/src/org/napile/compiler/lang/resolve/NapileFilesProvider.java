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
package org.napile.compiler.lang.resolve;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import com.google.common.base.Predicate;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;

public abstract class NapileFilesProvider
{
	public static NapileFilesProvider getInstance(Project project)
	{
		return ServiceManager.getService(project, NapileFilesProvider.class);
	}

	@NotNull
	public abstract Function<NapileFile, Collection<NapileFile>> sampleToAllFilesInModule();

	@NotNull
	public abstract List<NapileFile> allInScope(GlobalSearchScope scope);

	public static class SameJetFilePredicate implements Predicate<NapileFile>
	{
		private final FqName name;

		public SameJetFilePredicate(NapileFile file)
		{
			this.name = NapilePsiUtil.getFQName(file);
		}

		@Override
		public boolean apply(NapileFile psiFile)
		{
			return NapilePsiUtil.getFQName(psiFile).equals(name);
		}
	}
}
