package org.napile.compiler.injection.protobuf.lang.psi.api.declaration;

import org.napile.compiler.injection.protobuf.lang.psi.api.PbPsiElement;
import org.napile.compiler.injection.protobuf.lang.psi.api.auxiliary.PbNamedElement;
import org.napile.compiler.injection.protobuf.lang.psi.api.reference.PbRef;

import static org.napile.compiler.injection.protobuf.lang.psi.PbPsiEnums.*;

/**
 * @author Nikolay Matveev
 */
public interface PbFieldDef extends PbPsiElement, PbNamedElement
{

	public FieldLabel getLabel();

	public FieldType getType();

	public FieldType getConcreteType();

	public PbRef getTypeRef();

}
