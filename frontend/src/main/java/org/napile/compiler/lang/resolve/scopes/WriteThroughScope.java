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

package org.napile.compiler.lang.resolve.scopes;

import java.util.Collection;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.PackageDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * @author abreslav
 */
public class WriteThroughScope extends WritableScopeWithImports
{
	private final WritableScope writableWorker;
	private Collection<DeclarationDescriptor> allDescriptors;

	public WriteThroughScope(@NotNull NapileScope outerScope, @NotNull WritableScope scope, @NotNull RedeclarationHandler redeclarationHandler, @NotNull String debugName)
	{
		super(outerScope, redeclarationHandler, debugName);
		this.writableWorker = scope;
	}

	@Override
	@NotNull
	public DeclarationDescriptor getContainingDeclaration()
	{
		checkMayRead();

		return writableWorker.getContainingDeclaration();
	}

	@Override
	@NotNull
	public ReceiverDescriptor getImplicitReceiver()
	{
		checkMayRead();

		return writableWorker.getImplicitReceiver();
	}

	@Override
	@NotNull
	public Collection<MethodDescriptor> getMethods(@NotNull Name name)
	{
		checkMayRead();

		Set<MethodDescriptor> result = Sets.newLinkedHashSet();

		result.addAll(writableWorker.getMethods(name));

		result.addAll(getWorkerScope().getMethods(name));

		result.addAll(super.getMethods(name)); // Imports

		return result;
	}

	@Override
	@NotNull
	public Set<VariableDescriptor> getVariables(@NotNull Name name)
	{
		checkMayRead();

		Set<VariableDescriptor> properties = Sets.newLinkedHashSet();
		properties.addAll(writableWorker.getVariables(name));
		properties.addAll(getWorkerScope().getVariables(name));
		properties.addAll(super.getVariables(name)); //imports
		return properties;
	}

	@Override
	@Nullable
	public VariableDescriptor getLocalVariable(@NotNull Name name)
	{
		checkMayRead();

		VariableDescriptor variable = writableWorker.getLocalVariable(name);
		if(variable != null)
			return variable;

		variable = getWorkerScope().getLocalVariable(name);
		if(variable != null)
			return variable;

		return super.getLocalVariable(name); // Imports
	}

	@Override
	@Nullable
	public PackageDescriptor getPackage(@NotNull Name name)
	{
		checkMayRead();

		PackageDescriptor namespace = writableWorker.getPackage(name);
		if(namespace != null)
			return namespace;

		namespace = getWorkerScope().getPackage(name);
		if(namespace != null)
			return namespace;

		return super.getPackage(name); // Imports
	}

	@Override
	@Nullable
	public ClassifierDescriptor getClassifier(@NotNull Name name)
	{
		checkMayRead();

		ClassifierDescriptor classifier = writableWorker.getClassifier(name);
		if(classifier != null)
			return classifier;

		classifier = getWorkerScope().getClassifier(name);
		if(classifier != null)
			return classifier;

		return super.getClassifier(name); // Imports
	}

	@Override
	public ClassDescriptor getObjectDescriptor(@NotNull Name name)
	{
		checkMayRead();

		ClassDescriptor objectDescriptor = writableWorker.getObjectDescriptor(name);
		if(objectDescriptor != null)
			return objectDescriptor;

		objectDescriptor = getWorkerScope().getObjectDescriptor(name);
		if(objectDescriptor != null)
			return objectDescriptor;

		return super.getObjectDescriptor(name); // Imports
	}

	@NotNull
	@Override
	public Set<ClassDescriptor> getObjectDescriptors()
	{
		checkMayRead();
		Set<ClassDescriptor> objectDescriptors = Sets.newHashSet();

		objectDescriptors.addAll(super.getObjectDescriptors());
		objectDescriptors.addAll(getWorkerScope().getObjectDescriptors());
		objectDescriptors.addAll(writableWorker.getObjectDescriptors());
		return objectDescriptors;
	}

	@Override
	public void addVariableDescriptor(@NotNull VariableDescriptor variableDescriptor)
	{
		checkMayWrite();

		writableWorker.addVariableDescriptor(variableDescriptor);
	}

	@Override
	public void addPropertyDescriptor(@NotNull VariableDescriptor propertyDescriptor)
	{
		checkMayWrite();

		writableWorker.addPropertyDescriptor(propertyDescriptor);
	}

	@Override
	public void addMethodDescriptor(@NotNull MethodDescriptor methodDescriptor)
	{
		checkMayWrite();

		writableWorker.addMethodDescriptor(methodDescriptor);
	}

	@Override
	public void addTypeParameterDescriptor(@NotNull TypeParameterDescriptor typeParameterDescriptor)
	{
		checkMayWrite();

		writableWorker.addTypeParameterDescriptor(typeParameterDescriptor);
	}

	@Override
	public void addClassifierDescriptor(@NotNull ClassifierDescriptor classDescriptor)
	{
		checkMayWrite();

		writableWorker.addClassifierDescriptor(classDescriptor);
	}

	@Override
	public void addObjectDescriptor(@NotNull ClassDescriptor objectDescriptor)
	{
		checkMayWrite();

		writableWorker.addObjectDescriptor(objectDescriptor);
	}

	@Override
	public void addClassifierAlias(@NotNull Name name, @NotNull ClassifierDescriptor classifierDescriptor)
	{
		checkMayWrite();

		writableWorker.addClassifierAlias(name, classifierDescriptor);
	}

	@Override
	public void addNamespaceAlias(@NotNull Name name, @NotNull PackageDescriptor packageDescriptor)
	{
		checkMayWrite();

		writableWorker.addNamespaceAlias(name, packageDescriptor);
	}

	@Override
	public void addVariableAlias(@NotNull Name name, @NotNull VariableDescriptor variableDescriptor)
	{
		checkMayWrite();

		writableWorker.addVariableAlias(name, variableDescriptor);
	}

	@Override
	public void addFunctionAlias(@NotNull Name name, @NotNull MethodDescriptor methodDescriptor)
	{
		checkMayWrite();

		writableWorker.addFunctionAlias(name, methodDescriptor);
	}

	@Override
	public void addNamespace(@NotNull PackageDescriptor packageDescriptor)
	{
		checkMayWrite();

		writableWorker.addNamespace(packageDescriptor);
	}

	@Override
	@Nullable
	public PackageDescriptor getDeclaredNamespace(@NotNull Name name)
	{
		checkMayRead();

		return writableWorker.getDeclaredNamespace(name);
	}

	@NotNull
	@Override
	public Multimap<Name, DeclarationDescriptor> getDeclaredDescriptorsAccessibleBySimpleName()
	{
		return writableWorker.getDeclaredDescriptorsAccessibleBySimpleName();
	}

	@Override
	public void importScope(@NotNull NapileScope imported)
	{
		checkMayWrite();

		super.importScope(imported); //
	}

	@Override
	public void setImplicitReceiver(@NotNull ReceiverDescriptor implicitReceiver)
	{
		checkMayWrite();

		writableWorker.setImplicitReceiver(implicitReceiver);
	}

	@NotNull
	@Override
	public Collection<DeclarationDescriptor> getAllDescriptors()
	{
		checkMayRead();

		if(allDescriptors == null)
		{
			allDescriptors = Lists.newArrayList();
			allDescriptors.addAll(writableWorker.getAllDescriptors());
			allDescriptors.addAll(getWorkerScope().getAllDescriptors());

			for(NapileScope imported : getImports())
			{
				allDescriptors.addAll(imported.getAllDescriptors());
			}
		}
		return allDescriptors;
	}

	@NotNull
	public NapileScope getOuterScope()
	{
		checkMayRead();

		return getWorkerScope();
	}
}
