package org.napile.compiler.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.psi.stubs.NapilePsiRetellEntryStub;
import org.napile.compiler.lang.psi.stubs.elements.NapileStubElementTypes;
import com.intellij.lang.ASTNode;

/**
 * @author VISTALL
 * @date 23:41/05.09.12
 */
public class NapileRetellEntryImpl extends NapileNamedDeclarationStub<NapilePsiRetellEntryStub> implements NapileRetellEntry
{
	public NapileRetellEntryImpl(@NotNull ASTNode node)
	{
		super(node);
	}

	public NapileRetellEntryImpl(@NotNull NapilePsiRetellEntryStub stub)
	{
		super(stub, NapileStubElementTypes.RETELL_ENTRY);
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

	@Override
	@Nullable
	public NapileExpression getExpression()
	{
		return findChildByClass(NapileExpression.class);
	}
}
