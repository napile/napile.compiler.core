package org.napile.compiler.injection.protobuf.lang.psi.impl.declaration;

import com.intellij.lang.ASTNode;
import org.napile.compiler.injection.protobuf.lang.psi.api.declaration.PbPackageDef;
import org.napile.compiler.injection.protobuf.lang.psi.api.reference.PbRef;
import org.napile.compiler.injection.protobuf.lang.psi.impl.PbPsiElementImpl;

/**
 * @author Nikolay Matveev
 *         Date: Mar 10, 2010
 */
public class PbPackageDefImpl extends PbPsiElementImpl implements PbPackageDef
{

	public PbPackageDefImpl(ASTNode node)
	{
		super(node);
	}

	@Override
	public String getPackageName()
	{
		final PbRef packageRef = findChildByClass(PbRef.class);
		if(packageRef == null)
		{
			return "";
		}
		return packageRef.getText();
	}
}
