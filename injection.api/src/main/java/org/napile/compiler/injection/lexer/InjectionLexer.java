package org.napile.compiler.injection.lexer;

import org.napile.compiler.lexer.LookAheadLexer;
import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.IElementType;

/**
 * @author VISTALL
 * @since 12:45/07.04.13
 */
public class InjectionLexer extends LookAheadLexer
{
	private final IElementType sharpType;
	private final IElementType lbraceType;
	private final IElementType rbraceType;

	private int braceCount;

	public InjectionLexer(Lexer baseLexer, IElementType sharpTypes, IElementType lbraceTypes, IElementType rbraceTypes)
	{
		super(baseLexer);

		this.sharpType = sharpTypes;
		this.lbraceType = lbraceTypes;
		this.rbraceType = rbraceTypes;
	}

	@Override
	protected void lookAhead(Lexer baseLexer)
	{
		final IElementType tokenType = baseLexer.getTokenType();

		if(tokenType == sharpType)
		{
			int startEnd = baseLexer.getTokenEnd();

			baseLexer.advance();

			IElementType nextToken = baseLexer.getTokenType();
			if(nextToken != null)
			{
				if(nextToken == lbraceType)
				{
					baseLexer.advance();

					addToken(baseLexer.getTokenStart(), InjectionTokens.INNER_EXPRESSION_START);

					boolean hasBody = false;
					while(true)
					{
						IElementType elementType = baseLexer.getTokenType();
						if(elementType == lbraceType)
						{
							braceCount ++;

							hasBody = true;

							baseLexer.advance();
						}
						if(elementType == rbraceType)
						{
							if(braceCount > 0)
							{
								braceCount --;
								baseLexer.advance();
							}
							else
							{
								if(hasBody)
								{
									addToken(baseLexer.getTokenStart(), InjectionTokens.INNER_EXPRESSION);
								}

								advanceAs(baseLexer, InjectionTokens.INNER_EXPRESSION_STOP);
								break;
							}
						}
						else if(elementType == null)
						{
							if(hasBody)
							{
								addToken(InjectionTokens.INNER_EXPRESSION);
							}
							break;
						}
						else
						{
							hasBody = true;

							baseLexer.advance();
						}
					}
				}
				else
				{
					addToken(startEnd, sharpType);

					addToken(nextToken);

					baseLexer.advance();
				}
			}
		}
		else
		{
			advanceLexer(baseLexer);
		}
	}
}
