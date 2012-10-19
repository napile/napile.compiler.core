package org.napile.compiler.injection.lexer;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.napile.compiler.injection.CodeInjection;
import org.napile.compiler.lang.lexer.NapileKeywordToken;

/**
 * @author VISTALL
 * @date 10:36/28.09.12
 */
public class NapileInjectionKeywordToken extends NapileKeywordToken
{
	@NotNull
	public final CodeInjection codeInjection;

	public NapileInjectionKeywordToken(@NotNull @NonNls String value, @NotNull CodeInjection codeInjection)
	{
		super(value, true);
		this.codeInjection = codeInjection;
	}
}
