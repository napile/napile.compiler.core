package org.napile.compiler.injection.protobuf.lang.psi.impl.declaration;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.napile.compiler.injection.protobuf.lang.psi.PbPsiElementVisitor;
import org.napile.compiler.injection.protobuf.lang.psi.api.block.PbBlock;
import org.napile.compiler.injection.protobuf.lang.psi.api.declaration.PbExtendDef;
import org.napile.compiler.injection.protobuf.lang.psi.api.reference.PbRef;
import org.napile.compiler.injection.protobuf.lang.psi.impl.PbPsiElementImpl;

/**
 * @author Nikolay Matveev
 */


public class PbExtendDefImpl extends PbPsiElementImpl implements PbExtendDef
{
	public PbExtendDefImpl(ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull PbPsiElementVisitor visitor)
	{
		visitor.visitExtendDefinition(this);
	}

	@Override
	public PbRef getTypeRef()
	{
		return findChildByClass(PbRef.class);
	}

	@Override
	public PbBlock getBlock()
	{
		return findChildByClass(PbBlock.class);
	}
}
