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

import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.FunctionDescriptor;
import org.napile.compiler.lang.descriptors.NamespaceDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import com.google.common.collect.Maps;

/**
 * @author abreslav
 */
public class LazyPackageMemberScope extends AbstractLazyMemberScope<NamespaceDescriptor, PackageMemberDeclarationProvider>
{

	private final Map<Name, NamespaceDescriptor> packageDescriptors = Maps.newHashMap();

	public LazyPackageMemberScope(@NotNull ResolveSession resolveSession, @NotNull PackageMemberDeclarationProvider declarationProvider, @NotNull NamespaceDescriptor thisPackage)
	{
		super(resolveSession, declarationProvider, thisPackage);
	}

	@Override
	public NamespaceDescriptor getNamespace(@NotNull Name name)
	{
		NamespaceDescriptor known = packageDescriptors.get(name);
		if(known != null)
			return known;
		if(allDescriptorsComputed)
			return null;

		if(!declarationProvider.isPackageDeclared(name))
			return null;

		PackageMemberDeclarationProvider packageMemberDeclarationProvider = resolveSession.getDeclarationProviderFactory().getPackageMemberDeclarationProvider(DescriptorUtils.getFQName(thisDescriptor).child(name).toSafe());
		assert packageMemberDeclarationProvider != null : "Package is declared, but declaration provider is not found: " + name;
		NamespaceDescriptor namespaceDescriptor = new LazyPackageDescriptor(thisDescriptor, name, resolveSession, packageMemberDeclarationProvider);

		packageDescriptors.put(name, namespaceDescriptor);
		allDescriptors.add(namespaceDescriptor);

		return namespaceDescriptor;
	}

	@Override
	public ClassifierDescriptor getClassifier(@NotNull Name name)
	{
		// TODO: creating an FqName every time may be a performance problem
		Name actualName = resolveSession.resolveClassifierAlias(DescriptorUtils.getFQName(thisDescriptor).toSafe(), name);
		return super.getClassifier(actualName);
	}

	@NotNull
	@Override
	protected JetScope getScopeForMemberDeclarationResolution(NapileDeclaration declaration)
	{
		return resolveSession.getInjector().getScopeProvider().getFileScopeForDeclarationResolution((NapileFile) declaration.getContainingFile());
	}

	@NotNull
	@Override
	public ReceiverDescriptor getImplicitReceiver()
	{
		return ReceiverDescriptor.NO_RECEIVER;
	}

	@Override
	protected void getNonDeclaredFunctions(@NotNull Name name, @NotNull Set<FunctionDescriptor> result)
	{
		// No extra functions
	}

	@Override
	protected void getNonDeclaredProperties(@NotNull Name name, @NotNull Set<VariableDescriptor> result)
	{
		// No extra properties
	}

	@Override
	protected void addExtraDescriptors()
	{
		for(FqName packageFqName : declarationProvider.getAllDeclaredPackages())
		{
			getNamespace(packageFqName.shortName());
		}
	}

	@Override
	public String toString()
	{
		// Do not add details here, they may compromise the laziness during debugging
		return "lazy scope for package " + thisDescriptor.getName();
	}
}