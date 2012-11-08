package org.napile.compiler.injection.protobuf.lang.psi.impl.declaration;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.napile.compiler.injection.protobuf.lang.psi.PbPsiElementVisitor;
import org.napile.compiler.injection.protobuf.lang.psi.api.declaration.PbEnumDef;
import org.napile.compiler.injection.protobuf.lang.psi.impl.auxiliary.PbNamedBlockHolderImpl;
import org.napile.compiler.injection.protobuf.lang.psi.utils.PbPsiUtil;

/**
 * @author Nikolay Matveev
 *         Date: Mar 10, 2010
 */
public class PbEnumDefImpl extends PbNamedBlockHolderImpl implements PbEnumDef
{
	public PbEnumDefImpl(ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull PbPsiElementVisitor visitor)
	{
		visitor.visitEnumDefinition(this);
	}

	@Override
	public PsiElement getNameElement()
	{
		return PbPsiUtil.getChild(this, 1, true, true, false);
	}
}
