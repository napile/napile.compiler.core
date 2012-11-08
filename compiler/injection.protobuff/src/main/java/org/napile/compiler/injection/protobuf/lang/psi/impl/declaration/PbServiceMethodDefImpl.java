package org.napile.compiler.injection.protobuf.lang.psi.impl.declaration;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.napile.compiler.injection.protobuf.lang.psi.PbPsiElementVisitor;
import org.napile.compiler.injection.protobuf.lang.psi.api.declaration.PbServiceMethodDef;
import org.napile.compiler.injection.protobuf.lang.psi.impl.auxiliary.PbNamedBlockHolderImpl;
import org.napile.compiler.injection.protobuf.lang.psi.utils.PbPsiUtil;

/**
 * @author Nikolay Matveev
 *         Date: Mar 24, 2010
 */
public class PbServiceMethodDefImpl extends PbNamedBlockHolderImpl implements PbServiceMethodDef
{
	public PbServiceMethodDefImpl(ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull PbPsiElementVisitor visitor)
	{
		visitor.visitServiceMethodDefinition(this);
	}

	@Override
	public PsiElement getNameElement()
	{
		return PbPsiUtil.getChild(this, 1, true, true, false);
	}
}
