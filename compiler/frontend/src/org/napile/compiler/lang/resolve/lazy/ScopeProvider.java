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

package org.napile.compiler.lang.resolve.lazy;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.NamespaceDescriptor;
import org.napile.compiler.lang.psi.NapileClassOrObject;
import org.napile.compiler.lang.psi.NapileClassObject;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileNamespaceHeader;
import org.napile.compiler.lang.resolve.ImportsResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.RedeclarationHandler;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.psi.NapileEnumEntry;
import org.napile.compiler.lang.psi.NapileImportDirective;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author abreslav
 */
public class ScopeProvider
{

	private final ResolveSession resolveSession;

	public ScopeProvider(@NotNull ResolveSession resolveSession)
	{
		this.resolveSession = resolveSession;
	}

	// This scope does not contain imported functions
	@NotNull
	public JetScope getFileScopeForDeclarationResolution(NapileFile file)
	{
		// package
		NapileNamespaceHeader header = file.getNamespaceHeader();
		if(header == null)
		{
			throw new IllegalArgumentException("Scripts are not supported: " + file.getName());
		}

		FqName fqName = new FqName(header.getQualifiedName());
		NamespaceDescriptor packageDescriptor = resolveSession.getPackageDescriptorByFqName(fqName);

		if(packageDescriptor == null)
		{
			throw new IllegalStateException("Package not found: " + fqName + " maybe the file is not in scope of this resolve session: " + file.getName());
		}

		WritableScope writableScope = new WritableScopeImpl(JetScope.EMPTY, packageDescriptor, RedeclarationHandler.DO_NOTHING, "File scope for declaration resolution");
		writableScope.changeLockLevel(WritableScope.LockLevel.BOTH);

		NamespaceDescriptor rootPackageDescriptor = resolveSession.getPackageDescriptorByFqName(FqName.ROOT);
		if(rootPackageDescriptor == null)
		{
			throw new IllegalStateException("Root package not found");
		}

		// Don't import twice
		if(!packageDescriptor.getQualifiedName().equals(FqName.ROOT))
		{
			writableScope.importScope(rootPackageDescriptor.getMemberScope());
		}

		List<NapileImportDirective> importDirectives = getFileImports(file);

		ImportsResolver.processImportsInFile(true, writableScope, importDirectives, rootPackageDescriptor.getMemberScope(), resolveSession.getTrace(), resolveSession.getInjector().getQualifiedExpressionResolver(), file.getProject());

		writableScope.importScope(packageDescriptor.getMemberScope());

		writableScope.changeLockLevel(WritableScope.LockLevel.READING);
		// TODO: Cache
		return writableScope;
	}

	public static List<NapileImportDirective> getFileImports(NapileFile file)
	{
		List<NapileImportDirective> fileImports = file.getImportDirectives();
		List<NapileImportDirective> importDirectives = Lists.newArrayList();

		importDirectives.addAll(fileImports);
		return importDirectives;
	}

	@NotNull
	public JetScope getResolutionScopeForDeclaration(@NotNull NapileDeclaration jetDeclaration)
	{
		PsiElement immediateParent = jetDeclaration.getParent();
		if(immediateParent instanceof NapileFile)
		{
			return getFileScopeForDeclarationResolution((NapileFile) immediateParent);
		}

		NapileDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(jetDeclaration, NapileDeclaration.class);
		if(parentDeclaration instanceof NapileClassOrObject)
		{
			NapileClassOrObject classOrObject = (NapileClassOrObject) parentDeclaration;
			LazyClassDescriptor classDescriptor = (LazyClassDescriptor) resolveSession.getClassDescriptor(classOrObject);
			if(jetDeclaration instanceof NapileEnumEntry)
			{
				return ((LazyClassDescriptor) classDescriptor.getClassObjectDescriptor()).getScopeForMemberDeclarationResolution();
			}
			return classDescriptor.getScopeForMemberDeclarationResolution();
		}
		else if(parentDeclaration instanceof NapileClassObject)
		{
			NapileClassObject classObject = (NapileClassObject) parentDeclaration;
			LazyClassDescriptor classObjectDescriptor = resolveSession.getClassObjectDescriptor(classObject);
			return classObjectDescriptor.getScopeForMemberDeclarationResolution();
		}
		else
		{
			throw new IllegalStateException("Don't call this method for local declarations: " + jetDeclaration + " " + jetDeclaration.getText());
		}
	}
}
