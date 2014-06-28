package org.napile.compiler.testFramework;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.analyzer.AnalyzerFacade;
import org.napile.compiler.common.NapileCoreEnvironment;
import org.napile.compiler.common.messages.AnalyzerWithCompilerReport;
import org.napile.compiler.common.messages.CompilerMessageLocation;
import org.napile.compiler.common.messages.CompilerMessageSeverity;
import org.napile.compiler.common.messages.MessageCollector;
import org.napile.compiler.config.CompilerConfiguration;
import org.napile.compiler.lang.psi.NapileFile;
import com.google.common.base.Predicates;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Function;
import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author VISTALL
 * @since 08.10.13.
 */
public abstract class AbstractMassiveTestSuite extends TestSuite
{
	protected NapileCoreEnvironment environment;
	protected AnalyzeExhaust analyzeExhaust;

	protected AbstractMassiveTestSuite()
	{
		CompilerConfiguration configuration = new CompilerConfiguration();

		environment = new NapileCoreEnvironment(Disposer.newDisposable(), configuration);
		File testDir = new File("testFramework/src/test/napileRt");
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

		try
		{
			setUp();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		for(final NapileFile file : environment.getSourceFiles())
		{
			final String relativePath = FileUtil.getRelativePath(testDir, new File(file.getVirtualFile().getPath()));
			addTest(new TestCase()
			{

				@Override
				public String getName()
				{
					return relativePath;
				}

				@Override
				public void runTest() throws Throwable
				{
					assertTest(getExpectedText(file), file.getVirtualFile().getPath(), getResultExt());
				}
			});
		}
	}


	protected void setUp() throws Exception
	{

	}

	protected void assertTest(String orig, String path, String ext) throws Exception
	{
		File file = getResultFile(path, ext);
		if(!file.exists())
		{
			FileUtil.writeToFile(file, orig);
		}
		else
		{
			Assert.assertEquals("Failed to test: " + path, getResultData(path, ext), orig);
		}
	}

	public abstract String getExpectedText(NapileFile file) throws Exception;

	public abstract String getResultExt();

	protected File getResultFile(String path, String ext)
	{
		return new File(path + "." + ext);
	}

	protected String getResultData(String path, String ext) throws Exception
	{
		File file = getResultFile(path, ext);
		if(!file.exists())
		{
			return null;
		}
		return FileUtil.loadFile(file, true);
	}
}
