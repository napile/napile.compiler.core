package org.napile.compiler.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @date 19:26/20.02.13
 */
public interface NapileAnnotation extends NapileCallElement
{
	@Nullable
	@IfNotParsed
	NapileTypeReference getTypeReference();

	@Override
	@NotNull
	NapileConstructorCalleeExpression getCalleeExpression();
}
