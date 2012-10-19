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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.NapileNamedDeclaration;
import org.napile.compiler.lang.psi.NapileFile;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;

/**
 * @author abreslav
 */
public class RedeclarationDiagnosticFactory extends DiagnosticFactory1<PsiElement, String>
{
	private static final PositioningStrategy<PsiElement> POSITION_REDECLARATION = new PositioningStrategy<PsiElement>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull PsiElement element)
		{
			if(element instanceof NapileNamedDeclaration)
			{
				PsiElement nameIdentifier = ((NapileNamedDeclaration) element).getNameIdentifier();
				if(nameIdentifier != null)
				{
					return markElement(nameIdentifier);
				}
			}
			else if(element instanceof NapileFile)
			{
				NapileFile file = (NapileFile) element;
				PsiElement nameIdentifier = file.getNamespaceHeader().getNameIdentifier();
				if(nameIdentifier != null)
				{
					return markElement(nameIdentifier);
				}
			}
			return markElement(element);
		}
	};

	public RedeclarationDiagnosticFactory(Severity severity)
	{
		super(severity, POSITION_REDECLARATION);
	}
}
