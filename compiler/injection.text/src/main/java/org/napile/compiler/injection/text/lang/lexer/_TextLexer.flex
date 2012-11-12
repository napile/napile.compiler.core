package org.napile.compiler.injection.text.lang.lexer;

import java.util.Stack;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

%%

%class _TextLexer
%implements FlexLexer
%unicode
%public

%{
    private static final class State
    {
        final int lBraceCount;
        final int state;

        public State(int state, int lBraceCount)
        {
            this.state = state;
            this.lBraceCount = lBraceCount;
        }

        @Override
        public String toString()
        {
            return "yystate = " + state + (lBraceCount == 0 ? "" : "lBraceCount = " + lBraceCount);
        }
    }

    private final Stack<State> states = new Stack<State>();
    private int lBraceCount;

	private int injectionBraceCount;
	private int injectionStart;
	private int injectionStart2;
	private boolean isInjectionBlock;

    private void pushState(int state)
    {
        states.push(new State(yystate(), lBraceCount));
        lBraceCount = 0;
        yybegin(state);
    }

    private void popState()
    {
        State state = states.pop();
        lBraceCount = state.lBraceCount;
        yybegin(state.state);
    }
%}

%type IElementType

%function advance

%eof{ return;
%eof}

%xstate NAPILE_EXPRESSION NAPILE_EXPRESSION_BLOCK

%%

"#"
{
	pushState(NAPILE_EXPRESSION);

	injectionStart = getTokenStart();
	return TextTokens.HASH;
}

<NAPILE_EXPRESSION>
{
	"{"
	{
		if(!isInjectionBlock)
		{
			isInjectionBlock = true;

			pushState(NAPILE_EXPRESSION_BLOCK);

			injectionStart2 = getTokenStart() + 1;

			return TextTokens.LBRACE;
		}
	}

	"}"
	{
		if(isInjectionBlock)
		{
			isInjectionBlock = false;
			popState();
			return TextTokens.RBRACE;
		}
	}

	.
	{
		if(!isInjectionBlock)
		{
			popState();
			zzStartRead = injectionStart2;
		}
	}
}

<NAPILE_EXPRESSION_BLOCK>
{
	"{"
	{
		injectionBraceCount ++;
	}

	<<EOF>>
	{
		popState();
		zzStartRead = injectionStart2;
		return TextTokens.NAPILE_EXPRESSION;
    }

	"}"
	{
		if(injectionBraceCount > 0)
			injectionBraceCount --;
		else
		{
			popState();
			yypushback(1);
			zzStartRead = injectionStart2;
			return TextTokens.NAPILE_EXPRESSION;
		}
	}

	. {}
}

\r|\n|\r\n|.  {return TextTokens.TEXT_PART;}


