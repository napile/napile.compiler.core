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

import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.codegen.CompilationErrorHandler;
import org.napile.compiler.codegen.GenerationState;
import org.napile.compiler.codegen.Progress;
import org.napile.compiler.codegen.processors.BindingTraceKeys2;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileFile;

/**
 * @author VISTALL
 * @since 16:40/16.04.13
 */
public class CodegenTest extends NapileMassTestCase
{
	private GenerationState generationState;

	@Override
	public void testAll() throws Exception
	{
		generationState = new GenerationState(environment.getProject(), Progress.DEAF, analyzeExhaust, environment.getSourceFiles());
		generationState.compileAndGenerate(CompilationErrorHandler.THROW_EXCEPTION);

		super.testAll();
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
