package org.napile.compiler.injection.protobuf.lang.psi.impl.auxiliary;

import com.intellij.lang.ASTNode;
import org.napile.compiler.injection.protobuf.lang.psi.api.auxiliary.PbBlockHolder;
import org.napile.compiler.injection.protobuf.lang.psi.api.block.PbBlock;

/**
 * @author Nikolay Matveev
 */

public abstract class PbNamedBlockHolderImpl extends PbNamedElementImpl implements PbBlockHolder
{

	protected PbNamedBlockHolderImpl(ASTNode node)
	{
		super(node);
	}

	@Override
	public PbBlock getBlock()
	{
		return findChildByClass(PbBlock.class);
	}
}
