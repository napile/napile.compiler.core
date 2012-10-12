package org.napile.compiler.lang.lexer;

import java.util.*;
import com.intellij.lexer.*;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;

import org.napile.compiler.lexer.NapileTokens;

%%

%unicode
%class _NapileLexer
%implements FlexLexer

%{
    private static final class State {
        final int lBraceCount;
        final int state;

        public State(int state, int lBraceCount) {
            this.state = state;
            this.lBraceCount = lBraceCount;
        }

        @Override
        public String toString() {
            return "yystate = " + state + (lBraceCount == 0 ? "" : "lBraceCount = " + lBraceCount);
        }
    }

    private final Stack<State> states = new Stack<State>();
    private int lBraceCount;
    
    private int commentStart;
    private int commentDepth;

    private void pushState(int state) {
        states.push(new State(yystate(), lBraceCount));
        lBraceCount = 0;
        yybegin(state);
    }

    private void popState() {
        State state = states.pop();
        lBraceCount = state.lBraceCount;
        yybegin(state.state);
    }

    private IElementType commentStateToTokenType(int state) {
        switch (state) {
            case BLOCK_COMMENT:
                return NapileTokens.BLOCK_COMMENT;
            case DOC_COMMENT:
                return NapileTokens.DOC_COMMENT;
            default:
                throw new IllegalArgumentException("Unexpected state: " + state);
        }
    }
%}

%function advance
%type IElementType
%eof{
  return;
%eof}

%xstate STRING RAW_STRING SHORT_TEMPLATE_ENTRY BLOCK_COMMENT DOC_COMMENT
%state LONG_TEMPLATE_ENTRY

DIGIT=[0-9]
HEX_DIGIT=[0-9A-Fa-f]
WHITE_SPACE_CHAR=[\ \n\t\f]

// TODO: prohibit '$' in identifiers?
LETTER = [:letter:]|_
IDENTIFIER_PART=[:digit:]|{LETTER}
PLAIN_IDENTIFIER={LETTER} {IDENTIFIER_PART}*
// TODO: this one MUST allow everything accepted by the runtime
// TODO: Replace backticks by one backslash in the begining
ESCAPED_IDENTIFIER = `[^`\n]+`
IDENTIFIER = {PLAIN_IDENTIFIER}|{ESCAPED_IDENTIFIER}
FIELD_IDENTIFIER = \${IDENTIFIER}

EOL_COMMENT="/""/"[^\n]*
SHEBANG_COMMENT="#!"[^\n]*

INTEGER_LITERAL={DECIMAL_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}|{BIN_INTEGER_LITERAL}
DECIMAL_INTEGER_LITERAL=(0|([1-9]({DIGIT})*))
HEX_INTEGER_LITERAL=0[Xx]({HEX_DIGIT})*
BIN_INTEGER_LITERAL=0[Bb]({DIGIT})*

//FLOAT_LITERAL=(({FLOATING_POINT_LITERAL1})[Ff])|(({FLOATING_POINT_LITERAL2})[Ff])|(({FLOATING_POINT_LITERAL3})[Ff])|(({FLOATING_POINT_LITERAL4})[Ff])
//DOUBLE_LITERAL=(({FLOATING_POINT_LITERAL1})[Dd]?)|(({FLOATING_POINT_LITERAL2})[Dd]?)|(({FLOATING_POINT_LITERAL3})[Dd]?)|(({FLOATING_POINT_LITERAL4})[Dd])
DOUBLE_LITERAL={FLOATING_POINT_LITERAL1}|{FLOATING_POINT_LITERAL2}|{FLOATING_POINT_LITERAL3}|{FLOATING_POINT_LITERAL4}
FLOATING_POINT_LITERAL1=({DIGIT})+"."({DIGIT})+({EXPONENT_PART})?
FLOATING_POINT_LITERAL2="."({DIGIT})+({EXPONENT_PART})?
FLOATING_POINT_LITERAL3=({DIGIT})+({EXPONENT_PART})
FLOATING_POINT_LITERAL4=({DIGIT})+
EXPONENT_PART=[Ee]["+""-"]?({DIGIT})*
HEX_FLOAT_LITERAL={HEX_SIGNIFICAND}{BINARY_EXPONENT}[Ff]
//HEX_DOUBLE_LITERAL={HEX_SIGNIFICAND}{BINARY_EXPONENT}[Dd]?
HEX_DOUBLE_LITERAL={HEX_SIGNIFICAND}{BINARY_EXPONENT}?
BINARY_EXPONENT=[Pp][+-]?{DIGIT}+
HEX_SIGNIFICAND={HEX_INTEGER_LITERAL}|0[Xx]{HEX_DIGIT}*\.{HEX_DIGIT}+
//HEX_SIGNIFICAND={HEX_INTEGER_LITERAL}|{HEX_INTEGER_LITERAL}\.|0[Xx]{HEX_DIGIT}*\.{HEX_DIGIT}+

CHARACTER_LITERAL="'"([^\\\'\n]|{ESCAPE_SEQUENCE})*("'"|\\)?
// TODO: introduce symbols (e.g. 'foo) as another way to write string literals
STRING_LITERAL=\"([^\\\"\n]|{ESCAPE_SEQUENCE})*(\"|\\)?
ESCAPE_SEQUENCE=\\(u{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}{HEX_DIGIT}|[^\n])

// ANY_ESCAPE_SEQUENCE = \\[^]
THREE_QUO = (\"\"\")
ONE_TWO_QUO = (\"[^\"]) | (\"\"[^\"])
QUO_STRING_CHAR = [^\"] | {ONE_TWO_QUO}
RAW_STRING_LITERAL = {THREE_QUO} {QUO_STRING_CHAR}* {THREE_QUO}?

REGULAR_STRING_PART=[^\\\"\n\$]+
SHORT_TEMPLATE_ENTRY=\${IDENTIFIER}
LONELY_DOLLAR=\$
LONG_TEMPLATE_ENTRY_START=\$\{
LONG_TEMPLATE_ENTRY_END=\}

%%

// String templates

{THREE_QUO}                      { pushState(RAW_STRING); return NapileTokens.OPEN_QUOTE; }
<RAW_STRING> \n                  { return NapileTokens.REGULAR_STRING_PART; }
<RAW_STRING> \"                  { return NapileTokens.REGULAR_STRING_PART; }
<RAW_STRING> \\                  { return NapileTokens.REGULAR_STRING_PART; }
<RAW_STRING> {THREE_QUO}         { popState(); return NapileTokens.CLOSING_QUOTE; }

\"                          { pushState(STRING); return NapileTokens.OPEN_QUOTE; }
<STRING> \n                 { popState(); yypushback(1); return NapileTokens.DANGLING_NEWLINE; }
<STRING> \"                 { popState(); return NapileTokens.CLOSING_QUOTE; }
<STRING> {ESCAPE_SEQUENCE}  { return NapileTokens.ESCAPE_SEQUENCE; }

<STRING, RAW_STRING> {REGULAR_STRING_PART}         { return NapileTokens.REGULAR_STRING_PART; }
<STRING, RAW_STRING> {SHORT_TEMPLATE_ENTRY}        {
                                                        pushState(SHORT_TEMPLATE_ENTRY);
                                                        yypushback(yylength() - 1);
                                                        return NapileTokens.SHORT_TEMPLATE_ENTRY_START;
                                                   }
// Only *this* keyword is itself an expression valid in this position
// *null*, *true* and *false* are also keywords and expression, but it does not make sense to put them
// in a string template for it'd be easier to just type them in without a dollar
<SHORT_TEMPLATE_ENTRY> "this"          { popState(); return NapileTokens.THIS_KEYWORD; }
<SHORT_TEMPLATE_ENTRY> {IDENTIFIER}    { popState(); return NapileTokens.IDENTIFIER; }

<STRING, RAW_STRING> {LONELY_DOLLAR}               { return NapileTokens.REGULAR_STRING_PART; }
<STRING, RAW_STRING> {LONG_TEMPLATE_ENTRY_START}   { pushState(LONG_TEMPLATE_ENTRY); return NapileTokens.LONG_TEMPLATE_ENTRY_START; }

<LONG_TEMPLATE_ENTRY> "{"              { lBraceCount++; return NapileTokens.LBRACE; }
<LONG_TEMPLATE_ENTRY> "}"              {
                                           if (lBraceCount == 0) {
                                             popState();
                                             return NapileTokens.LONG_TEMPLATE_ENTRY_END;
                                           }
                                           lBraceCount--;
                                           return NapileTokens.RBRACE;
                                       }

// (Nested) comments

"/**/" {
    return NapileTokens.BLOCK_COMMENT;
}

"/**" {
    pushState(DOC_COMMENT);
    commentDepth = 0;
    commentStart = getTokenStart();
}

"/*" {
    pushState(BLOCK_COMMENT);
    commentDepth = 0;
    commentStart = getTokenStart();
}

<BLOCK_COMMENT, DOC_COMMENT> {
    "/*" {
         commentDepth++;
    }

    <<EOF>> {
        int state = yystate();
        popState();
        zzStartRead = commentStart;
        return commentStateToTokenType(state);
    }

    "*/" {
        if (commentDepth > 0) {
            commentDepth--;
        }
        else {
             int state = yystate();
             popState();
             zzStartRead = commentStart;
             return commentStateToTokenType(state);
        }
    }

    .|{WHITE_SPACE_CHAR} {}
}

// Mere mortals

({WHITE_SPACE_CHAR})+ { return NapileTokens.WHITE_SPACE; }

{EOL_COMMENT} { return NapileTokens.EOL_COMMENT; }
{SHEBANG_COMMENT} {
            if (zzCurrentPos == 0) {
                return NapileTokens.SHEBANG_COMMENT;
            }
            else {
                yypushback(yylength() - 1);
                return NapileTokens.HASH;
            }
          }

{INTEGER_LITERAL}\.\. { yypushback(2); return NapileTokens.INTEGER_LITERAL; }
{INTEGER_LITERAL} { return NapileTokens.INTEGER_LITERAL; }

{DOUBLE_LITERAL}     { return NapileTokens.FLOAT_LITERAL; }
{HEX_DOUBLE_LITERAL} { return NapileTokens.FLOAT_LITERAL; }

{CHARACTER_LITERAL} { return NapileTokens.CHARACTER_LITERAL; }

"continue"   { return NapileTokens.CONTINUE_KEYWORD ;}
"package"    { return NapileTokens.PACKAGE_KEYWORD ;}
"return"     { return NapileTokens.RETURN_KEYWORD ;}
"anonym"     { return NapileTokens.ANONYM_KEYWORD ;}
"while"      { return NapileTokens.WHILE_KEYWORD ;}
"break"      { return NapileTokens.BREAK_KEYWORD ;}

"class"      { return NapileTokens.CLASS_KEYWORD ;}
"enum"       { return NapileTokens.ENUM_KEYWORD ;}
"retell"       { return NapileTokens.RETELL_KEYWORD ;}

"var"        { return NapileTokens.VAR_KEYWORD ;}
"meth"       { return NapileTokens.METH_KEYWORD ;}

"throw"      { return NapileTokens.THROW_KEYWORD ;}
"false"      { return NapileTokens.FALSE_KEYWORD ;}
"super"      { return NapileTokens.SUPER_KEYWORD ;}
"when"       { return NapileTokens.WHEN_KEYWORD ;}
"true"       { return NapileTokens.TRUE_KEYWORD ;}
"this"       { return NapileTokens.THIS_KEYWORD ;}
"null"       { return NapileTokens.NULL_KEYWORD ;}
"else"       { return NapileTokens.ELSE_KEYWORD ;}
"try"        { return NapileTokens.TRY_KEYWORD ;}
"for"        { return NapileTokens.FOR_KEYWORD ;}
"is"         { return NapileTokens.IS_KEYWORD ;}
"in"         { return NapileTokens.IN_KEYWORD ;}
"if"         { return NapileTokens.IF_KEYWORD ;}
"do"         { return NapileTokens.DO_KEYWORD ;}
"as"         { return NapileTokens.AS_KEYWORD ;}

{FIELD_IDENTIFIER} { return NapileTokens.FIELD_IDENTIFIER; }
{IDENTIFIER} { return NapileTokens.IDENTIFIER; }
\!in{IDENTIFIER_PART}        { yypushback(3); return NapileTokens.EXCL; }
\!is{IDENTIFIER_PART}        { yypushback(3); return NapileTokens.EXCL; }

"<#<"        { return NapileTokens.IDE_TEMPLATE_START    ; }
">#>"        { return NapileTokens.IDE_TEMPLATE_END    ; }
"==="        { return NapileTokens.EQEQEQ    ; }
"!=="        { return NapileTokens.EXCLEQEQEQ; }
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
"||"         { return NapileTokens.OROR      ; }
"*="         { return NapileTokens.MULTEQ    ; }
"/="         { return NapileTokens.DIVEQ     ; }
"%="         { return NapileTokens.PERCEQ    ; }
"+="         { return NapileTokens.PLUSEQ    ; }
"-="         { return NapileTokens.MINUSEQ   ; }
"->"         { return NapileTokens.ARROW     ; }
"=>"         { return NapileTokens.DOUBLE_ARROW; }
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
">"          { return NapileTokens.GT        ; }
"?"          { return NapileTokens.QUEST     ; }
":"          { return NapileTokens.COLON     ; }
";"          { return NapileTokens.SEMICOLON ; }
"="          { return NapileTokens.EQ        ; }
","          { return NapileTokens.COMMA     ; }
"#"          { return NapileTokens.HASH      ; }
"@"          { return NapileTokens.AT        ; }

. { return TokenType.BAD_CHARACTER; }

