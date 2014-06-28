package org.napile.compiler.injection.protobuf.lang.psi.api.reference;

import org.napile.compiler.injection.protobuf.lang.psi.PbPsiEnums;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiQualifiedReference;
import com.intellij.psi.PsiReference;

/**
 * @author Nikolay Matveev
 *         Date: Mar 30, 2010
 */
public interface PbRef extends PsiElement, PsiReference, PsiQualifiedReference
{

	String getReferenceName();

	PbPsiEnums.ReferenceKind getRefKind();

	PbPsiEnums.CompletionKind getCompletionKind();

	boolean isLastInChainReference();

	PsiElement getReferenceNameElement();
}
