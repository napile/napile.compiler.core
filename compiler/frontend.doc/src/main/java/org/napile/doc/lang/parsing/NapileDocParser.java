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

package org.napile.doc.lang.parsing;

import org.jetbrains.annotations.NotNull;
import org.napile.doc.lang.lexer.NapileDocNodes;
import org.napile.doc.lang.lexer.NapileDocTokens;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * @author VISTALL
 * @since 23:04/30.01.13
 */
public class NapileDocParser implements PsiParser, NapileDocTokens, NapileDocNodes
{
	private static final TokenSet SKIP_ELEMENTS = TokenSet.create(NEW_LINE, WHITE_SPACE);

	@NotNull
	@Override
	public ASTNode parse(IElementType root, PsiBuilder builder)
	{
		PsiBuilder.Marker marker = builder.mark();

		while(!builder.eof())
		{
			skipUntilNotSpaceOrNewLine(builder);

			if(builder.getTokenType() == DOC_START)
			{
				builder.advanceLexer();

				if(builder.getTokenType() != NEW_LINE)
					builder.error("Expect new line");
				else
					builder.advanceLexer();
			}
			else if(builder.getTokenType() == DOC_END)
			{
				builder.advanceLexer();
				break;
			}
			else if (builder.getTokenType() == TILDE)
			{
				parseDocLine(builder);
			}
			else
			{
				builder.error("Unknown symbol");
				builder.advanceLexer();
			}
		}
		marker.done(root);
		return builder.getTreeBuilt();
	}

	private void parseDocLine(PsiBuilder builder)
	{
		builder.advanceLexer();

		if(builder.getTokenType() == WHITE_SPACE)
			builder.advanceLexer();
		else if(builder.getTokenType() == NEW_LINE)
		{
			PsiBuilder.Marker marker = builder.mark();
			marker.done(DOC_LINE);
		}
		else
			builder.error("Expect whitespace");

		PsiBuilder.Marker marker = builder.mark();

		while(!builder.eof())
		{
			if(builder.getTokenType() == NEW_LINE)
				break;

			if(builder.getTokenType() == WHITE_SPACE || builder.getTokenType() == TILDE)
				builder.remapCurrentToken(TEXT_PART);

			if(builder.getTokenType() == NapileDocTokens.CODE_MARKER)
			{
				builder.remapCurrentToken(TEXT_PART); //FIXME [VISTALL] remove this
				/*PsiBuilder.Marker codeMarker = builder.mark();

				builder.advanceLexer();

				while(!builder.eof())
				{
					if(builder.getTokenType() == NapileDocTokens.CODE_MARKER)
					{
						break;
					}
					else
					{
						builder.advanceLexer();
					}
				}

				if(builder.getTokenType() != NapileDocTokens.CODE_MARKER)
				{
					codeMarker.drop();
				}
				else
				{
					builder.advanceLexer();
					codeMarker.done(NapileDocNodes.CODE_BLOCK);
				} */
			}

			builder.advanceLexer();
		}

		if(builder.getTokenType() == NEW_LINE)
		{
			marker.done(DOC_LINE);

			builder.advanceLexer();
		}
		else
			marker.drop();
	}

	private static boolean expect(PsiBuilder builder, IElementType e)
	{
		if(builder.getTokenType() == e)
		{
			builder.advanceLexer();
			return true;
		}
		else
		{
			builder.error("Invalid symbol");
			return false;
		}
	}

	private static void skipUntilNotSpaceOrNewLine(PsiBuilder builder)
	{
		while(!builder.eof())
		{
			if(!SKIP_ELEMENTS.contains(builder.getTokenType()))
			{
				break;
			}

			builder.advanceLexer();
		}
	}
}
