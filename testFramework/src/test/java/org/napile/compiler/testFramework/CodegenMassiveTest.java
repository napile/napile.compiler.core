package org.napile.compiler.testFramework;

import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.codegen.processors.BindingTraceKeys2;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileFile;
import junit.framework.Test;

/**
 * @author VISTALL
 * @since 08.10.13.
 */
public class CodegenMassiveTest extends AbstractMassiveTestSuite
{
	private TestGenerationState generationState;

	public static Test suite()
	{
		return new CodegenMassiveTest();
	}

	@Override
	public void setUp() throws Exception
	{
		generationState = new TestGenerationState(analyzeExhaust, environment.getSourceFiles());
		generationState.compileAndGenerate();
	}

	@Override
	public String getExpectedText(NapileFile file) throws Exception
	{
		StringBuilder builder = new StringBuilder();

		final NapileClass[] declarations = file.getDeclarations();
		for(int i = 0; i < declarations.length; i++)
		{
			NapileClass napileClass = declarations[i];

			final FqName fqName = analyzeExhaust.getBindingTrace().safeGet(BindingTraceKeys2.DECLARATION_TO_FQ_NAME, napileClass);

			builder.append(generationState.getClassNodes().get(fqName));

			if(i != 0)
			{
				builder.append("\n");
			}
		}

		return builder.toString();
	}

	@Override
	public String getResultExt()
	{
		return "codegen";
	}
}
