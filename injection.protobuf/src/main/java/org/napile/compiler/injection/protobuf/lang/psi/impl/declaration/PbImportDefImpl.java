package org.napile.compiler.injection.protobuf.lang.psi.impl.declaration;

import org.jetbrains.annotations.NotNull;
import com.intellij.lang.ASTNode;
import org.napile.compiler.injection.protobuf.lang.psi.PbPsiElementVisitor;
import org.napile.compiler.injection.protobuf.lang.psi.api.PbFile;
import org.napile.compiler.injection.protobuf.lang.psi.api.declaration.PbImportDef;
import org.napile.compiler.injection.protobuf.lang.psi.api.reference.PbRef;
import org.napile.compiler.injection.protobuf.lang.psi.impl.PbPsiElementImpl;

/**
 * @author Nikolay Matveev
 *         Date: Mar 10, 2010
 */
public class PbImportDefImpl extends PbPsiElementImpl implements PbImportDef
{

	public PbImportDefImpl(ASTNode node)
	{
		super(node);
	}

	@Override
	public void accept(@NotNull PbPsiElementVisitor visitor)
	{
		visitor.visitImportDefinition(this);
	}

	@Override
	public PbFile getAliasedFile()
	{
		PbRef ref = this.findChildByClass(PbRef.class);
		return ref != null ? (PbFile) ref.resolve() : null;
	}
}
