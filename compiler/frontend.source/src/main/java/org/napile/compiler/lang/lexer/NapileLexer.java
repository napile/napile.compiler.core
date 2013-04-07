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

/*
 * @author max
 */
package org.napile.compiler.lang.lexer;

import java.io.Reader;

import org.napile.compiler.lexer.LookAheadLexer;
import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.IElementType;

public class NapileLexer extends LookAheadLexer implements NapileTokens
{
	public NapileLexer()
	{
		super(new FlexAdapter(new _NapileLexer((Reader) null)));
	}

	@Override
	protected void lookAhead(Lexer baseLexer)
	{
		if(baseLexer.getTokenType() == INJECTION_START)
		{
			advanceLexer(baseLexer);

			if(baseLexer.getTokenType() == IDENTIFIER)
			{
				advanceAs(baseLexer, INJECTION_NAME);

				if(WHITESPACES.contains(baseLexer.getTokenType()))
				{
					advanceLexer(baseLexer);
				}

				boolean hasBody = false;
				while(true)
				{
					final IElementType tokenType = baseLexer.getTokenType();
					if(tokenType == null)
					{
						if(hasBody)
						{
							addToken(INJECTION_BLOCK);
						}
						break;
					}
					else if(tokenType == INJECTION_STOP)
					{
						addToken(baseLexer.getTokenStart(), INJECTION_BLOCK);
						break;
					}
					else
					{
						hasBody = true;
						baseLexer.advance();
					}
				}
			}
		}
		else
		{
			advanceLexer(baseLexer);
		}
	}
}
