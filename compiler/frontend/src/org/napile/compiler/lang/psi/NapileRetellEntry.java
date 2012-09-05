package org.napile.compiler.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.stubs.NapilePsiRetellEntryStub;
import org.napile.compiler.lang.psi.stubs.elements.JetStubElementTypes;
import com.intellij.lang.ASTNode;

/**
 * @author VISTALL
 * @date 23:41/05.09.12
 */
public class NapileRetellEntry extends NapileNamedDeclarationStub<NapilePsiRetellEntryStub>
{
	public NapileRetellEntry(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileRetellEntry(@NotNull NapilePsiRetellEntryStub stub)
	{
		super(stub, JetStubElementTypes.RETELL_ENTRY);
	}

	@Override
	public void accept(@NotNull NapileVisitorVoid visitor)
	{
		visitor.visitRetellEntry(this);
	}

	@Override
	public <R, D> R accept(@NotNull NapileVisitor<R, D> visitor, D data)
	{
		return visitor.visitRetellEntry(this, data);
	}

	public NapileExpression getExpression()
	{
		return findChildByClass(NapileExpression.class);
	}
}
