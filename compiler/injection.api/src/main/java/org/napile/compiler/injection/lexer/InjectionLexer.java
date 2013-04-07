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

	private boolean expressionStarted;


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

		if(expressionStarted)
		{
			if(tokenType == rbraceType)
			{
				expressionStarted = false;

				advanceAs(baseLexer, InjectionTokens.INNER_EXPRESSION_STOP);
			}
			else
			{
				if(tokenType == null)
				{
					expressionStarted = false;
					addToken(baseLexer.getBufferEnd(), InjectionTokens.INNER_EXPRESSION);
				}
				else
				{
					advanceAs(baseLexer, InjectionTokens.INNER_EXPRESSION);
				}
			}
		}
		else if(tokenType == sharpType)
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

					expressionStarted = true;
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
