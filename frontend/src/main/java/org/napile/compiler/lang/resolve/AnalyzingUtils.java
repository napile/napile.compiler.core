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

package org.napile.compiler.lang.resolve;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.diagnostics.DiagnosticHolder;
import org.napile.compiler.lang.diagnostics.DiagnosticUtils;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;

/**
 * @author abreslav
 */
public class AnalyzingUtils
{

	public abstract static class PsiErrorElementVisitor extends NapileVisitorVoid
	{
		@Override
		public void visitElement(PsiElement element)
		{
			element.acceptChildren(this);
		}

		@Override
		public abstract void visitErrorElement(PsiErrorElement element);
	}


	public static void checkForSyntacticErrors(@NotNull PsiElement root)
	{
		root.acceptChildren(new PsiErrorElementVisitor()
		{
			@Override
			public void visitErrorElement(PsiErrorElement element)
			{
				throw new IllegalArgumentException(element.getErrorDescription() +
						"; looking at " +
						element.getNode().getElementType() +
						" '" +
						element.getText() +
						DiagnosticUtils.atLocation(element));
			}
		});
	}

	public static List<PsiErrorElement> getSyntaxErrorRanges(@NotNull PsiElement root)
	{
		final ArrayList<PsiErrorElement> r = new ArrayList<PsiErrorElement>();
		root.acceptChildren(new PsiErrorElementVisitor()
		{
			@Override
			public void visitErrorElement(PsiErrorElement element)
			{
				r.add(element);
			}
		});
		return r;
	}

	public static void throwExceptionOnErrors(BindingTrace bindingContext)
	{
		for(Diagnostic diagnostic : bindingContext.getDiagnostics())
		{
			DiagnosticHolder.THROW_EXCEPTION.report(diagnostic);
		}
	}

	// --------------------------------------------------------------------------------------------------------------------------
}
