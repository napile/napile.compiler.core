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

/**
 * @author VISTALL
 * @date 13:27/25.02.13
 */
public class ModuleAnalyzerImpl extends ModuleAnalyzer
{
	private volatile AnalyzeExhaust srcAnalyzeExhaust;
	private volatile AnalyzeExhaust testAnalyzeExhaust;

	private final Module module;

	public ModuleAnalyzerImpl(Module module)
	{
		this.module = module;
	}

	@NotNull
	@Override
	public AnalyzeExhaust getSourceAnalyze(boolean updateIfNeed)
	{
		srcAnalyzeExhaust = getOrUpdate(false, updateIfNeed, srcAnalyzeExhaust);
		return srcAnalyzeExhaust;
	}

	@NotNull
	@Override
	public AnalyzeExhaust getTestSourceAnalyze(boolean updateIfNeed)
	{
		testAnalyzeExhaust = getOrUpdate(true, updateIfNeed, testAnalyzeExhaust);
		return testAnalyzeExhaust;
	}

	private AnalyzeExhaust getOrUpdate(boolean test, boolean needUpdate, AnalyzeExhaust old)
	{
		if(old == null || needUpdate)
		{
			return AnalyzerFacade.analyzeFiles(module.getProject(), ModuleCollector.getAnalyzeContext(module.getProject(), null, test, module), Predicates.<NapileFile>alwaysTrue());
		}
		else
		{
			return old;
		}
	}
}
