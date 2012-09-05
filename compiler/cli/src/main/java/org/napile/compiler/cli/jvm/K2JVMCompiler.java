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

package org.napile.compiler.cli.jvm;

import static org.napile.compiler.cli.common.ExitCode.COMPILATION_ERROR;
import static org.napile.compiler.cli.common.ExitCode.INTERNAL_ERROR;
import static org.napile.compiler.cli.common.ExitCode.OK;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.cli.common.CLICompiler;
import org.napile.compiler.cli.common.CLIConfigurationKeys;
import org.napile.compiler.cli.common.ExitCode;
import org.napile.compiler.cli.common.messages.CompilerMessageLocation;
import org.napile.compiler.cli.common.messages.CompilerMessageSeverity;
import org.napile.compiler.cli.common.messages.MessageRenderer;
import org.napile.compiler.cli.common.messages.MessageUtil;
import org.napile.compiler.cli.common.messages.PrintingMessageCollector;
import org.napile.compiler.cli.jvm.compiler.JetCoreEnvironment;
import org.napile.compiler.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
import org.napile.compiler.codegen.CompilationException;
import org.napile.compiler.config.CommonConfigurationKeys;
import org.napile.compiler.config.CompilerConfiguration;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.text.StringUtil;

/**
 * @author yole
 * @author alex.tkachman
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class K2JVMCompiler extends CLICompiler<K2JVMCompilerArguments>
{

	public static void main(String... args)
	{
		doMain(new K2JVMCompiler(), args);
	}

	@Override
	@NotNull
	protected ExitCode doExecute(K2JVMCompilerArguments arguments, PrintingMessageCollector messageCollector, Disposable rootDisposable)
	{
		CompilerConfiguration configuration = new CompilerConfiguration();
		configuration.addAll(JVMConfigurationKeys.CLASSPATH_KEY, getClasspath(arguments));

		for(String freeArg : arguments.freeArgs)
		{
			if(freeArg.contains(File.pathSeparator))
			{
				List<String> sourcePathsSplitByPathSeparator = Arrays.asList(freeArg.split(StringUtil.escapeToRegexp(File.pathSeparator)));
				configuration.addAll(CommonConfigurationKeys.SOURCE_ROOTS_KEY, sourcePathsSplitByPathSeparator);
			}
			else
				configuration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, freeArg);
		}

		boolean builtins = arguments.builtins;

		configuration.put(JVMConfigurationKeys.STUBS, builtins);

		configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector);

		messageCollector.report(CompilerMessageSeverity.LOGGING, "Configuring the compilation environment", CompilerMessageLocation.NO_LOCATION);
		try
		{
			configureEnvironment(configuration, arguments);

			File outputDir = arguments.outputDir != null ? new File(arguments.outputDir) : null;

			boolean noErrors;
			JetCoreEnvironment environment = new JetCoreEnvironment(rootDisposable, configuration);
			noErrors = KotlinToJVMBytecodeCompiler.compileBunchOfSources(environment, outputDir);

			return noErrors ? OK : COMPILATION_ERROR;
		}
		catch(CompilationException e)
		{
			messageCollector.report(CompilerMessageSeverity.EXCEPTION, MessageRenderer.PLAIN.renderException(e), MessageUtil.psiElementToMessageLocation(e.getElement()));
			return INTERNAL_ERROR;
		}
		catch(Throwable t)
		{
			messageCollector.report(CompilerMessageSeverity.EXCEPTION, MessageRenderer.PLAIN.renderException(t), CompilerMessageLocation.NO_LOCATION);
			return INTERNAL_ERROR;
		}
	}


	/**
	 * Allow derived classes to add additional command line arguments
	 */
	@NotNull
	@Override
	protected K2JVMCompilerArguments createArguments()
	{
		return new K2JVMCompilerArguments();
	}

	@NotNull
	private static List<File> getClasspath(@NotNull K2JVMCompilerArguments arguments)
	{
		List<File> classpath = Lists.newArrayList();
		if(arguments.classpath != null)
		{
			for(String element : Splitter.on(File.pathSeparatorChar).split(arguments.classpath))
			{
				classpath.add(new File(element));
			}
		}
		return classpath;
	}
}
