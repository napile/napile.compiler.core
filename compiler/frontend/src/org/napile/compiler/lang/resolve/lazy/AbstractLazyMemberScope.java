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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileAnonymClass;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileEnumEntry;
import org.napile.compiler.lang.psi.NapileLikeClass;
import org.napile.compiler.lang.psi.NapileMethod;
import org.napile.compiler.lang.psi.NapileNamedFunction;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.psi.NapilePropertyParameter;
import org.napile.compiler.lang.resolve.lazy.data.JetClassInfoUtil;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author abreslav
 */
public abstract class AbstractLazyMemberScope<D extends DeclarationDescriptor, DP extends DeclarationProvider> implements JetScope
{
	protected final ResolveSession resolveSession;
	protected final DP declarationProvider;
	protected final D thisDescriptor;

	protected boolean allDescriptorsComputed = false;

	private final Map<Name, ClassDescriptor> classDescriptors = Maps.newHashMap();
	private final Map<Name, Set<MethodDescriptor>> functionDescriptors = Maps.newHashMap();
	private final Map<Name, Set<VariableDescriptor>> propertyDescriptors = Maps.newHashMap();

	protected final List<DeclarationDescriptor> allDescriptors = Lists.newArrayList();

	protected AbstractLazyMemberScope(@NotNull ResolveSession resolveSession, @NotNull DP declarationProvider, @NotNull D thisDescriptor)
	{
		this.resolveSession = resolveSession;
		this.declarationProvider = declarationProvider;
		this.thisDescriptor = thisDescriptor;
	}

	@Nullable
	private ClassDescriptor getClassOrObjectDescriptor(@NotNull Name name, boolean object)
	{
		ClassDescriptor known = classDescriptors.get(name);
		if(known != null)
			return known;

		if(allDescriptorsComputed)
			return null;

		NapileLikeClass classOrObjectDeclaration = declarationProvider.getClassOrObjectDeclaration(name);
		if(classOrObjectDeclaration == null)
			return null;

		if(object != classOrObjectDeclaration instanceof NapileAnonymClass)
			return null;

		ClassDescriptor classDescriptor = new LazyClassDescriptor(resolveSession, thisDescriptor, name, JetClassInfoUtil.createClassLikeInfo(classOrObjectDeclaration), false);

		classDescriptors.put(name, classDescriptor);
		if(!object)
		{
			allDescriptors.add(classDescriptor);
		}

		return classDescriptor;
	}

	@Override
	public ClassifierDescriptor getClassifier(@NotNull Name name)
	{
		return getClassOrObjectDescriptor(name, false);
	}

	@Override
	public ClassDescriptor getObjectDescriptor(@NotNull Name name)
	{
		// TODO: We shouldn't really allow objects in classes...
		return getClassOrObjectDescriptor(name, true);
	}

	@NotNull
	@Override
	public Set<MethodDescriptor> getFunctions(@NotNull Name name)
	{
		Set<MethodDescriptor> known = functionDescriptors.get(name);
		if(known != null)
			return known;

		// If all descriptors are already computed, we are
		if(allDescriptorsComputed)
			return Collections.emptySet();

		Set<MethodDescriptor> result = Sets.newLinkedHashSet();

		Collection<NapileNamedFunction> declarations = declarationProvider.getFunctionDeclarations(name);
		for(NapileNamedFunction functionDeclaration : declarations)
		{
			JetScope resolutionScope = getScopeForMemberDeclarationResolution(functionDeclaration);
			result.add(resolveSession.getInjector().getDescriptorResolver().resolveFunctionDescriptor(thisDescriptor, resolutionScope, functionDeclaration, resolveSession.getTrace()));
		}

		getNonDeclaredFunctions(name, result);

		if(!result.isEmpty())
		{
			functionDescriptors.put(name, result);
			allDescriptors.addAll(result);
		}
		return result;
	}

	@NotNull
	protected abstract JetScope getScopeForMemberDeclarationResolution(NapileDeclaration declaration);

	protected abstract void getNonDeclaredFunctions(@NotNull Name name, @NotNull Set<MethodDescriptor> result);

	@NotNull
	@Override
	public Set<VariableDescriptor> getProperties(@NotNull Name name)
	{
		Set<VariableDescriptor> known = propertyDescriptors.get(name);
		if(known != null)
			return known;

		// If all descriptors are already computed, we are
		if(allDescriptorsComputed)
			return Collections.emptySet();

		Set<VariableDescriptor> result = Sets.newLinkedHashSet();

		Collection<NapileProperty> declarations = declarationProvider.getPropertyDeclarations(name);
		for(NapileProperty propertyDeclaration : declarations)
		{
			JetScope resolutionScope = getScopeForMemberDeclarationResolution(propertyDeclaration);
			result.add(resolveSession.getInjector().getDescriptorResolver().resolvePropertyDescriptor(thisDescriptor, resolutionScope, propertyDeclaration, resolveSession.getTrace()));
		}

		// Objects are also properties
		NapileLikeClass classOrObjectDeclaration = declarationProvider.getClassOrObjectDeclaration(name);
		if(classOrObjectDeclaration instanceof NapileAnonymClass)
		{
			NapileAnonymClass objectDeclaration = (NapileAnonymClass) classOrObjectDeclaration;
			ClassDescriptor classifier = getObjectDescriptor(name);
			if(classifier == null)
			{
				throw new IllegalStateException("Object declaration " +
						name +
						" found in the DeclarationProvider " +
						declarationProvider +
						" but not in the scope " +
						this);
			}
			VariableDescriptor propertyDescriptor = resolveSession.getInjector().getDescriptorResolver().resolveObjectDeclaration(thisDescriptor, objectDeclaration, classifier, resolveSession.getTrace());
			result.add(propertyDescriptor);
		}

		getNonDeclaredProperties(name, result);

		if(!result.isEmpty())
		{
			propertyDescriptors.put(name, result);
			allDescriptors.addAll(result);
		}
		return result;
	}

	protected abstract void getNonDeclaredProperties(@NotNull Name name, @NotNull Set<VariableDescriptor> result);

	@NotNull
	@Override
	public Collection<ClassDescriptor> getObjectDescriptors()
	{
		throw new UnsupportedOperationException(); // TODO
	}

	@Override
	public VariableDescriptor getLocalVariable(@NotNull Name name)
	{
		return null;
	}

	@NotNull
	@Override
	public DeclarationDescriptor getContainingDeclaration()
	{
		return thisDescriptor;
	}

	@NotNull
	@Override
	public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull Name labelName)
	{
		// A member scope has no labels
		return Collections.emptySet();
	}

	@Override
	public PropertyDescriptor getPropertyByFieldReference(@NotNull Name fieldName)
	{
		throw new UnsupportedOperationException(); // TODO
	}

	@NotNull
	@Override
	public Collection<DeclarationDescriptor> getAllDescriptors()
	{
		for(NapileDeclaration declaration : declarationProvider.getAllDeclarations())
		{
			if(declaration instanceof NapileEnumEntry)
			{
				NapileEnumEntry jetEnumEntry = (NapileEnumEntry) declaration;
				Name name = jetEnumEntry.getNameAsName();
				if(name != null)
				{
					getProperties(name);
				}
			}
			else if(declaration instanceof NapileAnonymClass)
			{
				NapileLikeClass classOrObject = (NapileLikeClass) declaration;
				Name name = classOrObject.getNameAsName();
				if(name != null)
				{
					getProperties(name);
				}
			}
			else if(declaration instanceof NapileLikeClass)
			{
				NapileLikeClass classOrObject = (NapileLikeClass) declaration;
				Name name = classOrObject.getNameAsName();
				if(name != null)
				{
					getClassifier(name);
				}
			}
			else if(declaration instanceof NapileMethod)
			{
				NapileMethod function = (NapileMethod) declaration;
				getFunctions(function.getNameAsSafeName());
			}
			else if(declaration instanceof NapileProperty)
			{
				NapileProperty property = (NapileProperty) declaration;
				getProperties(property.getNameAsSafeName());
			}
			else if(declaration instanceof NapilePropertyParameter)
			{
				NapilePropertyParameter parameter = (NapilePropertyParameter) declaration;
				Name name = parameter.getNameAsName();
				if(name != null)
				{
					getProperties(name);
				}
			}
			else
			{
				throw new IllegalArgumentException("Unsupported declaration kind: " + declaration);
			}
		}
		addExtraDescriptors();
		allDescriptorsComputed = true;
		return allDescriptors;
	}

	protected abstract void addExtraDescriptors();

	@Override
	public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result)
	{
		ReceiverDescriptor receiver = getImplicitReceiver();
		if(receiver.exists())
		{
			result.add(receiver);
		}
	}

	// Do not change this, override in concrete subclasses:
	// it is very easy to compromise laziness of this class, and fail all the debugging
	// a generic implementation can't do this properly
	@Override
	public abstract String toString();

	@NotNull
	@Override
	public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors()
	{
		return getAllDescriptors();
	}
}
