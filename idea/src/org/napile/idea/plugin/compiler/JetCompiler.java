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

package org.napile.idea.plugin.compiler;

import static com.intellij.openapi.compiler.CompilerMessageCategory.ERROR;
import static com.intellij.openapi.compiler.CompilerMessageCategory.INFORMATION;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.NapileFileType;
import org.napile.compiler.cli.common.CLICompiler;
import org.napile.compiler.cli.jvm.K2JVMCompiler;
import com.intellij.compiler.impl.javaCompiler.ModuleChunk;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.JdkUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SimpleJavaSdkType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Chunk;
import com.intellij.util.PathsList;
import com.intellij.util.SystemProperties;

/**
 * @author yole
 */
public class JetCompiler implements TranslatingCompiler
{

	private static final boolean RUN_OUT_OF_PROCESS = false;

	@Override
	public boolean isCompilableFile(VirtualFile virtualFile, CompileContext compileContext)
	{
		if(!(virtualFile.getFileType() instanceof NapileFileType))
		{
			return false;
		}
		return true;
	}

	@NotNull
	@Override
	public String getDescription()
	{
		return "Napile Compiler";
	}

	@Override
	public boolean validateConfiguration(CompileScope compileScope)
	{
		return true;
	}

	@Override
	public void compile(final CompileContext compileContext, Chunk<Module> moduleChunk, final VirtualFile[] virtualFiles, OutputSink outputSink)
	{
		if(virtualFiles.length == 0)
			return;

		List<VirtualFile> productionFiles = new ArrayList<VirtualFile>();
		List<VirtualFile> testFiles = new ArrayList<VirtualFile>();
		for(VirtualFile file : virtualFiles)
		{
			final boolean inTests = ((CompileContextEx) compileContext).isInTestSourceContent(file);
			if(inTests)
			{
				testFiles.add(file);
			}
			else
			{
				productionFiles.add(file);
			}
		}

		final Module module = compileContext.getModuleByFile(virtualFiles[0]);

		doCompile(compileContext, moduleChunk, productionFiles, module, outputSink, false);
		doCompile(compileContext, moduleChunk, testFiles, module, outputSink, true);
	}

	private void doCompile(CompileContext compileContext, Chunk<Module> moduleChunk, List<VirtualFile> files, Module module, OutputSink outputSink, boolean tests)
	{
		if(files.isEmpty())
			return;

		final VirtualFile outputDir = tests ? compileContext.getModuleOutputDirectoryForTests(module) : compileContext.getModuleOutputDirectory(module);
		if(outputDir == null)
		{
			compileContext.addMessage(ERROR, "[Internal Error] No output directory", "", -1, -1);
			return;
		}

		String[] arguments = commandLineArguments(outputDir, moduleChunk, (CompileContextEx) compileContext, files);

		runCompiler(compileContext, arguments);
	}

	private void runCompiler(CompileContext compileContext, String[] arguments)
	{
		if(RUN_OUT_OF_PROCESS)
			runOutOfProcess(compileContext, arguments);
		else
			runInProcess(compileContext,  arguments);
	}

	private static void runInProcess(final CompileContext compileContext, final String[] arguments)
	{
		CompilerUtils.outputCompilerMessagesAndHandleExitCode(compileContext, new Function1<PrintStream, Integer>()
		{
			@Override
			public Integer invoke(PrintStream stream)
			{
				return CLICompiler.doMainNoExit(stream, new K2JVMCompiler(), arguments).getCode();
			}
		});
	}

	@NotNull
	private static String[] commandLineArguments(VirtualFile outputDir, Chunk<Module> moduleChunk, CompileContextEx compileContext, List<VirtualFile> sources)
	{
		ModuleChunk chunk = new ModuleChunk(compileContext, moduleChunk, Collections.<Module, List<VirtualFile>>emptyMap());
		List<String> strings = new ArrayList<String>();
		strings.add("-output");
		strings.add(path(outputDir));
		strings.add("-classpath");
		strings.add(chunk.getCompilationClasspath());
		//strings.add("-verbose");
		strings.add("-tags");

		PathsList sourcesPath = new PathsList();
		sourcesPath.addVirtualFiles(sources);
		strings.add(sourcesPath.getPathsString());

		return ArrayUtil.toStringArray(strings);
	}

	private static void runOutOfProcess(final CompileContext compileContext, String[] arguments)
	{
		final SimpleJavaParameters params = new SimpleJavaParameters();
		params.setJdk(new SimpleJavaSdkType().createJdk("tmp", SystemProperties.getJavaHome()));
		params.setMainClass("org.napile.compiler.cli.jvm.K2JVMCompiler");

		for(String arg : arguments)
			params.getProgramParametersList().add(arg);

		params.getVMParametersList().addParametersString("-Djava.awt.headless=true -Xmx512m");
		//        params.getVMParametersList().addParametersString("-agentlib:yjpagent=sampling");

		Sdk sdk = params.getJdk();

		final GeneralCommandLine commandLine = JdkUtil.setupJVMCommandLine(((JavaSdkType) sdk.getSdkType()).getVMExecutablePath(sdk), params, false);

		compileContext.addMessage(INFORMATION, "Invoking out-of-process compiler with arguments: " + commandLine, "", -1, -1);

		try
		{
			final Process process = commandLine.createProcess();

			ApplicationManager.getApplication().executeOnPooledThread(new Runnable()
			{
				@Override
				public void run()
				{
					CompilerUtils.parseCompilerMessagesFromReader(compileContext, new InputStreamReader(process.getInputStream()));
				}
			});

			ApplicationManager.getApplication().executeOnPooledThread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						FileUtil.loadBytes(process.getErrorStream());
					}
					catch(IOException e)
					{
						// Don't care
					}
				}
			});

			int exitCode = process.waitFor();
			CompilerUtils.handleProcessTermination(exitCode, compileContext);
		}
		catch(Exception e)
		{
			compileContext.addMessage(ERROR, "[Internal Error] " + e.getLocalizedMessage(), "", -1, -1);
			return;
		}
	}

	private static String path(VirtualFile root)
	{
		String path = root.getPath();
		if(path.endsWith("!/"))
		{
			return path.substring(0, path.length() - 2);
		}

		return path;
	}
}
