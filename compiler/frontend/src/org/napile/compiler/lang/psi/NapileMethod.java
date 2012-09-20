package org.napile.compiler.lang.psi;

import org.jetbrains.annotations.Nullable;

/**
 * @author Nikolay Krasko
 */
public interface NapileMethod extends NapileTypeParameterListOwner, NapileDeclarationWithBody
{
	@Nullable
	NapileParameterList getValueParameterList();

	@Nullable
	NapileTypeReference getReturnTypeRef();

	boolean isLocal();
}
