package org.napile.compiler.lang.psi.impl;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;

/**
 * @author VISTALL
 * @date 19:29/20.02.13
 */
public class NXmlAnnotationImpl extends NXmlParentedElementBase implements NapileAnnotation
{
	private NXmlConstructorCalleeExpressionImpl calleeExpression;

	public NXmlAnnotationImpl(PsiElement parent)
	{
		super(parent);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileAnnotation mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		calleeExpression = new NXmlConstructorCalleeExpressionImpl(this);
		calleeExpression.setMirror(mirror.getCalleeExpression());
	}

	@Nullable
	@Override
	public NapileTypeReference getTypeReference()
	{
		NapileConstructorCalleeExpression calleeExpression = getCalleeExpression();

		return calleeExpression.getTypeReference();
	}

	@NotNull
	@Override
	public NapileConstructorCalleeExpression getCalleeExpression()
	{
		return calleeExpression;
	}

	@Nullable
	@Override
	public NapileValueArgumentList getValueArgumentList()
	{
		return null;
	}

	@NotNull
	@Override
	public List<? extends ValueArgument> getValueArguments()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public List<NapileExpression> getFunctionLiteralArguments()
	{
		return Collections.emptyList();
	}

	@NotNull
	@Override
	public List<? extends NapileTypeReference> getTypeArguments()
	{
		return getTypeReference().getTypeElement().getTypeArguments();
	}

	@Nullable
	@Override
	public NapileTypeArgumentList getTypeArgumentList()
	{
		return null;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitAnnotation(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitAnnotation(this, data);
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return NXmlMirrorUtil.getAllToPsiArray(calleeExpression);
	}
}
