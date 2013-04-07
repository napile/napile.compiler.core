/*
 * Copyright 2010-2012 napile.org
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

package org.napile.compiler.lang.parsing;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.lexer.NapileNodes;
import org.napile.compiler.lang.lexer.NapileTokens;
import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @since 11:42/12.10.12
 */
public class CodeInjectionParser implements NapileTokens
{
	private final AbstractJetParsing parent;

	public CodeInjectionParser(@NotNull AbstractJetParsing parent)
	{
		this.parent = parent;
	}

	public void parse()
	{
		PsiBuilder.Marker marker = mark();

		if(tt() == NapileTokens.INJECTION_START)
		{
			advance();

			while(true)
			{
				IElementType tt = tt();
				if(tt == INJECTION_STOP)
				{
					break;
				}

				advance();
			}

			if(tt() != INJECTION_STOP)
			{
				parent.error("Expected ':/'");
			}
			else
			{
				advance();
			}

			marker.done(NapileNodes.INJECTION_EXPRESSION);
		}
		else
		{
			marker.error("Expected injection name");
		}
	}

	public PsiBuilder.Marker mark()
	{
		return parent.mark();
	}

	public IElementType tt()
	{
		return parent.tt();
	}

	public void advance()
	{
		parent.advance();
	}
}
