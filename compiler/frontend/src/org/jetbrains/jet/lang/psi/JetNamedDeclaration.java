package org.jetbrains.jet.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.Name;
import com.intellij.psi.PsiNameIdentifierOwner;

/**
 * @author Nikolay Krasko
 */
public interface JetNamedDeclaration extends JetDeclaration, PsiNameIdentifierOwner, JetStatementExpression, JetNamed
{
	@NotNull
	Name getNameAsSafeName();

	boolean isScriptDeclaration();

	@Nullable
	JetScript getScript();
}
