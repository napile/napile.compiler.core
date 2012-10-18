/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.napile.idea.plugin.editor.importOptimizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.ImportPath;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.psi.NapileImportDirective;
import org.napile.compiler.lang.psi.NapileNamedDeclaration;
import org.napile.compiler.lang.psi.NapilePackageImpl;
import org.napile.compiler.lang.psi.NapilePsiFactory;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.psi.NapileFile;
import org.napile.compiler.util.QualifiedNamesUtil;
import org.napile.idea.plugin.quickfix.ImportInsertHelper;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.TokenType;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author Nikolay Krasko
 */
public class JetImportOptimizer implements ImportOptimizer
{
	@Override
	public boolean supports(PsiFile file)
	{
		return file instanceof NapileFile;
	}

	@NotNull
	@Override
	public Runnable processFile(final PsiFile file)
	{
		return new Runnable()
		{

			@Override
			public void run()
			{
				final NapileFile jetFile = (NapileFile) file;
				final Set<FqName> usedQualifiedNames = extractUsedQualifiedNames(jetFile);

				final List<NapileImportDirective> sortedDirectives = jetFile.getImportDirectives();
				Collections.sort(sortedDirectives, new Comparator<NapileImportDirective>()
				{
					@Override
					public int compare(NapileImportDirective directive1, NapileImportDirective directive2)
					{
						ImportPath firstPath = NapilePsiUtil.getImportPath(directive1);
						ImportPath secondPath = NapilePsiUtil.getImportPath(directive2);

						if(firstPath == null || secondPath == null)
						{
							return firstPath == null && secondPath == null ? 0 : firstPath == null ? -1 : 1;
						}

						// import bla.bla.bla.* should be before import bla.bla.bla.something
						if(firstPath.isAllUnder() &&
								!secondPath.isAllUnder() &&
								firstPath.fqnPart().equals(secondPath.fqnPart().parent()))
						{
							return -1;
						}

						if(!firstPath.isAllUnder() &&
								secondPath.isAllUnder() &&
								secondPath.fqnPart().equals(firstPath.fqnPart().parent()))
						{
							return 1;
						}

						return firstPath.getPathStr().compareTo(secondPath.getPathStr());
					}
				});

				ApplicationManager.getApplication().runWriteAction(new Runnable()
				{
					@Override
					public void run()
					{
						// Remove imports
						List<NapileImportDirective> imports = jetFile.getImportDirectives();
						if(!imports.isEmpty())
						{
							PsiElement firstForDelete = getWithPreviousWhitespaces(imports.get(0));
							PsiElement lastForDelete = getWithFollowedWhitespaces(imports.get(imports.size() - 1));

							// Should be found before deletion
							PsiElement elementBeforeImports = firstForDelete.getPrevSibling();

							jetFile.deleteChildRange(firstForDelete, lastForDelete);

							if(elementBeforeImports != null)
							{
								jetFile.addAfter(NapilePsiFactory.createWhiteSpace(jetFile.getProject(), "\n"), elementBeforeImports);
							}
						}

						// Insert back only necessary imports in correct order
						for(NapileImportDirective anImport : sortedDirectives)
						{
							ImportPath importPath = NapilePsiUtil.getImportPath(anImport);
							if(importPath == null)
							{
								continue;
							}

							if(isUseful(importPath, anImport.getAliasName(), usedQualifiedNames))
							{
								ImportInsertHelper.addImportDirective(importPath, anImport.getAliasName(), jetFile);
							}
						}
					}
				});
			}
		};
	}

	public static boolean isUseful(ImportPath importPath, @Nullable String aliasName, Collection<FqName> usedNames)
	{
		if(aliasName != null)
		{
			// TODO: Add better analysis for aliases
			return true;
		}

		for(FqName usedName : usedNames)
		{
			if(QualifiedNamesUtil.isImported(importPath, usedName))
			{
				return true;
			}
		}

		return false;
	}

	public static Set<FqName> extractUsedQualifiedNames(final NapileFile jetFile)
	{
		final Set<FqName> usedQualifiedNames = new HashSet<FqName>();
		jetFile.accept(new NapileVisitorVoid()
		{
			@Override
			public void visitElement(PsiElement element)
			{
				ProgressIndicatorProvider.checkCanceled();
				element.acceptChildren(this);
			}

			@Override
			public void visitReferenceExpression(NapileReferenceExpression expression)
			{
				if(PsiTreeUtil.getParentOfType(expression, NapileImportDirective.class) == null && PsiTreeUtil.getParentOfType(expression, NapilePackageImpl.class) == null)
				{

					PsiReference reference = expression.getReference();
					if(reference != null)
					{
						List<PsiElement> references = new ArrayList<PsiElement>();
						PsiElement resolve = reference.resolve();
						if(resolve != null)
						{
							references.add(resolve);
						}

						if(references.isEmpty() && reference instanceof PsiPolyVariantReference)
						{
							for(ResolveResult resolveResult : ((PsiPolyVariantReference) reference).multiResolve(true))
							{
								references.add(resolveResult.getElement());
							}
						}

						for(PsiElement psiReference : references)
						{
							FqName fqName = getElementUsageFQName(psiReference);
							if(fqName != null)
							{
								usedQualifiedNames.add(fqName);
							}
						}
					}
				}

				super.visitReferenceExpression(expression);
			}
		});

		return usedQualifiedNames;
	}


	@Nullable
	public static FqName getElementUsageFQName(PsiElement element)
	{
		if(element instanceof NapileFile)
		{
			return NapilePsiUtil.getFQName((NapileFile) element);
		}

		if(element instanceof NapileNamedDeclaration)
		{
			return NapilePsiUtil.getFQName((NapileNamedDeclaration) element);
		}

		if(element instanceof PsiClass)
		{
			String qualifiedName = ((PsiClass) element).getQualifiedName();
			if(qualifiedName != null)
			{
				return new FqName(qualifiedName);
			}
		}

		// TODO: Still problem with kotlin global properties imported from class files
		if(element instanceof PsiMethod)
		{
			PsiMethod method = (PsiMethod) element;

			PsiClass containingClass = method.getContainingClass();

			if(containingClass != null)
			{
				String classFQNStr = containingClass.getQualifiedName();
				if(classFQNStr != null)
				{
					if(method.isConstructor())
					{
						return new FqName(classFQNStr);
					}

					FqName classFQN = new FqName(classFQNStr);
					return QualifiedNamesUtil.combine(classFQN, Name.identifier(method.getName()));
				}
			}
		}

		if(element instanceof PsiPackage)
		{
			return new FqName(((PsiPackage) element).getQualifiedName());
		}

		return null;
	}

	private static PsiElement getWithPreviousWhitespaces(PsiElement element)
	{
		PsiElement result = element;

		PsiElement siblingIterator = element.getPrevSibling();
		while(siblingIterator != null)
		{
			if(siblingIterator.getNode().getElementType() != TokenType.WHITE_SPACE)
			{
				break;
			}
			else
			{
				result = siblingIterator;
			}

			siblingIterator = siblingIterator.getPrevSibling();
		}

		return result;
	}

	private static PsiElement getWithFollowedWhitespaces(PsiElement element)
	{
		PsiElement result = element;

		PsiElement siblingIterator = element.getNextSibling();
		while(siblingIterator != null)
		{
			if(siblingIterator.getNode().getElementType() != TokenType.WHITE_SPACE)
			{
				break;
			}
			else
			{
				result = siblingIterator;
			}

			siblingIterator = siblingIterator.getNextSibling();
		}

		return result;
	}
}
