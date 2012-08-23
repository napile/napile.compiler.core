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

package org.napile.compiler.analyzer;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.di.InjectorForBodyResolve;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.AnalyzerScriptParameter;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.lang.resolve.ObservableBindingTrace;
import org.napile.compiler.lang.resolve.TopDownAnalysisParameters;
import com.google.common.base.Predicate;
import com.intellij.openapi.project.Project;

/**
 * @author Stepan Koltsov
 */
public class AnalyzerFacadeForEverything
{

	private AnalyzerFacadeForEverything()
	{
	}

	public static AnalyzeExhaust analyzeBodiesInFilesWithJavaIntegration(Project project, List<AnalyzerScriptParameter> scriptParameters, Predicate<NapileFile> filesToAnalyzeCompletely, @NotNull BindingTrace traceContext, @NotNull BodiesResolveContext bodiesResolveContext)
	{

		TopDownAnalysisParameters topDownAnalysisParameters = new TopDownAnalysisParameters(filesToAnalyzeCompletely, false, false, scriptParameters);

		bodiesResolveContext.setTopDownAnalysisParameters(topDownAnalysisParameters);

		InjectorForBodyResolve injector = new InjectorForBodyResolve(project, topDownAnalysisParameters, new ObservableBindingTrace(traceContext));

		try
		{
			injector.getBodyResolver().resolveBodies(bodiesResolveContext);
			return AnalyzeExhaust.success(traceContext.getBindingContext());
		}
		finally
		{
			injector.destroy();
		}
	}
}
