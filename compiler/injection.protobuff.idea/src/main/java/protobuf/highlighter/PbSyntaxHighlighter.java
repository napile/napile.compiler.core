package protobuf.highlighter;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.napile.idea.plugin.highlighter.InjectionSyntaxHighlighter;
import org.napile.idea.plugin.highlighter.SyntaxHighlighterUtil;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.tree.IElementType;
import protobuf.lang.PbTokenTypes;

/**
 * @author Nikolay Matveev
 *         Date: Mar 5, 2010
 */
public class PbSyntaxHighlighter implements InjectionSyntaxHighlighter
{

	private static final Map<IElementType, TextAttributesKey> ATTRIBUTES = new HashMap<IElementType, TextAttributesKey>();

	static
	{
		SyntaxHighlighterUtil.fillMap(ATTRIBUTES, PbTokenTypes.COMMENTS, PbDefaultHighlighter.LINE_COMMENT_ATTR_KEY);
		SyntaxHighlighterUtil.fillMap(ATTRIBUTES, PbTokenTypes.STRING_LITERALS, PbDefaultHighlighter.STRING_ATTR_KEY);
		SyntaxHighlighterUtil.fillMap(ATTRIBUTES, PbTokenTypes.WRONG_STRING_LITERALS, PbDefaultHighlighter.WRONG_STRING_ATTR_KEY);
		SyntaxHighlighterUtil.fillMap(ATTRIBUTES, PbTokenTypes.BAD_CHARACTERS, PbDefaultHighlighter.BAD_CHARACTER_ATTR_KEY);
		SyntaxHighlighterUtil.fillMap(ATTRIBUTES, PbTokenTypes.NUMBERS, PbDefaultHighlighter.NUMBER_ATTR_KEY);
		SyntaxHighlighterUtil.fillMap(ATTRIBUTES, PbTokenTypes.KEYWORDS, PbDefaultHighlighter.KEYWORD_ATTR_KEY);
	}

	@Override
	@NotNull
	public TextAttributesKey[] getTokenHighlights(IElementType iElementType)
	{
		return SyntaxHighlighterUtil.pack(ATTRIBUTES.get(iElementType));
	}
}
