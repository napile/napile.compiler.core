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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.NamespaceDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.util.CommonSuppliers;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

/**
 * @author abreslav
 */
public class WritableScopeImpl extends WritableScopeWithImports
{

	private final Collection<DeclarationDescriptor> allDescriptors = Lists.newArrayList();
	private final Multimap<Name, DeclarationDescriptor> declaredDescriptorsAccessibleBySimpleName = HashMultimap.create();
	private boolean allDescriptorsDone = false;

	@NotNull
	private final DeclarationDescriptor ownerDeclarationDescriptor;

	// FieldNames include "$"
	@Nullable
	private Map<Name, PropertyDescriptor> propertyDescriptorsByFieldNames;

	@Nullable
	private SetMultimap<Name, MethodDescriptor> functionGroups;

	@Nullable
	private Map<Name, DeclarationDescriptor> variableClassOrNamespaceDescriptors;

	@NotNull
	private final Map<FqName, ClassDescriptor> classDescriptors = new HashMap<FqName, ClassDescriptor>();

	@Nullable
	private SetMultimap<Name, VariableDescriptor> propertyGroups;

	@Nullable
	private Map<Name, NamespaceDescriptor> namespaceAliases;

	@Nullable
	private Map<Name, ClassDescriptor> objectDescriptors;

	@Nullable
	private ReceiverDescriptor implicitReceiver;

	public WritableScopeImpl(@NotNull JetScope scope, @NotNull DeclarationDescriptor owner, @NotNull RedeclarationHandler redeclarationHandler, @NotNull String debugName)
	{
		super(scope, redeclarationHandler, debugName);
		this.ownerDeclarationDescriptor = owner;
	}

	@NotNull
	@Override
	public DeclarationDescriptor getContainingDeclaration()
	{
		return ownerDeclarationDescriptor;
	}

	@Override
	public void importScope(@NotNull JetScope imported)
	{
		checkMayWrite();
		super.importScope(imported);
	}

	@Override
	public void importClassifierAlias(@NotNull Name importedClassifierName, @NotNull ClassifierDescriptor classifierDescriptor)
	{
		checkMayWrite();

		allDescriptors.add(classifierDescriptor);
		super.importClassifierAlias(importedClassifierName, classifierDescriptor);
	}

	@Override
	public void importNamespaceAlias(@NotNull Name aliasName, @NotNull NamespaceDescriptor namespaceDescriptor)
	{
		checkMayWrite();

		allDescriptors.add(namespaceDescriptor);
		super.importNamespaceAlias(aliasName, namespaceDescriptor);
	}

	@Override
	public void importFunctionAlias(@NotNull Name aliasName, @NotNull MethodDescriptor methodDescriptor)
	{
		checkMayWrite();

		addFunctionDescriptor(methodDescriptor);
		super.importFunctionAlias(aliasName, methodDescriptor);
	}

	@Override
	public void importVariableAlias(@NotNull Name aliasName, @NotNull VariableDescriptor variableDescriptor)
	{
		checkMayWrite();

		addPropertyDescriptor(variableDescriptor);
		super.importVariableAlias(aliasName, variableDescriptor);
	}

	@Override
	public void clearImports()
	{
		checkMayWrite();

		super.clearImports();
	}

	@NotNull
	@Override
	public Collection<DeclarationDescriptor> getAllDescriptors()
	{
		checkMayRead();

		if(!allDescriptorsDone)
		{
			allDescriptorsDone = true;

			// make sure no descriptors added to allDescriptors collection
			changeLockLevel(LockLevel.READING);

			allDescriptors.addAll(getWorkerScope().getAllDescriptors());
			for(JetScope imported : getImports())
			{
				allDescriptors.addAll(imported.getAllDescriptors());
			}
		}
		return allDescriptors;
	}

	@NotNull
	private Map<Name, ClassDescriptor> getObjectDescriptorsMap()
	{
		if(objectDescriptors == null)
		{
			objectDescriptors = Maps.newHashMap();
		}
		return objectDescriptors;
	}

	@NotNull
	private Map<Name, DeclarationDescriptor> getVariableClassOrNamespaceDescriptors()
	{
		if(variableClassOrNamespaceDescriptors == null)
		{
			variableClassOrNamespaceDescriptors = Maps.newHashMap();
		}
		return variableClassOrNamespaceDescriptors;
	}

	@NotNull
	private Map<Name, NamespaceDescriptor> getNamespaceAliases()
	{
		if(namespaceAliases == null)
		{
			namespaceAliases = Maps.newHashMap();
		}
		return namespaceAliases;
	}

	@Override
	public void addVariableDescriptor(@NotNull VariableDescriptor variableDescriptor)
	{
		addVariableDescriptor(variableDescriptor, false);
	}

	@Override
	public void addPropertyDescriptor(@NotNull VariableDescriptor propertyDescriptor)
	{
		addVariableDescriptor(propertyDescriptor, true);
	}

	@Override
	public void addConstructorDescriptor(@NotNull ConstructorDescriptor constructorDescriptor)
	{
		//allDescriptors.add(constructorDescriptor);
		//addToDeclared(constructorDescriptor);
	}

	private void addVariableDescriptor(@NotNull VariableDescriptor variableDescriptor, boolean isProperty)
	{
		checkMayWrite();

		Name name = variableDescriptor.getName();
		if(isProperty)
			getPropertyGroups().put(name, variableDescriptor);

		checkForRedeclaration(name, variableDescriptor);

		getVariableClassOrNamespaceDescriptors().put(name, variableDescriptor);

		allDescriptors.add(variableDescriptor);
		addToDeclared(variableDescriptor);
	}

	@NotNull
	@Override
	public Set<VariableDescriptor> getProperties(@NotNull Name name)
	{
		checkMayRead();

		Set<VariableDescriptor> result = Sets.newLinkedHashSet(getPropertyGroups().get(name));

		result.addAll(getWorkerScope().getProperties(name));

		result.addAll(super.getProperties(name));

		return result;
	}

	@Override
	public VariableDescriptor getLocalVariable(@NotNull Name name)
	{
		checkMayRead();

		Map<Name, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableClassOrNamespaceDescriptors();
		DeclarationDescriptor descriptor = variableClassOrNamespaceDescriptors.get(name);
		if(descriptor instanceof VariableDescriptor && !getPropertyGroups().get(name).contains(descriptor))
		{
			return (VariableDescriptor) descriptor;
		}

		VariableDescriptor variableDescriptor = getWorkerScope().getLocalVariable(name);
		if(variableDescriptor != null)
		{
			return variableDescriptor;
		}
		return super.getLocalVariable(name);
	}

	@NotNull
	private SetMultimap<Name, VariableDescriptor> getPropertyGroups()
	{
		if(propertyGroups == null)
		{
			propertyGroups = CommonSuppliers.newLinkedHashSetHashSetMultimap();
		}
		return propertyGroups;
	}

	@NotNull
	private SetMultimap<Name, MethodDescriptor> getFunctionGroups()
	{
		if(functionGroups == null)
		{
			functionGroups = CommonSuppliers.newLinkedHashSetHashSetMultimap();
		}
		return functionGroups;
	}

	@Override
	public void addFunctionDescriptor(@NotNull MethodDescriptor methodDescriptor)
	{
		checkMayWrite();

		getFunctionGroups().put(methodDescriptor.getName(), methodDescriptor);
		allDescriptors.add(methodDescriptor);
	}

	@Override
	@NotNull
	public Collection<MethodDescriptor> getMethods(@NotNull Name name)
	{
		checkMayRead();

		Set<MethodDescriptor> result = Sets.newLinkedHashSet(getFunctionGroups().get(name));

		result.addAll(getWorkerScope().getMethods(name));

		result.addAll(super.getMethods(name));

		return result;
	}

	@Override
	public void addTypeParameterDescriptor(@NotNull TypeParameterDescriptor typeParameterDescriptor)
	{
		checkMayWrite();

		Name name = typeParameterDescriptor.getName();
		addClassifierAlias(name, typeParameterDescriptor);
	}

	@Override
	public void addClassifierDescriptor(@NotNull ClassifierDescriptor classDescriptor)
	{
		checkMayWrite();

		if(DescriptorUtils.isObject(classDescriptor))
		{
			throw new IllegalStateException("must not be object: " + classDescriptor);
		}

		addClassifierAlias(classDescriptor.getName(), classDescriptor);
	}

	@Override
	public void addObjectDescriptor(@NotNull ClassDescriptor objectDescriptor)
	{
		checkMayWrite();

		if(!DescriptorUtils.isObject(objectDescriptor))
		{
			throw new IllegalStateException("must be object: " + objectDescriptor);
		}

		getObjectDescriptorsMap().put(objectDescriptor.getName(), objectDescriptor);
	}

	@Override
	public void addClassifierAlias(@NotNull Name name, @NotNull ClassifierDescriptor classifierDescriptor)
	{
		checkMayWrite();

		checkForRedeclaration(name, classifierDescriptor);
		getVariableClassOrNamespaceDescriptors().put(name, classifierDescriptor);
		allDescriptors.add(classifierDescriptor);
		addToDeclared(classifierDescriptor);

		if(classifierDescriptor instanceof ClassDescriptor)
			classDescriptors.put(DescriptorUtils.getFQName(classifierDescriptor).toSafe(), (ClassDescriptor) classifierDescriptor);
	}

	@Override
	public void addNamespaceAlias(@NotNull Name name, @NotNull NamespaceDescriptor namespaceDescriptor)
	{
		checkMayWrite();

		checkForRedeclaration(name, namespaceDescriptor);
		getNamespaceAliases().put(name, namespaceDescriptor);
		allDescriptors.add(namespaceDescriptor);
		addToDeclared(namespaceDescriptor);
	}

	@Override
	public void addFunctionAlias(@NotNull Name name, @NotNull MethodDescriptor methodDescriptor)
	{
		checkMayWrite();

		checkForRedeclaration(name, methodDescriptor);
		getFunctionGroups().put(name, methodDescriptor);
		allDescriptors.add(methodDescriptor);
	}

	@Override
	public void addVariableAlias(@NotNull Name name, @NotNull VariableDescriptor variableDescriptor)
	{
		checkMayWrite();

		checkForRedeclaration(name, variableDescriptor);
		getVariableClassOrNamespaceDescriptors().put(name, variableDescriptor);
		allDescriptors.add(variableDescriptor);
		addToDeclared(variableDescriptor);
	}


	private void checkForRedeclaration(@NotNull Name name, DeclarationDescriptor classifierDescriptor)
	{
		DeclarationDescriptor originalDescriptor = getVariableClassOrNamespaceDescriptors().get(name);
		if(originalDescriptor != null)
		{
			redeclarationHandler.handleRedeclaration(originalDescriptor, classifierDescriptor);
		}
	}

	@Override
	public ClassifierDescriptor getClassifier(@NotNull Name name)
	{
		checkMayRead();

		Map<Name, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableClassOrNamespaceDescriptors();
		DeclarationDescriptor descriptor = variableClassOrNamespaceDescriptors.get(name);
		if(descriptor instanceof ClassifierDescriptor)
			return (ClassifierDescriptor) descriptor;

		ClassifierDescriptor classifierDescriptor = getWorkerScope().getClassifier(name);
		if(classifierDescriptor != null)
			return classifierDescriptor;

		return super.getClassifier(name);
	}

	@Override
	public ClassDescriptor getClass(@NotNull FqName name)
	{
		checkMayRead();

		ClassDescriptor classDescriptor = classDescriptors.get(name);
		if(classDescriptor != null)
			return classDescriptor;

		classDescriptor = getWorkerScope().getClass(name);
		if(classDescriptor != null)
			return classDescriptor;

		return super.getClass(name);
	}

	@Override
	public ClassDescriptor getObjectDescriptor(@NotNull Name name)
	{
		return getObjectDescriptorsMap().get(name);
	}

	@NotNull
	@Override
	public Set<ClassDescriptor> getObjectDescriptors()
	{
		return Sets.newHashSet(getObjectDescriptorsMap().values());
	}

	@Override
	public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor)
	{
		checkMayWrite();

		Map<Name, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableClassOrNamespaceDescriptors();
		DeclarationDescriptor oldValue = variableClassOrNamespaceDescriptors.put(namespaceDescriptor.getName(), namespaceDescriptor);
		if(oldValue != null)
		{
			redeclarationHandler.handleRedeclaration(oldValue, namespaceDescriptor);
		}
		allDescriptors.add(namespaceDescriptor);
		addToDeclared(namespaceDescriptor);
	}

	@Override
	public NamespaceDescriptor getDeclaredNamespace(@NotNull Name name)
	{
		checkMayRead();

		Map<Name, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableClassOrNamespaceDescriptors();
		DeclarationDescriptor namespaceDescriptor = variableClassOrNamespaceDescriptors.get(name);
		if(namespaceDescriptor instanceof NamespaceDescriptor)
			return (NamespaceDescriptor) namespaceDescriptor;
		return null;
	}

	@Override
	public NamespaceDescriptor getNamespace(@NotNull Name name)
	{
		checkMayRead();

		NamespaceDescriptor declaredNamespace = getDeclaredNamespace(name);
		if(declaredNamespace != null)
			return declaredNamespace;

		NamespaceDescriptor aliased = getNamespaceAliases().get(name);
		if(aliased != null)
			return aliased;

		NamespaceDescriptor namespace = getWorkerScope().getNamespace(name);
		if(namespace != null)
			return namespace;
		return super.getNamespace(name);
	}

	@NotNull
	@Override
	public ReceiverDescriptor getImplicitReceiver()
	{
		checkMayRead();

		if(implicitReceiver == null)
		{
			return super.getImplicitReceiver();
		}
		return implicitReceiver;
	}

	@Override
	@Deprecated
	public void setImplicitReceiver(@NotNull ReceiverDescriptor implicitReceiver)
	{
		checkMayWrite();

		if(this.implicitReceiver != null)
		{
			throw new UnsupportedOperationException("Receiver redeclared");
		}
		this.implicitReceiver = implicitReceiver;
	}

	@Override
	public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result)
	{
		checkMayRead();

		super.getImplicitReceiversHierarchy(result);

		if(implicitReceiver != null && implicitReceiver.exists())
		{
			result.add(0, implicitReceiver);
		}
	}

	//    @SuppressWarnings({"NullableProblems"})
	@NotNull
	private Map<Name, PropertyDescriptor> getPropertyDescriptorsByFieldNames()
	{
		if(propertyDescriptorsByFieldNames == null)
		{
			propertyDescriptorsByFieldNames = new HashMap<Name, PropertyDescriptor>();
		}
		return propertyDescriptorsByFieldNames;
	}

	@Override
	public PropertyDescriptor getPropertyByFieldReference(@NotNull Name fieldName)
	{
		checkMayRead();

		if(!fieldName.getName().startsWith("$"))
		{
			throw new IllegalStateException();
		}

		PropertyDescriptor descriptor = getPropertyDescriptorsByFieldNames().get(fieldName);
		if(descriptor != null)
			return descriptor;
		return super.getPropertyByFieldReference(fieldName);
	}

	public List<VariableDescriptor> getDeclaredVariables()
	{
		checkMayRead();

		List<VariableDescriptor> result = Lists.newArrayList();
		for(DeclarationDescriptor descriptor : getVariableClassOrNamespaceDescriptors().values())
		{
			if(descriptor instanceof VariableDescriptor)
			{
				VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor;
				result.add(variableDescriptor);
			}
		}
		return result;
	}

	public boolean hasDeclaredItems()
	{
		return variableClassOrNamespaceDescriptors != null && !variableClassOrNamespaceDescriptors.isEmpty();
	}

	private void addToDeclared(DeclarationDescriptor descriptor)
	{
		declaredDescriptorsAccessibleBySimpleName.put(descriptor.getName(), descriptor);
	}

	@NotNull
	@Override
	public Multimap<Name, DeclarationDescriptor> getDeclaredDescriptorsAccessibleBySimpleName()
	{
		return declaredDescriptorsAccessibleBySimpleName;
	}

	@NotNull
	@Override
	public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors()
	{
		return declaredDescriptorsAccessibleBySimpleName.values();
	}
}
