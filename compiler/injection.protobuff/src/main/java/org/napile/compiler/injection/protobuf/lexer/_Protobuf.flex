package org.napile.compiler.injection.protobuf.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

@SuppressWarnings({"ALL"})
%%

%{
  public _ProtobufLexer()
  {
    this((java.io.Reader)null);
  }

  public void goTo(int offset)
  {
    zzCurrentPos = zzMarkedPos = zzStartRead = offset;
    zzPushbackPos = 0;
    zzAtEOF = offset < zzEndRead;
  }
%}

%unicode
%class _ProtobufLexer
%implements FlexLexer
%function advance
%type IElementType
%eof{  return;
%eof}

WHITE_SPACE_CHAR=[\ \n\r\t\f]

IDENTIFIER=[:jletter:] [:jletterdigit:]*

C_STYLE_COMMENT=("/*"{COMMENT_TAIL})|"/*"
COMMENT_TAIL=([^"*"]*("*"+[^"*""/"])?)*("*"+"/")?
END_OF_LINE_COMMENT="/""/"[^\r\n]*

STRING_LITERAL="'"([^\\\'\r\n]|{ESCAPE_SEQUENCE})*("'"|\\)?
ESCAPE_SEQUENCE=\\[^\r\n]

%%

<YYINITIAL> {WHITE_SPACE_CHAR}+ { return ProtobufTokens.WHITE_SPACE; }

<YYINITIAL> "message" { return ProtobufTokens.MESSAGE_KEYWORD; }

<YYINITIAL> {IDENTIFIER} { return ProtobufTokens.IDENTIFIER; }

<YYINITIAL> . { return ProtobufTokens.BAD_CHARACTER; }