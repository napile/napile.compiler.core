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
import org.napile.compiler.NapileNodeTypes;
import org.napile.compiler.injection.CodeInjection;
import org.napile.compiler.injection.lexer.NapileInjectionKeywordToken;
import org.napile.compiler.lang.parsing.injection.CodeInjectionManager;
import org.napile.compiler.lexer.NapileTokens;
import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.TokenSet;

/**
 * @author VISTALL
 * @date 11:42/12.10.12
 */
public class CodeInjectionParser
{
	private final AbstractJetParsing parent;

	public CodeInjectionParser(@NotNull AbstractJetParsing parent)
	{
		this.parent = parent;

		parse();
	}

	public void parse()
	{
		PsiBuilder.Marker marker = parent.mark();

		parent.advance(); // advance dot

		boolean val = parent.atSet(CodeInjectionManager.INSTANCE.getInjectionTokens());
		CodeInjection codeInjection = null;
		if(val)
			codeInjection = ((NapileInjectionKeywordToken) parent.tt()).codeInjection;

		parent.advance();

		if(parent.expect(NapileTokens.LBRACE, "'{' expected"))
		{
			skipBody();
			/*if(codeInjection == null)
				skipBody();
			else
			{*/
				/*final PsiBuilderFactory factory = PsiBuilderFactory.getInstance();

				FragmentCharSequence f = new FragmentCharSequence(parent.getBuilder().getOriginalText(), parent.getBuilder().getCurrentOffset());

				PsiBuilder builder = factory.createBuilder(codeInjection, codeInjection.createLexer(parent.getBuilder().getProject()), f);

				codeInjection.parse(builder);  */

				//ASTNode node = builder.getTreeBuilt();

				//System.out.println(node);
			//}
		}

		parent.expect(NapileTokens.RBRACE, "'}' expected");

		marker.done(NapileNodeTypes.CODE_INJECTION);
	}

	private void skipBody()
	{
		PsiBuilder.Marker m = parent.mark();
		while(!parent.eof())
		{
			if(parent.tt() == NapileTokens.LBRACE)
				parent.skipUntil(TokenSet.create(NapileTokens.RBRACE));
			else if(parent.tt() == NapileTokens.RBRACE)
				break;

			parent.advance();
		}
		m.done(NapileNodeTypes.CODE_INJECTION_BLOCK);
	}
}
