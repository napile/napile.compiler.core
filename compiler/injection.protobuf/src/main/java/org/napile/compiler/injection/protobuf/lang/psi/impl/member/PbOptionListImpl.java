package org.napile.compiler.injection.protobuf.lang.psi.impl.member;

import com.intellij.lang.ASTNode;
import org.napile.compiler.injection.protobuf.lang.psi.api.member.PbOptionAssignment;
import org.napile.compiler.injection.protobuf.lang.psi.api.member.PbOptionList;
import org.napile.compiler.injection.protobuf.lang.psi.impl.PbPsiElementImpl;

/**
 * @author Nikolay Matveev
 */
public class PbOptionListImpl extends PbPsiElementImpl implements PbOptionList
{
	public PbOptionListImpl(ASTNode node)
	{
		super(node);
	}

	public PbOptionAssignment[] getOptionAssigments()
	{
		return findChildrenByClass(PbOptionAssignmentImpl.class);
	}
}