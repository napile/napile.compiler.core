package org.napile.idea.injection.protobuf.highlighter;

import org.napile.compiler.injection.protobuf.lang.PbTokenTypes;
import org.napile.idea.plugin.highlighter.InjectionSyntaxHighlighter;
import org.napile.idea.plugin.highlighter.SyntaxHighlighterUtil;

/**
 * @author Nikolay Matveev
 *         Date: Mar 5, 2010
 */
public class PbSyntaxHighlighter extends InjectionSyntaxHighlighter
{
	public PbSyntaxHighlighter()
	{
		SyntaxHighlighterUtil.fillMap(keys, PbTokenTypes.COMMENTS, PbDefaultHighlighter.LINE_COMMENT_ATTR_KEY);
		SyntaxHighlighterUtil.fillMap(keys, PbTokenTypes.STRING_LITERALS, PbDefaultHighlighter.STRING_ATTR_KEY);
		SyntaxHighlighterUtil.fillMap(keys, PbTokenTypes.WRONG_STRING_LITERALS, PbDefaultHighlighter.WRONG_STRING_ATTR_KEY);
		SyntaxHighlighterUtil.fillMap(keys, PbTokenTypes.BAD_CHARACTERS, PbDefaultHighlighter.BAD_CHARACTER_ATTR_KEY);
		SyntaxHighlighterUtil.fillMap(keys, PbTokenTypes.NUMBERS, PbDefaultHighlighter.NUMBER_ATTR_KEY);
		SyntaxHighlighterUtil.fillMap(keys, PbTokenTypes.KEYWORDS, PbDefaultHighlighter.KEYWORD_ATTR_KEY);
	}
}
