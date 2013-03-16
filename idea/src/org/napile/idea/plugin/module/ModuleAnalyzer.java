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

package org.napile.idea.plugin.module;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.idea.plugin.module.impl.DummyModuleAnalyzerImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 13:26/25.02.13
 */
public abstract class ModuleAnalyzer
{
	public static final AnalyzeExhaust EMPTY = AnalyzeExhaust.success(BindingContext.EMPTY, BodiesResolveContext.EMPTY, null);

	@NotNull
	public static ModuleAnalyzer getInstance(@NotNull Module module)
	{
		return ModuleServiceManager.getService(module, ModuleAnalyzer.class);
	}

	public static ModuleAnalyzer getInstance(@NotNull PsiElement element)
	{
		final Module module = ModuleUtilCore.findModuleForPsiElement(element);
		if(module == null)
		{
			return DummyModuleAnalyzerImpl.INSTANCE;
		}
		else
		{
			return getInstance(module);
		}
	}

	@NotNull
	public abstract AnalyzeExhaust getSourceAnalyze(boolean updateIfNeed);

	@NotNull
	public abstract AnalyzeExhaust getTestSourceAnalyze(boolean updateIfNeed);
}
