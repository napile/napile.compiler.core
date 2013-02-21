package org.napile.compiler.lang.psi;

import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @date 20:01/20.02.13
 */
public interface NapileConstructorCalleeExpression extends NapileExpression
{
	@Nullable
	@IfNotParsed
	NapileTypeReference getTypeReference();

	@Nullable
	@IfNotParsed
	@Deprecated
	NapileReferenceExpression getConstructorReferenceExpression();
}
