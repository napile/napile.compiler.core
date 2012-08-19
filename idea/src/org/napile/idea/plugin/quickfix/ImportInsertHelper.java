/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.napile.idea.plugin.quickfix;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileImportDirective;
import org.napile.compiler.lang.psi.NapilePsiFactory;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.plugin.JetLanguage;
import org.napile.idea.plugin.references.JetPsiReference;
import org.napile.compiler.util.QualifiedNamesUtil;
import org.napile.idea.plugin.project.AnalyzeSingleFileUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

/**
 * @author svtk
 */
public class ImportInsertHelper
{
	private ImportInsertHelper()
	{
	}

	/**
	 * Add import directive corresponding to a type to file when it is needed.
	 *
	 * @param type type to import
	 * @param file file where import directive should be added
	 */
	public static void addImportDirectivesIfNeeded(@NotNull JetType type, @NotNull NapileFile file)
	{
		if(ErrorUtils.isErrorType(type))
		{
			return;
		}
		BindingContext bindingContext = AnalyzeSingleFileUtil.getContextForSingleFile(file);
		PsiElement element = BindingContextUtils.descriptorToDeclaration(bindingContext, type.getMemberScope().getContainingDeclaration());
		if(element != null && element.getContainingFile() == file)
		{ //declaration is in the same file, so no import is needed
			return;
		}
		for(ClassDescriptor clazz : TypeUtils.getAllClassDescriptors(type))
		{
			addImportDirective(DescriptorUtils.getFQName(getTopLevelClass(clazz)).toSafe(), file);
		}
	}

	/**
	 * Add import directive into the PSI tree for the given namespace.
	 *
	 * @param importFqn full name of the import
	 * @param file      File where directive should be added.
	 */
	public static void addImportDirective(@NotNull FqName importFqn, @NotNull NapileFile file)
	{
		addImportDirective(new ImportPath(importFqn, false), null, file);
	}

	public static void addImportDirectiveOrChangeToFqName(@NotNull FqName importFqn, @NotNull NapileFile file, int refOffset, @NotNull PsiElement targetElement)
	{
		PsiReference reference = file.findReferenceAt(refOffset);
		if(reference instanceof JetPsiReference)
		{
			PsiElement target = reference.resolve();
			if(target != null)
			{
				boolean same = file.getManager().areElementsEquivalent(target, targetElement);
				same |= target instanceof PsiClass && importFqn.getFqName().equals(((PsiClass) target).getQualifiedName());
				if(!same)
				{
					Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
					TextRange refRange = reference.getElement().getTextRange();
					document.replaceString(refRange.getStartOffset(), refRange.getEndOffset(), importFqn.getFqName());
				}
				return;
			}
		}
		addImportDirective(new ImportPath(importFqn, false), null, file);
	}

	public static void addImportDirective(@NotNull ImportPath importPath, @Nullable String aliasName, @NotNull NapileFile file)
	{
		if(!doNeedImport(importPath, aliasName, file))
		{
			return;
		}

		NapileImportDirective newDirective = NapilePsiFactory.createImportDirective(file.getProject(), importPath, aliasName);
		List<NapileImportDirective> importDirectives = file.getImportDirectives();

		if(!importDirectives.isEmpty())
		{
			NapileImportDirective lastDirective = importDirectives.get(importDirectives.size() - 1);
			lastDirective.getParent().addAfter(newDirective, lastDirective);
		}
		else
		{
			file.getNamespaceHeader().getParent().addAfter(newDirective, file.getNamespaceHeader());
		}
	}

	/**
	 * Check that import is useless.
	 */
	private static boolean isImportedByDefault(@NotNull ImportPath importPath, @Nullable String aliasName, @NotNull FqName filePackageFqn)
	{
		if(importPath.fqnPart().isRoot())
		{
			return true;
		}

		if(aliasName != null)
		{
			return false;
		}

		// Single element import without .* and alias is useless
		if(!importPath.isAllUnder() && QualifiedNamesUtil.isOneSegmentFQN(importPath.fqnPart()))
		{
			return true;
		}

		// There's no need to import a declaration from the package of current file
		if(!importPath.isAllUnder() && filePackageFqn.equals(importPath.fqnPart().parent()))
		{
			return true;
		}

		if(isDefaultImport(importPath))
			return true;

		return false;
	}

	public static boolean isDefaultImport(ImportPath importPath)
	{
		for(ImportPath defaultJetImport : JetLanguage.DEFAULT_IMPORTS)
		{
			if(QualifiedNamesUtil.isImported(defaultJetImport, importPath))
			{
				return true;
			}
		}
		return false;
	}

	public static boolean doNeedImport(@NotNull ImportPath importPath, @Nullable String aliasName, @NotNull NapileFile file)
	{
		if(isImportedByDefault(importPath, null, NapilePsiUtil.getFQName(file)))
		{
			return false;
		}

		List<NapileImportDirective> importDirectives = file.getImportDirectives();

		if(!importDirectives.isEmpty())
		{
			// Check if import is already present
			for(NapileImportDirective directive : importDirectives)
			{
				ImportPath existentImportPath = NapilePsiUtil.getImportPath(directive);
				if(directive.getAliasName() == null && aliasName == null)
				{
					if(existentImportPath != null && QualifiedNamesUtil.isImported(existentImportPath, importPath))
					{
						return false;
					}
				}
			}
		}

		return true;
	}

	public static ClassDescriptor getTopLevelClass(ClassDescriptor classDescriptor)
	{
		while(true)
		{
			DeclarationDescriptor parent = classDescriptor.getContainingDeclaration();
			if(parent instanceof ClassDescriptor)
			{
				classDescriptor = (ClassDescriptor) parent;
			}
			else
			{
				return classDescriptor;
			}
		}
	}
}
