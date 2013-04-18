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

import static org.napile.compiler.common.ExitCode.COMPILATION_ERROR;
import static org.napile.compiler.common.ExitCode.INTERNAL_ERROR;
import static org.napile.compiler.common.ExitCode.OK;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.Main;
import org.napile.compiler.codegen.CompilationException;
import org.napile.compiler.common.messages.CompilerMessageLocation;
import org.napile.compiler.common.messages.CompilerMessageSeverity;
import org.napile.compiler.common.messages.MessageRenderer;
import org.napile.compiler.common.messages.MessageUtil;
import org.napile.compiler.common.messages.PrintingMessageCollector;
import org.napile.compiler.config.CompilerConfiguration;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.sampullara.cli.Args;

/**
 * @author Pavel Talanov
 */
public class CompilerProcessor
{
	@NotNull
	public ExitCode exec(@NotNull PrintStream errStream, @NotNull String... args)
	{
		CompilerArguments arguments = new CompilerArguments();
		if(!parseArguments(errStream, arguments, args))
		{
			return INTERNAL_ERROR;
		}
		return exec(errStream, arguments);
	}

	/**
	 * Returns true if the arguments can be parsed correctly
	 */
	protected boolean parseArguments(@NotNull PrintStream errStream, @NotNull CompilerArguments arguments, @NotNull String[] args)
	{
		try
		{
			arguments.freeArgs = Args.parse(arguments, args);
			return true;
		}
		catch(IllegalArgumentException e)
		{
			errStream.println(e.getMessage());
			usage(errStream);
		}
		catch(Throwable t)
		{
			// Always use tags
			errStream.println(MessageRenderer.TAGS.renderException(t));
		}
		return false;
	}

	/**
	 * Allow derived classes to add additional command line arguments
	 */
	protected void usage(@NotNull PrintStream target)
	{
		// We should say something like

		// but currently cli-parser we are using does not support that
		// a corresponding patch has been sent to the authors
		// For now, we are using this:
		PrintStream oldErr = System.err;
		System.setErr(target);
		try
		{
			// TODO: use proper argv0
			Args.usage(new CompilerArguments());
		}
		finally
		{
			System.setErr(oldErr);
		}
	}

	/**
	 * Executes the compiler on the parsed arguments
	 */
	@NotNull
	public ExitCode exec(final PrintStream errStream, CompilerArguments arguments)
	{
		if(arguments.isHelp())
		{
			usage(errStream);
			return OK;
		}
		System.setProperty("java.awt.headless", "true");
		final MessageRenderer messageRenderer = getMessageRenderer(arguments);
		errStream.print(messageRenderer.renderPreamble());
		printVersionIfNeeded(errStream, arguments, messageRenderer);
		PrintingMessageCollector messageCollector = new PrintingMessageCollector(errStream, messageRenderer, arguments.isVerbose());
		Disposable rootDisposable = Disposer.newDisposable();
		try
		{
			return doExecute(arguments, messageCollector, rootDisposable);
		}
		finally
		{
			messageCollector.printToErrStream();
			errStream.print(messageRenderer.renderConclusion());
			Disposer.dispose(rootDisposable);
		}
	}

	@NotNull
	protected ExitCode doExecute(CompilerArguments arguments, PrintingMessageCollector messageCollector, Disposable rootDisposable)
	{
		CompilerConfiguration configuration = new CompilerConfiguration();
		configuration.addAll(CompilerConfigurationKeys.CLASSPATH_KEY, getClasspath(arguments));

		for(String freeArg : arguments.freeArgs)
		{
			if(freeArg.contains(File.pathSeparator))
			{
				List<String> sourcePathsSplitByPathSeparator = Arrays.asList(freeArg.split(StringUtil.escapeToRegexp(File.pathSeparator)));
				configuration.addAll(CompilerConfigurationKeys.SOURCE_ROOTS_KEY, sourcePathsSplitByPathSeparator);
			}
			else
				configuration.add(CompilerConfigurationKeys.SOURCE_ROOTS_KEY, freeArg);
		}

		configuration.put(CompilerConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector);

		messageCollector.report(CompilerMessageSeverity.LOGGING, "Configuring the compilation environment", CompilerMessageLocation.NO_LOCATION);
		try
		{
			File outputDir = arguments.outputDir != null ? new File(arguments.outputDir) : null;

			boolean noErrors;
			NapileCoreEnvironment environment = new NapileCoreEnvironment(rootDisposable, configuration);
			noErrors = AnalyzeProcessor.compileBunchOfSources(environment, outputDir);

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

	//TODO: can we make it private?
	@NotNull
	private MessageRenderer getMessageRenderer(@NotNull CompilerArguments arguments)
	{
		return arguments.isTags() ? MessageRenderer.TAGS : MessageRenderer.PLAIN;
	}

	protected void printVersionIfNeeded(@NotNull PrintStream errStream, @NotNull CompilerArguments arguments, @NotNull MessageRenderer messageRenderer)
	{
		if(arguments.isVersion())
		{
			String versionMessage = messageRenderer.render(CompilerMessageSeverity.INFO, "Kotlin Compiler version " + Main.VERSION, CompilerMessageLocation.NO_LOCATION);
			errStream.println(versionMessage);
		}
	}

	@NotNull
	private static List<File> getClasspath(@NotNull CompilerArguments arguments)
	{
		List<File> classpath = Lists.newArrayList();
		if(arguments.classpath != null)
		{
			for(String element : Splitter.on(File.pathSeparatorChar).split(arguments.classpath))
				classpath.add(new File(element));
		}
		return classpath;
	}
}
