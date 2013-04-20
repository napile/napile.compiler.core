package org.napile.compiler.lang.psi.impl;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.lexer.NapileToken;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.util.NXmlMirrorUtil;
import org.napile.doc.lang.psi.NapileDoc;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.util.IncorrectOperationException;

/**
 * @author VISTALL
 * @since 16:16/22.02.13
 */
public class NXmlVariableImpl extends NXmlParentedElementBase implements NapileVariable
{
	private NXmlTypeReferenceImpl returnType;
	private boolean mutable;
	private NXmlIdentifierImpl nameIdentifier;

	public NXmlVariableImpl(PsiElement parent, PsiElement mirror)
	{
		super(parent, mirror);
	}

	@Override
	public void setMirror(@NotNull TreeElement element) throws InvalidMirrorException
	{
		NapileVariable mirror = SourceTreeToPsiMap.treeToPsiNotNull(element);

		setMirrorCheckingType(element, null);

		returnType = new NXmlTypeReferenceImpl(this, mirror.getType());
		mutable = mirror.isMutable();
		nameIdentifier = new NXmlIdentifierImpl(this, mirror.getNameIdentifier());
	}

	@NotNull
	@Override
	public FqName getFqName()
	{
		return NapilePsiUtil.getFQNameImpl(this);
	}

	@Nullable
	@Override
	public NapileTypeReference getType()
	{
		return returnType;
	}

	@Nullable
	@Override
	public ASTNode getVarOrValNode()
	{
		return null;
	}

	@Override
	public boolean isMutable()
	{
		return mutable;
	}

	@NotNull
	@Override
	public NapileVariableAccessor[] getAccessors()
	{
		return NapileVariableAccessor.EMPTY_ARRAY;
	}

	@Nullable
	@Override
	public NapileTypeParameterList getTypeParameterList()
	{
		return null;
	}

	@NotNull
	@Override
	public NapileTypeParameter[] getTypeParameters()
	{
		return NapileTypeParameter.EMPTY_ARRAY;
	}

	@NotNull
	@Override
	public Name getNameAsSafeName()
	{
		return Name.identifier(getName());
	}

	@Nullable
	@Override
	public NapileDoc getDocComment()
	{
		return null;
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitVariable(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitVariable(this, data);
	}

	@Nullable
	@Override
	public NapileModifierList getModifierList()
	{
		return null;
	}

	@Override
	public boolean hasModifier(NapileToken modifier)
	{
		return false;
	}

	@Nullable
	@Override
	public ASTNode getModifierNode(NapileToken token)
	{
		return null;
	}

	@Nullable
	@Override
	public Name getNameAsName()
	{
		return getNameAsSafeName();
	}

	@Nullable
	@Override
	public NapileExpression getInitializer()
	{
		return null;
	}

	@Nullable
	@Override
	public PsiElement getNameIdentifier()
	{
		return nameIdentifier;
	}

	@Override
	public String getName()
	{
		return getNameIdentifier().getText();
	}

	@Override
	public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException
	{
		return null;
	}

	@NotNull
	@Override
	public PsiElement[] getChildren()
	{
		return NXmlMirrorUtil.getAllToPsiArray(returnType, nameIdentifier);
	}
}
