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

package org.napile.compiler.common;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.LangVersion;
import org.napile.asm.io.xml.out.AsmXmlFileWriter;
import org.napile.asm.tree.members.ClassNode;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.analyzer.AnalyzerFacade;
import org.napile.compiler.codegen.CompilationErrorHandler;
import org.napile.compiler.codegen.GenerationState;
import org.napile.compiler.codegen.Progress;
import org.napile.compiler.common.messages.AnalyzerWithCompilerReport;
import org.napile.compiler.common.messages.CompilerMessageLocation;
import org.napile.compiler.common.messages.CompilerMessageSeverity;
import org.napile.compiler.lang.psi.NapileFile;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.util.Function;

/**
 * @author yole
 * @author abreslav
 * @author alex.tkachman
 */
public class AnalyzeProcessor
{

	private AnalyzeProcessor()
	{
	}


	public static boolean compileBunchOfSources(JetCoreEnvironment environment, @Nullable File outputDir)
	{
		GenerationState generationState = analyzeAndGenerate(environment);
		if(generationState == null)
			return false;

		if(outputDir != null)
		{
			AsmXmlFileWriter writer = new AsmXmlFileWriter(outputDir);
			for(ClassNode classNode : generationState.getClassNodes().values())
				writer.write(LangVersion.CURRENT, classNode);
		}
		else
			throw new CompileEnvironmentException("Output directory is not specified - no files will be saved to the disk");

		return true;
	}

	@Nullable
	public static GenerationState analyzeAndGenerate(JetCoreEnvironment environment)
	{
		AnalyzeExhaust exhaust = analyze(environment);

		if(exhaust == null)
			return null;

		exhaust.throwIfError();

		return generate(environment, exhaust);
	}

	@Nullable
	public static AnalyzeExhaust analyze(final JetCoreEnvironment environment)
	{
		AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(environment.getConfiguration().get(CompilerConfigurationKeys.MESSAGE_COLLECTOR_KEY));
		final Predicate<NapileFile> filesToAnalyzeCompletely = Predicates.<NapileFile>alwaysTrue();
		analyzerWithCompilerReport.analyzeAndReport(new Function<Void, AnalyzeExhaust>()
		{
			@NotNull
			@Override
			public AnalyzeExhaust fun(Void v)
			{
				return AnalyzerFacade.analyzeFiles(environment.getProject(), environment.getSourceFiles(), filesToAnalyzeCompletely);
			}
		}, environment.getSourceFiles());

		return analyzerWithCompilerReport.hasErrors() ? null : analyzerWithCompilerReport.getAnalyzeExhaust();
	}

	@NotNull
	private static GenerationState generate(final JetCoreEnvironment environment, AnalyzeExhaust exhaust)
	{
		Project project = environment.getProject();
		Progress backendProgress = new Progress()
		{
			@Override
			public void log(String message)
			{
				environment.getConfiguration().get(CompilerConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(CompilerMessageSeverity.LOGGING, message, CompilerMessageLocation.NO_LOCATION);
			}
		};
		GenerationState generationState = new GenerationState(project, backendProgress, exhaust, environment.getSourceFiles());
		generationState.compileAndGenerate(CompilationErrorHandler.THROW_EXCEPTION);

		return generationState;
	}
}
