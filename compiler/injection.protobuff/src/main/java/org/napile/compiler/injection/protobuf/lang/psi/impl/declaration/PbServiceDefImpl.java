package org.napile.compiler.injection.protobuf.lang.psi.impl.declaration;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.napile.compiler.injection.protobuf.lang.psi.PbPsiElementVisitor;
import org.napile.compiler.injection.protobuf.lang.psi.api.declaration.PbServiceDef;
import org.napile.compiler.injection.protobuf.lang.psi.impl.auxiliary.PbNamedBlockHolderImpl;
import org.napile.compiler.injection.protobuf.lang.psi.utils.PbPsiUtil;

/**
 * @author Nikolay Matveev
 */
public class PbServiceDefImpl extends PbNamedBlockHolderImpl implements PbServiceDef
{
	public PbServiceDefImpl(ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull PbPsiElementVisitor visitor)
	{
		visitor.visitServiceDefinition(this);
	}

	@Override
	public PsiElement getNameElement()
	{
		return PbPsiUtil.getChild(this, 1, true, true, false);
	}
}
