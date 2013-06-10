package org.napile.compiler.injection.protobuf.lang.parser;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.injection.protobuf.lang.parser.parsing.CompilationUnit;
import org.napile.compiler.injection.protobuf.lang.parser.util.PbPatchedPsiBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageVersion;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;

/**
 * @author Nikolay Matveev
 *         Date: Mar 7, 2010
 */
public class PbParser implements PsiParser
{
	@NotNull
	@Override
	public ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder, @NotNull LanguageVersion languageVersion)
	{
		builder.setDebugMode(true);
		PsiBuilder.Marker rootMarker = builder.mark();
		CompilationUnit.parse(new PbPatchedPsiBuilder(builder));
		rootMarker.done(root);
		return builder.getTreeBuilt();
	}
}
