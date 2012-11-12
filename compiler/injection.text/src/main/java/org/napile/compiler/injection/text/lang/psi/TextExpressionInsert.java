package org.napile.compiler.injection.text.lang.psi;

import org.jetbrains.annotations.NotNull;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;

/**
 * @author VISTALL
 * @date 15:55/12.11.12
 */
public class TextExpressionInsert extends ASTWrapperPsiElement
{
	public TextExpressionInsert(@NotNull ASTNode node)
	{
		super(node);
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
