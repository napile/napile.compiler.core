package org.napile.compiler.injection.protobuf.lang.psi.api.member;

import org.napile.compiler.injection.protobuf.lang.psi.PbPsiEnums;
import org.napile.compiler.injection.protobuf.lang.psi.api.PbPsiElement;

/**
 * @author Nikolay Matveev
 *         Date: Mar 12, 2010
 */
public interface PbOptionAssignment extends PbPsiElement
{

	PbPsiEnums.OptionType getType();

	String getOptionName();

	String getOptionValue();
}
