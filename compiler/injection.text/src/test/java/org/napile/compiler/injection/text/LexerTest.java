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

package org.napile.compiler.injection.text;

import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @since 21:47/09.11.12
 */
public class LexerTest
{
	public static void main(String... arg)
	{
		String str = "myVar is #{myVar fdsfv dsv sdvsdv vsd{dsadas} dsad dsad} #{empty} #} # # # # # # }}}}} #{fsdfsdfsd";

		TextCodeInjection injection = new TextCodeInjection();

		Lexer textLexer = injection.createLexer(null);
		textLexer.start(str);

		IElementType token = null;
		while((token = textLexer.getTokenType()) != null)
		{
			System.out.println("[" + token + "] - [" + textLexer.getTokenText() + "]");
			textLexer.advance();
		}
	}
}
