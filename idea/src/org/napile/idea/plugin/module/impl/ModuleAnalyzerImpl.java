/*
 * Copyright 2010-2013 napile.org
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

package org.napile.idea.plugin.module.impl;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.analyzer.AnalyzerFacade;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.idea.plugin.module.ModuleAnalyzer;
import org.napile.idea.plugin.module.ModuleCollector;
import com.google.common.base.Predicates;
import com.intellij.openapi.module.Module;
import com.intellij.psi.util.PsiModificationTracker;

/**
 * @author VISTALL
 * @date 13:27/25.02.13
 */
public class ModuleAnalyzerImpl extends ModuleAnalyzer
{
	private final ModuleAnalyzerHolder[] holders = new ModuleAnalyzerHolder[]
	{
			new ModuleAnalyzerHolder(-1, EMPTY),
			new ModuleAnalyzerHolder(-1, EMPTY)
	};

	private final PsiModificationTracker psiModificationTracker;
	private final Module module;

	public ModuleAnalyzerImpl(Module module)
	{
		this.module = module;
		psiModificationTracker = PsiModificationTracker.SERVICE.getInstance(module.getProject());
	}

	@NotNull
	@Override
	public AnalyzeExhaust getSourceAnalyze(boolean updateIfNeed)
	{
		return getOrUpdate(false, updateIfNeed);
	}

	@NotNull
	@Override
	public AnalyzeExhaust getTestSourceAnalyze(boolean updateIfNeed)
	{
		return getOrUpdate(true, updateIfNeed);
	}

	private AnalyzeExhaust getOrUpdate(boolean test, boolean updateIfNeed)
	{
		ModuleAnalyzerHolder analyzerHolder = holders[test ? 1 : 0];
		if(!updateIfNeed)
			return analyzerHolder.getAnalyzeExhaust();
		else
		{
			synchronized(analyzerHolder)
			{
				final long modificationCount = psiModificationTracker.getModificationCount();
				if(analyzerHolder.getModificationCount() != modificationCount)
				{
					analyzerHolder.setAnalyzeExhaust(AnalyzerFacade.analyzeFiles(module.getProject(), ModuleCollector.getAnalyzeContext(module.getProject(), null, test, module), Predicates.<NapileFile>alwaysTrue()));
					analyzerHolder.setModificationCount(modificationCount);
				}
			}
		}
		return analyzerHolder.getAnalyzeExhaust();
	}
}
