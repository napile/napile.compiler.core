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

package org.napile.compiler.lang.resolve;

import static org.napile.compiler.lang.resolve.BindingTraceKeys.NAMESPACE_TO_FILES;
import static org.napile.compiler.lang.resolve.BindingTraceKeys.REFERENCE_TARGET;
import static org.napile.compiler.lang.resolve.BindingTraceKeys.RESOLUTION_SCOPE;

import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.FqNameUnsafe;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ModuleDescriptor;
import org.napile.compiler.lang.descriptors.NamespaceDescriptorParent;
import org.napile.compiler.lang.descriptors.PackageDescriptor;
import org.napile.compiler.lang.descriptors.PackageDescriptorImpl;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapilePackage;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.psi.NapileReferenceExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.RedeclarationHandler;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;

/**
 * @author Stepan Koltsov
 */
public class NamespaceFactoryImpl implements NamespaceFactory
{
	private ModuleDescriptor moduleDescriptor;
	private BindingTrace trace;

	@Inject
	public void setModuleDescriptor(ModuleDescriptor moduleDescriptor)
	{
		this.moduleDescriptor = moduleDescriptor;
	}

	@Inject
	public void setTrace(BindingTrace trace)
	{
		this.trace = trace;
	}

	@NotNull
	public PackageDescriptorImpl createNamespaceDescriptorPathIfNeeded(@NotNull NapileFile file, @NotNull JetScope outerScope, @NotNull RedeclarationHandler handler)
	{
		NapilePackage filePackage = file.getPackage();

		if(moduleDescriptor.getRootNamespaceDescriptorImpl() == null)
		{
			createRootNamespaceDescriptorIfNeeded(null, moduleDescriptor, null, handler);
		}

		PackageDescriptorImpl currentOwner = moduleDescriptor.getRootNamespaceDescriptorImpl();
		if(currentOwner == null)
		{
			throw new IllegalStateException("must be initialized 5 lines above");
		}

		for(NapileSimpleNameExpression nameExpression : filePackage.getParentNamespaceNames())
		{
			Name namespaceName = NapilePsiUtil.safeName(nameExpression.getReferencedName());

			PackageDescriptorImpl namespaceDescriptor = createNamespaceDescriptorIfNeeded(null, currentOwner, namespaceName, nameExpression, handler);

			trace.record(BindingTraceKeys.NAMESPACE_IS_SRC, namespaceDescriptor, true);
			trace.record(RESOLUTION_SCOPE, nameExpression, outerScope);

			outerScope = namespaceDescriptor.getMemberScope();
			currentOwner = namespaceDescriptor;
		}

		PackageDescriptorImpl namespaceDescriptor;
		Name name;
		if(filePackage.getLastPartExpression() == null)
		{
			// previous call to createRootNamespaceDescriptorIfNeeded couldn't store occurrence for current file.
			namespaceDescriptor = moduleDescriptor.getRootNamespaceDescriptorImpl();
			storeBindingForFileAndExpression(file, null, namespaceDescriptor);
		}
		else
		{
			name = filePackage.getNameAsName();
			namespaceDescriptor = createNamespaceDescriptorIfNeeded(file, currentOwner, name, filePackage.getLastPartExpression(), handler);

			trace.record(BindingTraceKeys.NAMESPACE_IS_SRC, namespaceDescriptor, true);
			trace.record(RESOLUTION_SCOPE, filePackage, outerScope);
		}

		return namespaceDescriptor;
	}

	@Override
	@NotNull
	public PackageDescriptorImpl createNamespaceDescriptorPathIfNeeded(@NotNull FqName fqName)
	{
		PackageDescriptorImpl owner = null;
		for(FqName pathElement : fqName.path())
		{
			if(pathElement.isRoot())
			{
				owner = createRootNamespaceDescriptorIfNeeded(null, moduleDescriptor, null, RedeclarationHandler.DO_NOTHING);
			}
			else
			{
				assert owner != null : "Should never be null as first element in the path must be root";
				owner = createNamespaceDescriptorIfNeeded(null, owner, pathElement.shortName(), null, RedeclarationHandler.DO_NOTHING);
			}
		}

		assert owner != null : "Should never be null as first element in the path must be root";
		return owner;
	}

	private PackageDescriptorImpl createRootNamespaceDescriptorIfNeeded(@Nullable NapileFile file, @NotNull ModuleDescriptor owner, @Nullable NapileReferenceExpression expression, @NotNull RedeclarationHandler handler)
	{
		FqName fqName = FqName.ROOT;
		PackageDescriptorImpl namespaceDescriptor = owner.getRootNamespaceDescriptorImpl();

		if(namespaceDescriptor == null)
		{
			namespaceDescriptor = createNewNamespaceDescriptor(owner, FqNameUnsafe.ROOT_NAME, expression, handler, fqName);
		}

		storeBindingForFileAndExpression(file, expression, namespaceDescriptor);

		return namespaceDescriptor;
	}

	@NotNull
	private PackageDescriptorImpl createNamespaceDescriptorIfNeeded(@Nullable NapileFile file, @NotNull PackageDescriptorImpl owner, @NotNull Name name, @Nullable NapileReferenceExpression expression, @NotNull RedeclarationHandler handler)
	{
		FqName ownerFqName = DescriptorUtils.getFQName(owner).toSafe();
		FqName fqName = ownerFqName.child(name);
		// !!!
		PackageDescriptorImpl namespaceDescriptor = (PackageDescriptorImpl) owner.getMemberScope().getDeclaredNamespace(name);

		if(namespaceDescriptor == null)
		{
			namespaceDescriptor = createNewNamespaceDescriptor(owner, name, expression, handler, fqName);
		}

		storeBindingForFileAndExpression(file, expression, namespaceDescriptor);

		return namespaceDescriptor;
	}

	private PackageDescriptorImpl createNewNamespaceDescriptor(NamespaceDescriptorParent owner, Name name, PsiElement expression, RedeclarationHandler handler, FqName fqName)
	{
		PackageDescriptorImpl namespaceDescriptor;
		namespaceDescriptor = new PackageDescriptorImpl(owner, Collections.<AnnotationDescriptor>emptyList(), // TODO: annotations
				name);

		WritableScopeImpl scope = new WritableScopeImpl(JetScope.EMPTY, namespaceDescriptor, handler, "Namespace member scope");
		scope.changeLockLevel(WritableScope.LockLevel.BOTH);

		namespaceDescriptor.initialize(scope);
		scope.changeLockLevel(WritableScope.LockLevel.BOTH);

		owner.addNamespace(namespaceDescriptor);
		if(expression != null)
		{
			trace.record(BindingTraceKeys.PACKAGE, expression, namespaceDescriptor);
		}
		return namespaceDescriptor;
	}

	private void storeBindingForFileAndExpression(@Nullable NapileFile file, @Nullable NapileReferenceExpression expression, @NotNull PackageDescriptor packageDescriptor)
	{
		if(expression != null)
		{
			trace.record(REFERENCE_TARGET, expression, packageDescriptor);
		}

		if(file != null)
		{
			trace.record(BindingTraceKeys.FILE_TO_NAMESPACE, file, packageDescriptor);

			// Register files corresponding to this namespace
			// The trace currently does not support bi-di multimaps that would handle this task nicer
			Collection<NapileFile> files = trace.get(NAMESPACE_TO_FILES, packageDescriptor);
			if(files == null)
			{
				files = Sets.newIdentityHashSet();
			}
			files.add(file);
			trace.record(BindingTraceKeys.NAMESPACE_TO_FILES, packageDescriptor, files);
		}
	}
}
