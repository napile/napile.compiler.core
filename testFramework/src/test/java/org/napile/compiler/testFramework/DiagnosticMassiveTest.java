package org.napile.compiler.testFramework;

import java.util.ArrayList;
import java.util.List;

import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.psi.NapileFile;
import junit.framework.Test;

/**
 * @author VISTALL
 * @since 14:43/14.04.13
 */
public class DiagnosticMassiveTest extends AbstractMassiveTestSuite
{
	public static Test suite()
	{
		return new DiagnosticMassiveTest();
	}

	@Override
	public String getExpectedText(NapileFile file) throws Exception
	{
		final List<Diagnostic> diagnostics = analyzeExhaust.getBindingTrace().getDiagnostics();

		List<Diagnostic> myDiagnostics = new ArrayList<Diagnostic>();
		for(Diagnostic diagnostic : diagnostics)
		{
			if(diagnostic.getPsiFile() == file && diagnostic.isValid())
			{
				myDiagnostics.add(diagnostic);
			}
		}

		return new DiagnosticTextBuilder(myDiagnostics, file.getText()).getText();
	}

	@Override
	public String getResultExt()
	{
		return "diagnostic";
	}
}