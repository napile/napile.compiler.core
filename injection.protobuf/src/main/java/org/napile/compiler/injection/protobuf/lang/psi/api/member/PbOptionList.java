package org.napile.compiler.injection.protobuf.lang.psi.api.member;

import org.napile.compiler.injection.protobuf.lang.psi.api.PbPsiElement;

/**
 * @author Nikolay Matveev
 *         Date: Mar 12, 2010
 */
public interface PbOptionList extends PbPsiElement
{

	public PbOptionAssignment[] getOptionAssigments();
}
