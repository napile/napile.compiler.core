package org.napile.doc.lang.lexer;

import java.util.Stack;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

%%

%unicode
%class _NapileDocLexer
%implements FlexLexer
%public

%function advance
%type IElementType
%eof{
  return;
%eof}

%%

"/~"  {return NapileDocTokens.DOC_START;}

"~/"  {return NapileDocTokens.DOC_END;}

\n  {return NapileDocTokens.NEW_LINE;}

\ |\t  {return NapileDocTokens.WHITE_SPACE;}

"~"  {return NapileDocTokens.TILDE;}

.  {return NapileDocTokens.TEXT_PART;}