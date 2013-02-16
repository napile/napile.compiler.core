package org.napile.compiler.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.doc.lang.psi.NapileDoc;
import com.intellij.psi.PsiNameIdentifierOwner;

/**
 * @author Nikolay Krasko
 */
public interface NapileNamedDeclaration extends NapileDeclaration, PsiNameIdentifierOwner, NapileStatementExpression, NapileNamed
{
	@NotNull
	Name getNameAsSafeName();

	@Nullable
	NapileDoc getDocComment();
}
