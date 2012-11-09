/* It's an automatically generated code. Do not modify it. */
package org.napile.compiler.injection.regexp.lang.lexer;

import java.util.EnumSet;
import java.util.LinkedList;

import org.napile.compiler.injection.regexp.lang.parser.RegExpCapability;
import org.napile.compiler.injection.regexp.lang.parser.RegExpTokens;
import org.napile.compiler.injection.regexp.lang.parser.StringEscapesTokenTypes;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

// IDEADEV-11055
@SuppressWarnings({ "ALL", "SameParameterValue", "WeakerAccess", "SameReturnValue", "RedundantThrows", "UnusedDeclaration", "UnusedDeclaration" })
%%

%class _RegExpLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

%{
    // This adds support for nested states. I'm no JFlex pro, so maybe this is overkill, but it works quite well.
    private final LinkedList<Integer> states = new LinkedList();

    // This was an idea to use the regex implementation for XML schema regexes (which use a slightly different syntax)
    // as well, but is currently unfinished as it requires to tweak more places than just the lexer.
    private boolean xmlSchemaMode;


    private boolean allowDanglingMetacharacters;
    private boolean allowNestedCharacterClasses;
    private boolean allowOctalNoLeadingZero;

    _RegExpLexer(EnumSet<RegExpCapability> capabilities) {
      this((java.io.Reader)null);
      this.xmlSchemaMode = capabilities.contains(RegExpCapability.XML_SCHEMA_MODE);
      this.allowDanglingMetacharacters = capabilities.contains(RegExpCapability.DANGLING_METACHARACTERS);
      this.allowNestedCharacterClasses = capabilities.contains(RegExpCapability.NESTED_CHARACTER_CLASSES);
      this.allowOctalNoLeadingZero = capabilities.contains(RegExpCapability.OCTAL_NO_LEADING_ZERO);
      this.commentMode = capabilities.contains(RegExpCapability.COMMENT_MODE);
    }

    private void yypushstate(int state) {
        states.addFirst(yystate());
        yybegin(state);
    }
    private void yypopstate() {
        final int state = states.removeFirst();
        yybegin(state);
    }

    private void handleOptions() {
      final String o = yytext().toString();
      if (o.contains("x")) {
        commentMode = !o.startsWith("-");
      }
    }

    // tracks whether the lexer is in comment mode, i.e. whether whitespace is not significant and whether to ignore
    // text after '#' till EOL
    boolean commentMode = false;
%}

%xstate QUOTED
%xstate EMBRACED
%xstate CLASS1
%xstate CLASS1PY
%state CLASS2
%state PROP
%xstate OPTIONS
%xstate COMMENT
%xstate NAMED_GROUP
%xstate QUOTED_NAMED_GROUP
%xstate PY_NAMED_GROUP_REF
%xstate PY_COND_REF

DIGITS=[1-9][0-9]*

DOT="."
LPAREN="("
RPAREN=")"
LBRACE="{"
RBRACE="}"
LBRACKET="["
RBRACKET="]"

ESCAPE="\\"
ANY=.|\n

META={ESCAPE} | {DOT} |
  "^" | "$" | "?" | "*" | "+" | "|" |
  {LBRACKET} | {LBRACE} | {LPAREN} | {RPAREN}

CONTROL="t" | "n" | "r" | "f" | "a" | "e"
BOUNDARY="b" | "B" | "A" | "z" | "Z" | "G"

CLASS="w" | "W" | "s" | "S" | "d" | "D" | "X" | "C"
XML_CLASS="c" | "C" | "i" | "I"
PROP="p" | "P"

HEX_CHAR=[0-9a-fA-F]

%%

"\\Q"                { yypushstate(QUOTED); return RegExpTokens.QUOTE_BEGIN; }

<QUOTED> {
  "\\E"              { yypopstate(); return RegExpTokens.QUOTE_END; }
  {ANY}              { return RegExpTokens.CHARACTER; }
}

/* \\ */
{ESCAPE} {ESCAPE}    { return RegExpTokens.ESC_CHARACTER; }

/* hex escapes */
{ESCAPE} "x" {HEX_CHAR}{2}   { return RegExpTokens.HEX_CHAR; }
{ESCAPE} "x" {ANY}{0,2}      { return RegExpTokens.BAD_HEX_VALUE; }

/* unicode escapes */
{ESCAPE} "u" {HEX_CHAR}{4}   { return RegExpTokens.UNICODE_CHAR; }
{ESCAPE} "u" {ANY}{0,4}      { return StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN; }

/* octal escapes */
{ESCAPE} "0" [0-7]{1,3}      { return RegExpTokens.OCT_CHAR; }
{ESCAPE} "0"                 { return (allowOctalNoLeadingZero ? RegExpTokens.OCT_CHAR : RegExpTokens.BAD_OCT_VALUE); }

/* single character after "\c" */
{ESCAPE} "c" {ANY}           { if (xmlSchemaMode) { yypushback(1); return RegExpTokens.CHAR_CLASS; } else return RegExpTokens.CTRL; }

{ESCAPE} {XML_CLASS}         { if (xmlSchemaMode) return RegExpTokens.CHAR_CLASS; else return StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN; }


/* java.util.regex.Pattern says about backrefs:
    "In this class, \1 through \9 are always interpreted as back references,
    and a larger number is accepted as a back reference if at least that many
    subexpressions exist at that point in the regular expression, otherwise the
    parser will drop digits until the number is smaller or equal to the existing
    number of groups or it is one digit."

    So, for 100% compatibility, backrefs > 9 should be resolved by the parser, but
    I'm not sure if it's worth the effort - at least not atm.
*/
{ESCAPE} [0-7]{3}             { if (allowOctalNoLeadingZero) return RegExpTokens.OCT_CHAR;
                                return yystate() != CLASS2 ? RegExpTokens.BACKREF : RegExpTokens.ESC_CHARACTER;
                              }

{ESCAPE} {DIGITS}             { return yystate() != CLASS2 ? RegExpTokens.BACKREF : RegExpTokens.ESC_CHARACTER; }

{ESCAPE}  "-"                 { return RegExpTokens.ESC_CHARACTER; }
{ESCAPE}  {META}              { return RegExpTokens.ESC_CHARACTER; }
{ESCAPE}  {CLASS}             { return RegExpTokens.CHAR_CLASS;    }
{ESCAPE}  {PROP}              { yypushstate(PROP); return RegExpTokens.PROPERTY;      }

{ESCAPE}  {BOUNDARY}          { return yystate() != CLASS2 ? RegExpTokens.BOUNDARY : RegExpTokens.ESC_CHARACTER; }
{ESCAPE}  {CONTROL}           { return RegExpTokens.ESC_CTRL_CHARACTER; }

{ESCAPE}  [:letter:]          { return StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN; }
{ESCAPE}  [\n\b\t\r\f ]       { return commentMode ? RegExpTokens.CHARACTER : RegExpTokens.REDUNDANT_ESCAPE; }

<CLASS2> {
  {ESCAPE} {RBRACKET}         { if (!allowNestedCharacterClasses) return RegExpTokens.CHARACTER;
                                return RegExpTokens.REDUNDANT_ESCAPE; }
}

{ESCAPE}  {ANY}               { return RegExpTokens.REDUNDANT_ESCAPE; }


{ESCAPE}                      { return StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN; }

<PROP> {
  {LBRACE}                    { yypopstate(); yypushstate(EMBRACED); return RegExpTokens.LBRACE; }
  {ANY}                       { yypopstate(); yypushback(1); }
}

/* "{" \d+(,\d*)? "}" */
/* "}" outside counted closure is treated as regular character */
{LBRACE}              { if (yystate() != CLASS2) yypushstate(EMBRACED); return RegExpTokens.LBRACE; }

<EMBRACED> {
  [:letter:]([:letter:]|_|[:digit:])*     { return RegExpTokens.NAME;   }
  [:digit:]+          { return RegExpTokens.NUMBER; }
  ","                 { return RegExpTokens.COMMA;  }

  {RBRACE}            { yypopstate(); return RegExpTokens.RBRACE; }
  {ANY}               { if (allowDanglingMetacharacters) {
                          yypopstate(); yypushback(1); 
                        } else {
                          return RegExpTokens.BAD_CHARACTER;
                        }
                      }
}

"-"                   { return RegExpTokens.MINUS; }
"^"                   { return RegExpTokens.CARET; }

<CLASS2> {
  {LBRACKET}          { if (allowNestedCharacterClasses) {
                           yypushstate(CLASS2);
                           return RegExpTokens.CLASS_BEGIN;
                        }
                        return RegExpTokens.CHARACTER;
                      }

  {LBRACKET} / {RBRACKET} { if (allowNestedCharacterClasses) {
                              yypushstate(CLASS1);
                              return RegExpTokens.CLASS_BEGIN;
                            }
                            return RegExpTokens.CHARACTER;
                          }
}

{LBRACKET} / {RBRACKET}   { yypushstate(CLASS1);
                            return RegExpTokens.CLASS_BEGIN; }

/* Python understands that, Java doesn't */
{LBRACKET} / "^" {RBRACKET} { if (!allowNestedCharacterClasses) {
                                yypushstate(CLASS1PY);
                              }
                              else {
                                yypushstate(CLASS2);
                              }
                              return RegExpTokens.CLASS_BEGIN;
                            }

{LBRACKET}                { yypushstate(CLASS2);
                            return RegExpTokens.CLASS_BEGIN; }

/* []abc] is legal. The first ] is treated as literal character */
<CLASS1> {
  {RBRACKET}              { yybegin(CLASS2); return RegExpTokens.CHARACTER; }
  .                       { assert false : yytext(); }
}

<CLASS1PY> {
  "^"                     { yybegin(CLASS1); return RegExpTokens.CARET; }
  .                       { assert false : yytext(); }
}

<CLASS2> {
  {RBRACKET}            { yypopstate(); return RegExpTokens.CLASS_END; }

  "&&"                  { return allowNestedCharacterClasses ? RegExpTokens.ANDAND : RegExpTokens.CHARACTER;    }
  [\n\b\t\r\f]          { return commentMode ? com.intellij.psi.TokenType.WHITE_SPACE : RegExpTokens.ESC_CHARACTER; }
  {ANY}                 { return RegExpTokens.CHARACTER; }
}


<YYINITIAL> {
  {LPAREN}      { return RegExpTokens.GROUP_BEGIN; }
  {RPAREN}      { return RegExpTokens.GROUP_END;   }

  "|"           { return RegExpTokens.UNION;  }
  "?"           { return RegExpTokens.QUEST;  }
  "*"           { return RegExpTokens.STAR;   }
  "+"           { return RegExpTokens.PLUS;   }
  "$"           { return RegExpTokens.DOLLAR; }
  {DOT}         { return RegExpTokens.DOT;    }

  "(?:"|"(?>" { return RegExpTokens.NON_CAPT_GROUP;  }
  "(?="       { return RegExpTokens.POS_LOOKAHEAD;   }
  "(?!"       { return RegExpTokens.NEG_LOOKAHEAD;   }
  "(?<="      { return RegExpTokens.POS_LOOKBEHIND;  }
  "(?<!"      { return RegExpTokens.NEG_LOOKBEHIND;  }
  "(?#" [^)]+ ")" { return RegExpTokens.COMMENT;    }
  "(?P<" { yybegin(NAMED_GROUP); return RegExpTokens.PYTHON_NAMED_GROUP; }
  "(?P=" { yybegin(PY_NAMED_GROUP_REF); return RegExpTokens.PYTHON_NAMED_GROUP_REF; }
  "(?("  { yybegin(PY_COND_REF); return RegExpTokens.PYTHON_COND_REF; }

  "(?<" { yybegin(NAMED_GROUP); return RegExpTokens.RUBY_NAMED_GROUP; }
  "(?'" { yybegin(QUOTED_NAMED_GROUP); return RegExpTokens.RUBY_QUOTED_NAMED_GROUP; }

  "(?"        { yybegin(OPTIONS); return RegExpTokens.SET_OPTIONS; }
}

<OPTIONS> {
  [:letter:]*         { handleOptions(); return RegExpTokens.OPTIONS_ON; }
  ("-" [:letter:]*)   { handleOptions(); return RegExpTokens.OPTIONS_OFF; }

  ":"               { yybegin(YYINITIAL); return RegExpTokens.COLON;  }
  ")"               { yybegin(YYINITIAL); return RegExpTokens.GROUP_END; }

  {ANY}             { yybegin(YYINITIAL); return RegExpTokens.BAD_CHARACTER; }
}

<NAMED_GROUP> {
  [:letter:]([:letter:]|_|[:digit:])* { return RegExpTokens.NAME; }
  ">"               { yybegin(YYINITIAL); return RegExpTokens.GT; }
  {ANY}             { yybegin(YYINITIAL); return RegExpTokens.BAD_CHARACTER; }
}

<QUOTED_NAMED_GROUP> {
  [:letter:]([:letter:]|_|[:digit:])* { return RegExpTokens.NAME; }
  "'"               { yybegin(YYINITIAL); return RegExpTokens.QUOTE; }
  {ANY}             { yybegin(YYINITIAL); return RegExpTokens.BAD_CHARACTER; }
}

<PY_NAMED_GROUP_REF> {
  [:letter:]([:letter:]|_|[:digit:])* { return RegExpTokens.NAME;   }
  ")"               { yybegin(YYINITIAL); return RegExpTokens.GROUP_END; }
  {ANY}             { yybegin(YYINITIAL); return RegExpTokens.BAD_CHARACTER; }
}

<PY_COND_REF> {
  [:letter:]([:letter:]|_|[:digit:])* { return RegExpTokens.NAME; }
  [:digit:]+          { return RegExpTokens.NUMBER; }
  ")"               { yybegin(YYINITIAL); return RegExpTokens.GROUP_END; }
  {ANY}             { yybegin(YYINITIAL); return RegExpTokens.BAD_CHARACTER; }
}

/* "dangling ]" */
<YYINITIAL> {RBRACKET}    { return RegExpTokens.CHARACTER; }


"#"           { if (commentMode) { yypushstate(COMMENT); return RegExpTokens.COMMENT; } else return RegExpTokens.CHARACTER; }
<COMMENT> {
  [^\r\n]*[\r\n]?  { yypopstate(); return RegExpTokens.COMMENT; }
}

" "          { return commentMode ? com.intellij.psi.TokenType.WHITE_SPACE : RegExpTokens.CHARACTER; }
[\n\b\t\r\f]   { return commentMode ? com.intellij.psi.TokenType.WHITE_SPACE : RegExpTokens.CTRL_CHARACTER; }

{ANY}        { return RegExpTokens.CHARACTER; }
