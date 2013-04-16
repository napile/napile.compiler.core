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

package org.napile.compiler.testFramework;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.analyzer.AnalyzerFacade;
import org.napile.compiler.common.JetCoreEnvironment;
import org.napile.compiler.common.messages.AnalyzerWithCompilerReport;
import org.napile.compiler.common.messages.CompilerMessageLocation;
import org.napile.compiler.common.messages.CompilerMessageSeverity;
import org.napile.compiler.common.messages.MessageCollector;
import org.napile.compiler.config.CompilerConfiguration;
import org.napile.compiler.lang.psi.NapileFile;
import com.google.common.base.Predicates;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Function;
import junit.framework.TestCase;

/**
 * @author VISTALL
 * @since 14:45/14.04.13
 */
public abstract class NapileTestCase extends TestCase
{
	private final File testDir = new File("testFramework/src/test/napileRt");
	protected JetCoreEnvironment environment;
	protected boolean genResults;
	protected AnalyzeExhaust analyzeExhaust;

	public NapileTestCase()
	{
		genResults
	}

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();

		CompilerConfiguration configuration = new CompilerConfiguration();

		environment = new JetCoreEnvironment(Disposer.newDisposable(), configuration);
		environment.addSources(testDir);

		AnalyzerWithCompilerReport analyzerWithCompilerReport = new AnalyzerWithCompilerReport(new MessageCollector()
		{
			@Override
			public void report(@NotNull CompilerMessageSeverity severity, @NotNull String message, @NotNull CompilerMessageLocation location)
			{
			}
		});

		analyzerWithCompilerReport.analyzeAndReport(new Function<Void, AnalyzeExhaust>()
		{
			@NotNull
			@Override
			public AnalyzeExhaust fun(Void v)
			{
				return AnalyzerFacade.analyzeFiles(environment.getProject(), environment.makeAnalyzeContext(), Predicates.<NapileFile>alwaysTrue());
			}
		}, environment.getSourceFiles());

		analyzeExhaust = analyzerWithCompilerReport.getAnalyzeExhaust();
	}

	@Nullable
	protected NapileFile getPsiFile(String path) throws Exception
	{
		VirtualFile fileByPath = environment.getApplicationEnvironment().getLocalFileSystem().findFileByPath(new File(testDir, path + ".ns").getAbsolutePath());
		if(fileByPath == null)
		{
			return null;
		}

		return (NapileFile) PsiManager.getInstance(environment.getProject()).findFile(fileByPath);
	}

	@NotNull
	protected NapileFile getPsiFile() throws Exception
	{
		final NapileFile psiFile = getPsiFile(getTestFileName());
		if(psiFile == null)
		{
			throw new IllegalArgumentException("Psi is null");
		}
		return psiFile;
	}

	@NotNull
	protected String getTestFileName()
	{
		String testName = getName();
		testName = testName.substring(4, testName.length()).replace("$", "/");
		return testName;
	}

	protected File getResultFile(String path, String ext)
	{
		return new File(testDir, path + "." + ext);
	}

	protected String getResultData(String path, String ext) throws Exception
	{
		File file = getResultFile(path, ext);
		if(!file.exists())
		{
			return null;
		}
		return FileUtil.loadFile(file);
	}

	protected void assertTest(String orig, String path, String ext) throws Exception
	{
		if(genResults)
		{
			File file = getResultFile(path, ext);
			if(file.exists())
			{
				file.delete();
			}

			FileUtil.writeToFile(file, orig);
		}
		else
		{
			assertEquals("Failed to test: " + path, getResultData(path, ext), orig);
		}
	}
}
