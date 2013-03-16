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
import org.napile.idea.plugin.module.ModuleAnalyzer;

/**
 * @author VISTALL
 * @since 13:41/25.02.13
 */
public class DummyModuleAnalyzerImpl extends ModuleAnalyzer
{
	public static final DummyModuleAnalyzerImpl INSTANCE = new DummyModuleAnalyzerImpl();

	@NotNull
	@Override
	public AnalyzeExhaust getSourceAnalyze(boolean updateIfNeed)
	{
		return ModuleAnalyzer.EMPTY;
	}

	@NotNull
	@Override
	public AnalyzeExhaust getTestSourceAnalyze(boolean updateIfNeed)
	{
		return ModuleAnalyzer.EMPTY;
	}
}
