package org.napile.compiler.injection.protobuf.lang.psi.api;

import com.intellij.psi.PsiFile;
import org.napile.compiler.injection.protobuf.lang.psi.api.declaration.PbImportDef;
import org.napile.compiler.injection.protobuf.lang.psi.api.declaration.PbPackageDef;

/**
 * @author Nikolay Matveev
 */

@Deprecated
public interface PbFile extends PsiFile, PbPsiElement
{

	PbPackageDef getPackageDefinition();

	String getPackageName();

	PbImportDef[] getImportDefinitions();
}
