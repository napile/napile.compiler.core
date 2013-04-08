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

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.PackageDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;

/**
 * Introduces a simple wrapper for internal scope.
 *
 * @author abreslav
 */
public abstract class AbstractScopeAdapter implements JetScope
{
	@NotNull
	protected abstract JetScope getWorkerScope();

	@NotNull
	@Override
	public ReceiverDescriptor getImplicitReceiver()
	{
		return getWorkerScope().getImplicitReceiver();
	}

	@Override
	public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result)
	{
		getWorkerScope().getImplicitReceiversHierarchy(result);
	}

	@NotNull
	@Override
	public Collection<MethodDescriptor> getMethods(@NotNull Name name)
	{
		return getWorkerScope().getMethods(name);
	}

	@Override
	public PackageDescriptor getPackage(@NotNull Name name)
	{
		return getWorkerScope().getPackage(name);
	}

	@Override
	public ClassDescriptor getClass(@NotNull FqName name)
	{
		return getWorkerScope().getClass(name);
	}

	@Override
	public ClassifierDescriptor getClassifier(@NotNull Name name)
	{
		return getWorkerScope().getClassifier(name);
	}

	@Override
	public ClassDescriptor getObjectDescriptor(@NotNull Name name)
	{
		return getWorkerScope().getObjectDescriptor(name);
	}

	@NotNull
	@Override
	public Collection<ClassDescriptor> getObjectDescriptors()
	{
		return getWorkerScope().getObjectDescriptors();
	}

	@NotNull
	@Override
	public Collection<VariableDescriptor> getVariables(@NotNull Name name)
	{
		return getWorkerScope().getVariables(name);
	}

	@Override
	public VariableDescriptor getLocalVariable(@NotNull Name name)
	{
		return getWorkerScope().getLocalVariable(name);
	}

	@NotNull
	@Override
	public DeclarationDescriptor getContainingDeclaration()
	{
		return getWorkerScope().getContainingDeclaration();
	}

	@NotNull
	@Override
	public Collection<DeclarationDescriptor> getAllDescriptors()
	{
		return getWorkerScope().getAllDescriptors();
	}

	@NotNull
	@Override
	public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors()
	{
		return getWorkerScope().getOwnDeclaredDescriptors();
	}
}
