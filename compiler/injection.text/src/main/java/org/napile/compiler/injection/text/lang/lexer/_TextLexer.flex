package org.napile.compiler.injection.text.lang.lexer;

import java.util.Stack;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

%%

%class _TextLexer
%implements FlexLexer
%unicode
%public

%type IElementType

%function advance

%eof{ return;
%eof}


%%


"#"  {return TextTokens.HASH;}
"{"  {return TextTokens.LBRACE;}
"}"  {return TextTokens.RBRACE;}
\r|\n|\r\n|.  {return TextTokens.TEXT_PART;}


