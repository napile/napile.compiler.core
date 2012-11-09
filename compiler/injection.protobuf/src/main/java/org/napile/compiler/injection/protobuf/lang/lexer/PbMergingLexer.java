package org.napile.compiler.injection.protobuf.lang.lexer;

import static org.napile.compiler.injection.protobuf.lang.PbElementTypes.WHITE_SPACE;

import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.psi.tree.TokenSet;

/**
 * @author Nikolay Matveev
 *         Date: Mar 21, 2010
 */

public class PbMergingLexer extends MergingLexerAdapter
{
	private static final TokenSet tokensToMerge = TokenSet.create(WHITE_SPACE);

	public PbMergingLexer()
	{
		super(new PbFlexLexer(), tokensToMerge);
	}
}
