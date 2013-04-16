package org.napile.compiler.testFramework;

import java.util.ArrayList;
import java.util.List;

import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.psi.NapileFile;

/**
 * @author VISTALL
 * @since 14:43/14.04.13
 */
public class DiagnosticTest extends NapileMassTestCase
{
	@Override
	public String getExpectedText(NapileFile file) throws Exception
	{
		final List<Diagnostic> diagnostics = analyzeExhaust.getBindingTrace().getDiagnostics();

		List<Diagnostic> myDiagnostics = new ArrayList<Diagnostic>();
		for(Diagnostic diagnostic : diagnostics)
		{
			if(diagnostic.getPsiFile() == file)
			{
				myDiagnostics.add(diagnostic);
			}
		}

		return file.getText();
	}

	@Override
	public String getResultExt()
	{
		return "diagnostic";
	}
}