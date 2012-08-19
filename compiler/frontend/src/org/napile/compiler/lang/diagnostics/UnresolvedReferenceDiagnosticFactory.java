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
import org.napile.compiler.lang.psi.NapileArrayAccessExpression;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import com.intellij.openapi.util.TextRange;

/**
 * @author abreslav
 */
public class UnresolvedReferenceDiagnosticFactory extends DiagnosticFactory1<NapileReferenceExpression, String>
{
	public UnresolvedReferenceDiagnosticFactory()
	{
		super(Severity.ERROR, new PositioningStrategy<NapileReferenceExpression>()
		{
			@NotNull
			@Override
			public List<TextRange> mark(@NotNull NapileReferenceExpression element)
			{
				if(element instanceof NapileArrayAccessExpression)
				{
					List<TextRange> ranges = ((NapileArrayAccessExpression) element).getBracketRanges();
					if(!ranges.isEmpty())
					{
						return ranges;
					}
				}
				return Collections.singletonList(element.getTextRange());
			}
		});
	}

	public DiagnosticWithParameters1<NapileReferenceExpression, String> on(@NotNull NapileReferenceExpression reference)
	{
		return new DiagnosticWithParameters1<NapileReferenceExpression, String>(reference, reference.getText(), this, severity);
	}

	public static UnresolvedReferenceDiagnosticFactory create()
	{
		return new UnresolvedReferenceDiagnosticFactory();
	}
}
