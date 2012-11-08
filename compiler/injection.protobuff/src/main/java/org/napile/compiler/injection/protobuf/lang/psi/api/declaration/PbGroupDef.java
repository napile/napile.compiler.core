package org.napile.compiler.injection.protobuf.lang.psi.api.declaration;

import org.napile.compiler.injection.protobuf.lang.psi.api.PbPsiElement;
import org.napile.compiler.injection.protobuf.lang.psi.api.auxiliary.PbBlockHolder;
import org.napile.compiler.injection.protobuf.lang.psi.api.auxiliary.PbNamedElement;

/**
 * @author Nikolay Matveev
 *         Date: Apr 3, 2010
 */

public interface PbGroupDef extends PbPsiElement, PbNamedElement, PbBlockHolder
{

	String getFieldName();
}
