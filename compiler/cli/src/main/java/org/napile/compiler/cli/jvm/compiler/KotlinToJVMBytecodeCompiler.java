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

package org.napile.compiler.cli.jvm.compiler;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.LinkedList;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.utils.ExceptionUtils;
import org.jetbrains.jet.utils.Progress;
import org.napile.asm.LangVersion;
import org.napile.asm.io.xml.out.AsmXmlFileWriter;
import org.napile.asm.tree.members.ClassNode;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.analyzer.AnalyzerFacade;
import org.napile.compiler.cli.common.CLIConfigurationKeys;
import org.napile.compiler.cli.common.CompilerPlugin;
import org.napile.compiler.cli.common.CompilerPluginContext;
import org.napile.compiler.cli.common.messages.AnalyzerWithCompilerReport;
import org.napile.compiler.cli.common.messages.CompilerMessageLocation;
import org.napile.compiler.cli.common.messages.CompilerMessageSeverity;
import org.napile.compiler.cli.jvm.JVMConfigurationKeys;
import org.napile.compiler.codegen.CompilationErrorHandler;
import org.napile.compiler.codegen.GenerationState;
import org.napile.compiler.psi.NapileFile;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.util.Function;

/**
 * @author yole
 * @author abreslav
 * @author alex.tkachman
 */
public class KotlinToJVMBytecodeCompiler
{

	private KotlinToJVMBytecodeCompiler()
	{
	}


	public static boolean compileBunchOfSources(JetCoreEnvironment environment, @Nullable File outputDir)
	{
		GenerationState generationState = analyzeAndGenerate(environment);
		if(generationState == null)
			return false;

		try
		{
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
		finally
		{
			generationState.destroy();
		}
	}

	@Nullable
	public static GenerationState analyzeAndGenerate(JetCoreEnvironment environment)
	{
		return analyzeAndGenerate(environment, environment.getConfiguration().get(JVMConfigurationKeys.STUBS, false));
	}

	@Nullable
	public static GenerationState analyzeAndGenerate(JetCoreEnvironment environment, boolean stubs)
	{
		AnalyzeExhaust exhaust = analyze(environment, stubs);

		if(exhaust == null)
		{
			return null;
		}

		exhaust.throwIfError();

		return generate(environment, exhaust, stubs);
	}

	@Nullable
	private static AnalyzeExhaust analyze(final JetCoreEnvironment environment, boolean stubs)
	{
		AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(environment.getConfiguration().get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY));
		final Predicate<NapileFile> filesToAnalyzeCompletely = stubs ? Predicates.<NapileFile>alwaysFalse() : Predicates.<NapileFile>alwaysTrue();
		analyzerWithCompilerReport.analyzeAndReport(new Function<Void, AnalyzeExhaust>()
		{
			@NotNull
			@Override
			public AnalyzeExhaust fun(Void v)
			{
				return AnalyzerFacade.analyzeFilesWithJavaIntegration(environment.getProject(), environment.getSourceFiles(), filesToAnalyzeCompletely, true);
			}
		}, environment.getSourceFiles());

		return analyzerWithCompilerReport.hasErrors() ? null : analyzerWithCompilerReport.getAnalyzeExhaust();
	}

	@NotNull
	private static GenerationState generate(final JetCoreEnvironment environment, AnalyzeExhaust exhaust, boolean stubs)
	{
		Project project = environment.getProject();
		Progress backendProgress = new Progress()
		{
			@Override
			public void log(String message)
			{
				environment.getConfiguration().get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(CompilerMessageSeverity.LOGGING, message, CompilerMessageLocation.NO_LOCATION);
			}
		};
		GenerationState generationState = new GenerationState(project, backendProgress, exhaust, environment.getSourceFiles());
		generationState.compileAndGenerate(CompilationErrorHandler.THROW_EXCEPTION);

		CompilerPluginContext context = new CompilerPluginContext(project, exhaust.getBindingContext(), environment.getSourceFiles());
		for(CompilerPlugin plugin : environment.getConfiguration().getList(CLIConfigurationKeys.COMPILER_PLUGINS))
		{
			plugin.processFiles(context);
		}
		return generationState;
	}

	private static Collection<File> getClasspath(ClassLoader loader)
	{
		return getClasspath(loader, new LinkedList<File>());
	}

	private static Collection<File> getClasspath(ClassLoader loader, LinkedList<File> files)
	{
		ClassLoader parent = loader.getParent();
		if(parent != null)
			getClasspath(parent, files);

		if(loader instanceof URLClassLoader)
		{
			for(URL url : ((URLClassLoader) loader).getURLs())
			{
				String urlFile = url.getFile();

				if(urlFile.contains("%"))
				{
					try
					{
						urlFile = url.toURI().getPath();
					}
					catch(URISyntaxException e)
					{
						throw ExceptionUtils.rethrow(e);
					}
				}

				File file = new File(urlFile);
				if(file.exists() && (file.isDirectory() || file.getName().endsWith(".jar")))
				{
					files.add(file);
				}
			}
		}
		return files;
	}
}
