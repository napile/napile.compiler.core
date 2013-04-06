 package org.napile.compiler.lang.lexer;

import java.util.Stack;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

%%

%unicode
%class _NapileLexer
%implements FlexLexer

%{
    private static final class State {
        final int state;

        public State(int state) {
            this.state = state;
        }

        @Override
        public String toString() {
            return "yystate = " + state ;
        }
    }

    private final Stack<State> states = new Stack<State>();

    private int commentStart;
    private int commentDepth;

	private int injectionBraceCount;
	private int injectionStart;
	private int injectionStart2;
	private boolean isInjectionBlock;

    private void pushState(int state) {
        states.push(new State(yystate()));
        yybegin(state);
    }

    private void popState()
    {
        State state = states.pop();
        yybegin(state.state);
    }
%}

%function advance
%type IElementType
%eof{
  return;
%eof}

%xstate STRING BLOCK_COMMENT DOC_COMMENT INJECTION INJECTION_BLOCK

DIGIT=[0-9]
HEX_DIGIT=[0-9A-Fa-f]
WHITE_SPACE_CHAR=[\ \n\t\f]

LETTER = [:letter:]|_
IDENTIFIER_PART=[:digit:]|{LETTER}
IDENTIFIER={LETTER} {IDENTIFIER_PART}*

EOL_COMMENT="/""/"[^\n]*

INTEGER_LITERAL={DECIMAL_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}|{BIN_INTEGER_LITERAL}
DECIMAL_INTEGER_LITERAL=(0|([1-9]({DIGIT})*))
HEX_INTEGER_LITERAL=0[Xx]({HEX_DIGIT})*
BIN_INTEGER_LITERAL=0[Bb]({DIGIT})*

DOUBLE_LITERAL={FLOATING_POINT_LITERAL1}|{FLOATING_POINT_LITERAL2}|{FLOATING_POINT_LITERAL3}|{FLOATING_POINT_LITERAL4}
FLOATING_POINT_LITERAL1=({DIGIT})+"."({DIGIT})+({EXPONENT_PART})?
FLOATING_POINT_LITERAL2="."({DIGIT})+({EXPONENT_PART})?
FLOATING_POINT_LITERAL3=({DIGIT})+({EXPONENT_PART})
FLOATING_POINT_LITERAL4=({DIGIT})+
EXPONENT_PART=[Ee]["+""-"]?({DIGIT})*
HEX_DOUBLE_LITERAL={HEX_SIGNIFICAND}{BINARY_EXPONENT}?
BINARY_EXPONENT=[Pp][+-]?{DIGIT}+
HEX_SIGNIFICAND={HEX_INTEGER_LITERAL}|0[Xx]{HEX_DIGIT}*\.{HEX_DIGIT}+

CHARACTER_LITERAL="'"([^\\\'\n]|{ESCAPE_SEQUENCE})*("'"|\\)?
STRING_LITERAL=\"([^\\\"\n]|{ESCAPE_SEQUENCE})*(\"|\\)?
ESCAPE_SEQUENCE=\\(u{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}|[^\n])

%%

// (Nested) comments

"/**/" {
    return NapileTokens.BLOCK_COMMENT;
}

"/~"
{
    pushState(DOC_COMMENT);
    commentDepth = 0;
    commentStart = getTokenStart();
}

"/*" {
    pushState(BLOCK_COMMENT);
    commentDepth = 0;
    commentStart = getTokenStart();
}

<DOC_COMMENT>
{
    <<EOF>>
    {
        popState();
        zzStartRead = commentStart;
        return NapileTokens.DOC_COMMENT;
    }

    "~/"
    {
		popState();
		zzStartRead = commentStart;
		return NapileTokens.DOC_COMMENT;
    }

    .|{WHITE_SPACE_CHAR} {}
}

<BLOCK_COMMENT>
{
    "/*" {
         commentDepth++;
    }

    <<EOF>> {
        int state = yystate();
        popState();
        zzStartRead = commentStart;
        return NapileTokens.BLOCK_COMMENT;
    }

    "*/" {
        if (commentDepth > 0) {
            commentDepth--;
        }
        else {
             int state = yystate();
             popState();
             zzStartRead = commentStart;
             return NapileTokens.BLOCK_COMMENT;
        }
    }

    .|{WHITE_SPACE_CHAR} {}
}

// Injections
"/" {IDENTIFIER} "/"
{
	pushState(INJECTION);

	injectionStart = getTokenStart();

	return NapileTokens.INJECTION_START;
}

<INJECTION>
{
	"{"
	{
		if(!isInjectionBlock)
		{
			isInjectionBlock = true;

			pushState(INJECTION_BLOCK);

			injectionStart2 = getTokenStart() + 1;

			return NapileTokens.LBRACE;
		}
	}

	"}"
	{
		if(isInjectionBlock)
		{
			isInjectionBlock = false;
			popState();
			return NapileTokens.RBRACE;
		}
	}

	.|{WHITE_SPACE_CHAR} {}
}

<INJECTION_BLOCK>
{
	"{"
	{
		injectionBraceCount ++;
	}

	<<EOF>>
	{
		popState();
		zzStartRead = injectionStart2;
		return NapileTokens.INJECTION_BLOCK;
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
			return NapileTokens.INJECTION_BLOCK;
		}
	}

	.|{WHITE_SPACE_CHAR} {}
}
// Mere mortals

({WHITE_SPACE_CHAR})+ { return NapileTokens.WHITE_SPACE; }

{EOL_COMMENT} { return NapileTokens.EOL_COMMENT; }

{INTEGER_LITERAL}\.\. { yypushback(2); return NapileTokens.INTEGER_LITERAL; }
{INTEGER_LITERAL} { return NapileTokens.INTEGER_LITERAL; }

{DOUBLE_LITERAL}     { return NapileTokens.FLOAT_LITERAL; }
{HEX_DOUBLE_LITERAL} { return NapileTokens.FLOAT_LITERAL; }

{CHARACTER_LITERAL} { return NapileTokens.CHARACTER_LITERAL; }
{STRING_LITERAL}    { return NapileTokens.STRING_LITERAL; }

"continue"   { return NapileTokens.CONTINUE_KEYWORD ;}
"package"    { return NapileTokens.PACKAGE_KEYWORD ;}
"return"     { return NapileTokens.RETURN_KEYWORD ;}
"anonym"     { return NapileTokens.ANONYM_KEYWORD ;}
"while"      { return NapileTokens.WHILE_KEYWORD ;}
"break"      { return NapileTokens.BREAK_KEYWORD ;}

"class"      { return NapileTokens.CLASS_KEYWORD ;}
"var"        { return NapileTokens.VAR_KEYWORD ;}
"val"        { return NapileTokens.VAL_KEYWORD ;}
"ref"        { return NapileTokens.REF_KEYWORD ;}
"meth"       { return NapileTokens.METH_KEYWORD ;}
"macro"      { return NapileTokens.MACRO_KEYWORD ;}
"this"       { return NapileTokens.THIS_KEYWORD ;}

"throw"      { return NapileTokens.THROW_KEYWORD ;}
"false"      { return NapileTokens.FALSE_KEYWORD ;}
"super"      { return NapileTokens.SUPER_KEYWORD ;}
"when"       { return NapileTokens.WHEN_KEYWORD ;}
"true"       { return NapileTokens.TRUE_KEYWORD ;}
"null"       { return NapileTokens.NULL_KEYWORD ;}
"else"       { return NapileTokens.ELSE_KEYWORD ;}
"try"        { return NapileTokens.TRY_KEYWORD ;}
"for"        { return NapileTokens.FOR_KEYWORD ;}
"is"         { return NapileTokens.IS_KEYWORD ;}
"in"         { return NapileTokens.IN_KEYWORD ;}
"if"         { return NapileTokens.IF_KEYWORD ;}
"do"         { return NapileTokens.DO_KEYWORD ;}
"as"         { return NapileTokens.AS_KEYWORD ;}

{IDENTIFIER} { return NapileTokens.IDENTIFIER; }
\!in{IDENTIFIER_PART}        { yypushback(3); return NapileTokens.EXCL; }
\!is{IDENTIFIER_PART}        { yypushback(3); return NapileTokens.EXCL; }

"<#<"        { return NapileTokens.IDE_TEMPLATE_START    ; }
">#>"        { return NapileTokens.IDE_TEMPLATE_END    ; }
"!in"        { return NapileTokens.NOT_IN; }
"!is"        { return NapileTokens.NOT_IS; }
"as?"        { return NapileTokens.AS_SAFE; }
"++"         { return NapileTokens.PLUSPLUS  ; }
"--"         { return NapileTokens.MINUSMINUS; }
"<="         { return NapileTokens.LTEQ      ; }
">="         { return NapileTokens.GTEQ      ; }
"=="         { return NapileTokens.EQEQ      ; }
"!="         { return NapileTokens.EXCLEQ    ; }
"&&"         { return NapileTokens.ANDAND    ; }
"&"          { return NapileTokens.AND       ; }
"&="         { return NapileTokens.ANDEQ     ; }
"||"         { return NapileTokens.OROR      ; }
"|"          { return NapileTokens.OR        ; }
"|="         { return NapileTokens.OREQ      ; }
"~"          { return NapileTokens.TILDE     ; }
"^"          { return NapileTokens.XOR       ; }
"^="         { return NapileTokens.XOREQ     ; }
"*="         { return NapileTokens.MULTEQ    ; }
"/="         { return NapileTokens.DIVEQ     ; }
"%="         { return NapileTokens.PERCEQ    ; }
"+="         { return NapileTokens.PLUSEQ    ; }
"-="         { return NapileTokens.MINUSEQ   ; }
"->>"        { return NapileTokens.DOUBLE_ARROW ; }
"->"         { return NapileTokens.ARROW     ; }
".."         { return NapileTokens.RANGE     ; }
"["          { return NapileTokens.LBRACKET  ; }
"]"          { return NapileTokens.RBRACKET  ; }
"{"          { return NapileTokens.LBRACE    ; }
"}"          { return NapileTokens.RBRACE    ; }
"("          { return NapileTokens.LPAR      ; }
")"          { return NapileTokens.RPAR      ; }
"."          { return NapileTokens.DOT       ; }
"*"          { return NapileTokens.MUL       ; }
"+"          { return NapileTokens.PLUS      ; }
"-"          { return NapileTokens.MINUS     ; }
"!"          { return NapileTokens.EXCL      ; }
"/"          { return NapileTokens.DIV       ; }
"%"          { return NapileTokens.PERC      ; }
"<"          { return NapileTokens.LT        ; }
"<<"         { return NapileTokens.LTLT      ; }
"<<="        { return NapileTokens.LTLTEQ    ; }
">"          { return NapileTokens.GT        ; }
"?"          { return NapileTokens.QUEST     ; }
":"          { return NapileTokens.COLON     ; }
";"          { return NapileTokens.SEMICOLON ; }
"="          { return NapileTokens.EQ        ; }
","          { return NapileTokens.COMMA     ; }
"#"          { return NapileTokens.HASH      ; }
"@"          { return NapileTokens.AT        ; }

. { return TokenType.BAD_CHARACTER; }

