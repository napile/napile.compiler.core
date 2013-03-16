package org.napile.compiler.injection.text.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.injection.text.lang.lexer.TextTokens;
import org.napile.compiler.lang.psi.NapileExpression;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author VISTALL
 * @since 15:55/12.11.12
 */
public class TextExpressionInsert extends ASTWrapperPsiElement
{
	public TextExpressionInsert(@NotNull ASTNode node)
	{
		super(node);
	}

	@NotNull
	public NapileExpression getExpression()
	{
		PsiElement e = findChildByType(TextTokens.NAPILE_EXPRESSION);
		return PsiTreeUtil.getChildOfType(e, NapileExpression.class);
	}

	@Override
	public void accept(@NotNull PsiElementVisitor visitor)
	{
		if(visitor instanceof TextPsiVisitor)
			((TextPsiVisitor) visitor).visitTextInsertElement(this);
		else
			visitor.visitElement(this);
	}
}
