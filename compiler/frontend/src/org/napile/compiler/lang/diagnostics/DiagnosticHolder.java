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

package org.napile.compiler.lang.diagnostics;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.diagnostics.rendering.DefaultErrorMessages;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;

/**
 * @author abreslav
 */
public interface DiagnosticHolder
{
	DiagnosticHolder DO_NOTHING = new DiagnosticHolder()
	{
		@Override
		public void report(@NotNull Diagnostic diagnostic)
		{
		}

		@NotNull
		@Override
		public List<Diagnostic> getDiagnostics()
		{
			return Collections.emptyList();
		}
	};
	DiagnosticHolder THROW_EXCEPTION = new DiagnosticHolder()
	{
		@Override
		public void report(@NotNull Diagnostic diagnostic)
		{
			if(diagnostic.getSeverity() == Severity.ERROR)
			{
				PsiFile psiFile = diagnostic.getPsiFile();
				List<TextRange> textRanges = diagnostic.getTextRanges();
				String diagnosticText = DefaultErrorMessages.RENDERER.render(diagnostic);
				throw new IllegalStateException(diagnostic.getFactory().getName() +
						": " +
						diagnosticText +
						" " +
						psiFile.getName() +
						" " +
						DiagnosticUtils.atLocation(psiFile, textRanges.get(0)));
			}
		}

		@NotNull
		@Override
		public List<Diagnostic> getDiagnostics()
		{
			return Collections.emptyList();
		}
	};

	void report(@NotNull Diagnostic diagnostic);

	@NotNull
	List<Diagnostic> getDiagnostics();
}
