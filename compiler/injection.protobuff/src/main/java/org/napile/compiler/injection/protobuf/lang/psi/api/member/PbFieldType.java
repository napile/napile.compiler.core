package org.napile.compiler.injection.protobuf.lang.psi.api.member;

import org.napile.compiler.injection.protobuf.lang.psi.api.PbPsiElement;
import org.napile.compiler.injection.protobuf.lang.psi.api.reference.PbRef;

import static org.napile.compiler.injection.protobuf.lang.psi.PbPsiEnums.*;

/**
 * @author Nikolay Matveev
 *         Date: Mar 12, 2010
 */
public interface PbFieldType extends PbPsiElement
{

	FieldType getType();

	PbRef getTypeRef();

}
