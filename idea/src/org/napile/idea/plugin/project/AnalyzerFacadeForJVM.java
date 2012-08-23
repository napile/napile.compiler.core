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

package org.napile.idea.plugin.project;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.analyzer.AnalyzerFacade;
import org.napile.compiler.analyzer.AnalyzerFacadeForEverything;
import org.napile.compiler.di.InjectorForTopDownAnalyzerBasic;
import org.napile.compiler.lang.descriptors.ModuleDescriptor;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.AnalyzerScriptParameter;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BindingTraceContext;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.lang.resolve.CachedBodiesResolveContext;
import org.napile.compiler.lang.resolve.ObservableBindingTrace;
import org.napile.compiler.lang.resolve.TopDownAnalysisParameters;
import com.google.common.base.Predicate;
import com.intellij.openapi.project.Project;

/**
 * @author abreslav
 */
public enum AnalyzerFacadeForJVM implements AnalyzerFacade
{

	INSTANCE;

	private AnalyzerFacadeForJVM()
	{
	}

	@Override
	@NotNull
	public AnalyzeExhaust analyzeFiles(@NotNull Project project, @NotNull Collection<NapileFile> files, @NotNull List<AnalyzerScriptParameter> scriptParameters, @NotNull Predicate<NapileFile> filesToAnalyzeCompletely)
	{
		return analyzeFilesWithJavaIntegration(project, files, scriptParameters, filesToAnalyzeCompletely, true);
	}

	@NotNull
	@Override
	public AnalyzeExhaust analyzeBodiesInFiles(@NotNull Project project, @NotNull List<AnalyzerScriptParameter> scriptParameters, @NotNull Predicate<NapileFile> filesForBodiesResolve, @NotNull BindingTrace headersTraceContext, @NotNull BodiesResolveContext bodiesResolveContext)
	{
		return AnalyzerFacadeForEverything.analyzeBodiesInFilesWithJavaIntegration(project, scriptParameters, filesForBodiesResolve, headersTraceContext, bodiesResolveContext);
	}

	public static AnalyzeExhaust analyzeFilesWithJavaIntegration(Project project, Collection<NapileFile> files, List<AnalyzerScriptParameter> scriptParameters, Predicate<NapileFile> filesToAnalyzeCompletely, boolean storeContextForBodiesResolve)
	{
		BindingTraceContext bindingTraceContext = new BindingTraceContext();

		final ModuleDescriptor owner = new ModuleDescriptor(Name.special("<module>"));

		TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(filesToAnalyzeCompletely, false, false, scriptParameters);

		InjectorForTopDownAnalyzerBasic injector = new InjectorForTopDownAnalyzerBasic(project, topDownAnalysisParameters, new ObservableBindingTrace(bindingTraceContext), owner);
		try
		{
			injector.getTopDownAnalyzer().analyzeFiles(files, scriptParameters);
			BodiesResolveContext bodiesResolveContext = storeContextForBodiesResolve ? new CachedBodiesResolveContext(injector.getTopDownAnalysisContext()) : null;
			return AnalyzeExhaust.success(bindingTraceContext.getBindingContext(), bodiesResolveContext);
		}
		finally
		{
			injector.destroy();
		}
	}
}
