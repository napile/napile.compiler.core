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

package org.napile.idea.plugin.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.LangVersion;
import org.napile.asm.io.xml.out.AsmXmlFileWriter;
import org.napile.asm.tree.members.ClassNode;
import org.napile.compiler.NapileFileType;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.analyzer.AnalyzerFacade;
import org.napile.compiler.codegen.CompilationErrorHandler;
import org.napile.compiler.codegen.GenerationState;
import org.napile.compiler.codegen.Progress;
import org.napile.compiler.common.messages.AnalyzerWithCompilerReport;
import org.napile.compiler.common.messages.CompilerMessageLocation;
import org.napile.compiler.common.messages.CompilerMessageSeverity;
import org.napile.compiler.common.messages.MessageCollector;
import org.napile.compiler.lang.psi.NapileFile;
import com.google.common.base.Predicates;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Chunk;
import com.intellij.util.Function;
import com.intellij.vcsUtil.VcsUtil;

/**
 * @author VISTALL
 */
public class NapileCompiler implements TranslatingCompiler
{
	@Override
	public boolean isCompilableFile(VirtualFile virtualFile, CompileContext compileContext)
	{
		if(!(virtualFile.getFileType() instanceof NapileFileType))
			return false;

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

		final Set<VirtualFile> productionFiles = new HashSet<VirtualFile>();
		final Set<VirtualFile> testFiles = new HashSet<VirtualFile>();
		for(VirtualFile file : virtualFiles)
		{
			final boolean inTests = ((CompileContextEx) compileContext).isInTestSourceContent(file);
			if(inTests)
				testFiles.add(file);
			else
				productionFiles.add(file);
		}

		final Module module = compileContext.getModuleByFile(virtualFiles[0]);
		collectDependFiles(module, productionFiles);
		collectDependFiles(module, testFiles);

		ApplicationManager.getApplication().runReadAction(new Runnable()
		{
			@Override
			public void run()
			{
				runInProcess(compileContext, productionFiles, compileContext.getModuleOutputDirectory(module));
				runInProcess(compileContext, testFiles, compileContext.getModuleOutputDirectoryForTests(module));
			}
		});
	}

	private static void collectDependFiles(Module module, final Set<VirtualFile> list)
	{
		final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
		moduleRootManager.getFileIndex().iterateContent(new ContentIterator()
		{
			@Override
			public boolean processFile(VirtualFile fileOrDir)
			{
				if(moduleRootManager.getFileIndex().isInSourceContent(fileOrDir) && !fileOrDir.isDirectory())
					list.add(fileOrDir);
				return true;
			}
		});

		for(Module depModule : moduleRootManager.getDependencies())
			collectDependFiles(depModule, list);
	}

	private static void runInProcess(final CompileContext compileContext, final Set<VirtualFile> virtualFiles, final VirtualFile outDir)
	{
		AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(new MessageCollector()
		{
			@Override
			public void report(@NotNull CompilerMessageSeverity severity, @NotNull String message, @NotNull CompilerMessageLocation location)
			{
				CompilerMessageCategory category = null;
				switch(severity)
				{
					case ERROR:
					case EXCEPTION:
						category = CompilerMessageCategory.ERROR;
						break;
					case WARNING:
						category = CompilerMessageCategory.WARNING;
						break;
					case INFO:
					case LOGGING:
						category = CompilerMessageCategory.INFORMATION;
						break;
				}
				VirtualFile virtualFile = VcsUtil.getVirtualFile(location.getPath());
				compileContext.addMessage(category, message, virtualFile == null ? null : virtualFile.getUrl(), location.getLine(), location.getColumn());
			}
		});

		final List<NapileFile> napileFiles = toNapileFiles(compileContext.getProject(), virtualFiles);

		analyzerWithCompilerReport.analyzeAndReport(new Function<Void, AnalyzeExhaust>()
		{
			@NotNull
			@Override
			public AnalyzeExhaust fun(Void v)
			{
				return AnalyzerFacade.analyzeFiles(compileContext.getProject(), napileFiles, Predicates.<NapileFile>alwaysTrue());
			}
		}, napileFiles);

		AnalyzeExhaust analyzeExhaust = analyzerWithCompilerReport.hasErrors() ? null : analyzerWithCompilerReport.getAnalyzeExhaust();
		if(analyzeExhaust == null)
			return;

		GenerationState generationState = new GenerationState(compileContext.getProject(), Progress.DEAF, analyzeExhaust, napileFiles);
		generationState.compileAndGenerate(CompilationErrorHandler.THROW_EXCEPTION);

		AsmXmlFileWriter writer = new AsmXmlFileWriter(new File(outDir.getPath()));
		for(ClassNode classNode : generationState.getClassNodes().values())
			writer.write(LangVersion.CURRENT, classNode);
	}

	@NotNull
	private static List<NapileFile> toNapileFiles(@NotNull final Project project, @NotNull final Set<VirtualFile> virtualFiles)
	{
		List<NapileFile> napileFiles = new ArrayList<NapileFile>(virtualFiles.size());
		for(VirtualFile virtualFile : virtualFiles)
		{
			PsiManager manager = PsiManager.getInstance(project);
			PsiFile file = manager.findFile(virtualFile);
			if(file instanceof NapileFile)
				napileFiles.add((NapileFile)file);
		}
		return napileFiles;
	}
}
