package org.napile.compiler.lang.psi.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.NXmlParentedElementBase;
import org.napile.compiler.lang.psi.NapileConstructorCalleeExpression;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.psi.NapileVisitor;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.util.NXmlMirrorUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;

/**
 * @author VISTALL
 * @date 20:05/20.02.13
 */
public class NXmlConstructorCalleeExpressionImpl extends NXmlParentedElementBase implements NapileConstructorCalleeExpression
{
	private NapileTypeReference typeReference;

	public NXmlConstructorCalleeExpressionImpl(PsiElement parent, PsiElement mirror)
	{
		super(parent, mirror);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileConstructorCalleeExpression mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		typeReference = NXmlMirrorUtil.mirrorType(this, mirror.getTypeReference());
	}

	@Nullable
	@Override
	public NapileTypeReference getTypeReference()
	{
		return typeReference;
	}

	@Nullable
	@Override
	public NapileReferenceExpression getConstructorReferenceExpression()
	{
		return null;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitExpression(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitExpression(this, data);
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return NXmlMirrorUtil.getAllToPsiArray(typeReference);
	}
}
