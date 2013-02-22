package org.napile.idea.plugin.editor.wordSelection;

import java.util.Iterator;
import java.util.List;

import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileElement;
import com.intellij.codeInsight.editorActions.wordSelection.AbstractWordSelectioner;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

/**
 * Original from IDEA CE: com.intellij.codeInsight.editorActions.wordSelection.JavaWordSelectioner
 */
public class NapileWordSelectioner extends AbstractWordSelectioner
{
	@Override
	public boolean canSelect(PsiElement e)
	{
		if(e instanceof NapileElement)
		{
			IElementType tokenType = ((NapileElement) e).getElementType();
			return tokenType == NapileTokens.IDENTIFIER || tokenType == NapileTokens.STRING_LITERAL;
		}
		return false;
	}

	@Override
	public List<TextRange> select(PsiElement e, CharSequence editorText, int cursorOffset, Editor editor)
	{
		List<TextRange> ranges = super.select(e, editorText, cursorOffset, editor);
		if(e instanceof NapileElement && ((NapileElement) e).getElementType() == NapileTokens.STRING_LITERAL)
		{
			killRangesBreakingEscapes(e, ranges, e.getTextRange());
		}
		return ranges;
	}

	private static void killRangesBreakingEscapes(PsiElement e, List<TextRange> ranges, TextRange literalRange)
	{
		for(Iterator<TextRange> iterator = ranges.iterator(); iterator.hasNext(); )
		{
			TextRange each = iterator.next();
			if(literalRange.contains(each) &&
					literalRange.getStartOffset() < each.getStartOffset() &&
					e.getText().charAt(each.getStartOffset() - literalRange.getStartOffset() - 1) == '\\')
			{
				iterator.remove();
			}
		}
	}
}
