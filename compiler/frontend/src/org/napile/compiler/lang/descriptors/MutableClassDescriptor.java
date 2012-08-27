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

package org.napile.compiler.lang.descriptors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.psi.NapileDelegationSpecifierListOwner;
import org.napile.compiler.lang.resolve.AbstractScopeAdapter;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.RedeclarationHandler;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.resolve.scopes.receivers.ClassReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import com.google.common.collect.Sets;

/**
 * @author abreslav
 */
public class MutableClassDescriptor extends MutableClassDescriptorLite
{
	private final Map<NapileDelegationSpecifierListOwner, ConstructorDescriptor> constructors = new LinkedHashMap<NapileDelegationSpecifierListOwner, ConstructorDescriptor>();

	private final Set<CallableMemberDescriptor> declaredCallableMembers = Sets.newHashSet();
	private final Set<CallableMemberDescriptor> allCallableMembers = Sets.newHashSet(); // includes fake overrides
	private final Set<PropertyDescriptor> properties = Sets.newHashSet();
	private final Set<SimpleFunctionDescriptor> functions = Sets.newHashSet();

	private final WritableScope scopeForMemberResolution;
	// This scope contains type parameters but does not contain inner classes
	private final WritableScope scopeForSupertypeResolution;
	private final WritableScope scopeForInitializers; //contains members + primary constructor value parameters + map for backing fields

	public MutableClassDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope outerScope, ClassKind kind, Name name, boolean isStatic)
	{
		super(containingDeclaration, kind, false);

		RedeclarationHandler redeclarationHandler = RedeclarationHandler.DO_NOTHING;

		setScopeForMemberLookup(new WritableScopeImpl(JetScope.EMPTY, this, redeclarationHandler, "MemberLookup").changeLockLevel(WritableScope.LockLevel.BOTH));
		this.scopeForSupertypeResolution = new WritableScopeImpl(outerScope, this, redeclarationHandler, "SupertypeResolution").changeLockLevel(WritableScope.LockLevel.BOTH);
		this.scopeForMemberResolution = new WritableScopeImpl(scopeForSupertypeResolution, this, redeclarationHandler, "MemberResolution").changeLockLevel(WritableScope.LockLevel.BOTH);
		this.scopeForInitializers = new WritableScopeImpl(scopeForMemberResolution, containingDeclaration, RedeclarationHandler.DO_NOTHING, "Initializers").changeLockLevel(WritableScope.LockLevel.BOTH);

		setName(name);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	public void addConstructor(@NotNull NapileDelegationSpecifierListOwner constructor, @NotNull ConstructorDescriptor constructorDescriptor, @NotNull BindingTrace trace)
	{
		if(constructorDescriptor.getContainingDeclaration() != this)
		{
			throw new IllegalStateException("invalid containing declaration of constructor");
		}
		constructors.put(constructor, constructorDescriptor);
		if(defaultType != null)
		{
			constructorDescriptor.setReturnType(getDefaultType());
		}
	}

	@NotNull
	@Override
	public Map<NapileDelegationSpecifierListOwner, ConstructorDescriptor> getConstructors()
	{
		return constructors;
	}

	@NotNull
	public Set<SimpleFunctionDescriptor> getFunctions()
	{
		return functions;
	}

	@NotNull
	public Set<PropertyDescriptor> getProperties()
	{
		return properties;
	}

	@NotNull
	public Set<CallableMemberDescriptor> getDeclaredCallableMembers()
	{
		return declaredCallableMembers;
	}

	@NotNull
	public Set<CallableMemberDescriptor> getAllCallableMembers()
	{
		return allCallableMembers;
	}

	@Override
	public void setTypeParameterDescriptors(List<TypeParameterDescriptor> typeParameters)
	{
		super.setTypeParameterDescriptors(typeParameters);
		for(TypeParameterDescriptor typeParameterDescriptor : typeParameters)
		{
			scopeForSupertypeResolution.addTypeParameterDescriptor(typeParameterDescriptor);
		}
		scopeForSupertypeResolution.changeLockLevel(WritableScope.LockLevel.READING);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void setName(@NotNull Name name)
	{
		super.setName(name);
		scopeForMemberResolution.addLabeledDeclaration(this);
	}

	@Override
	public void createTypeConstructor()
	{
		super.createTypeConstructor();
		for(FunctionDescriptor functionDescriptor : getConstructors().values())
			((ConstructorDescriptor) functionDescriptor).setReturnType(getDefaultType());
		scopeForMemberResolution.setImplicitReceiver(new ClassReceiver(this));
	}

	@NotNull
	public JetScope getScopeForSupertypeResolution()
	{
		return scopeForSupertypeResolution;
	}

	@NotNull
	public JetScope getScopeForMemberResolution()
	{
		return scopeForMemberResolution;
	}

	private WritableScope getWritableScopeForInitializers()
	{
		if(scopeForInitializers == null)
		{
			throw new IllegalStateException("Scope for initializers queried before the primary constructor is set");
		}
		return scopeForInitializers;
	}

	@NotNull
	public JetScope getScopeForInitializers()
	{
		return getWritableScopeForInitializers();
	}

	@Override
	public void lockScopes()
	{
		super.lockScopes();
		scopeForSupertypeResolution.changeLockLevel(WritableScope.LockLevel.READING);
		scopeForMemberResolution.changeLockLevel(WritableScope.LockLevel.READING);
		getWritableScopeForInitializers().changeLockLevel(WritableScope.LockLevel.READING);
	}

	private NamespaceLikeBuilder builder = null;

	@Override
	public NamespaceLikeBuilder getBuilder()
	{
		if(builder == null)
		{
			final NamespaceLikeBuilder superBuilder = super.getBuilder();
			builder = new NamespaceLikeBuilderDummy()
			{
				@NotNull
				@Override
				public DeclarationDescriptor getOwnerForChildren()
				{
					return superBuilder.getOwnerForChildren();
				}

				@Override
				public void addObjectDescriptor(@NotNull MutableClassDescriptorLite objectDescriptor)
				{
					superBuilder.addObjectDescriptor(objectDescriptor);
				}

				@Override
				public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor)
				{
					superBuilder.addClassifierDescriptor(classDescriptor);
					scopeForMemberResolution.addClassifierDescriptor(classDescriptor);
				}

				@Override
				public void addFunctionDescriptor(@NotNull SimpleFunctionDescriptor functionDescriptor)
				{
					superBuilder.addFunctionDescriptor(functionDescriptor);
					functions.add(functionDescriptor);
					if(functionDescriptor.getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
					{
						declaredCallableMembers.add(functionDescriptor);
					}
					allCallableMembers.add(functionDescriptor);
					scopeForMemberResolution.addFunctionDescriptor(functionDescriptor);
				}

				@Override
				public ClassObjectStatus setClassObjectDescriptor(@NotNull final MutableClassDescriptorLite classObjectDescriptor)
				{
					ClassObjectStatus r = superBuilder.setClassObjectDescriptor(classObjectDescriptor);
					if(r != ClassObjectStatus.OK)
					{
						return r;
					}

					// Members of the class object are accessible from the class
					// The scope must be lazy, because classObjectDescriptor may not by fully built yet
					scopeForMemberResolution.importScope(new AbstractScopeAdapter()
					{
						@NotNull
						@Override
						protected JetScope getWorkerScope()
						{
							return classObjectDescriptor.getDefaultType().getMemberScope();
						}

						@NotNull
						@Override
						public ReceiverDescriptor getImplicitReceiver()
						{
							return classObjectDescriptor.getImplicitReceiver();
						}
					});

					return ClassObjectStatus.OK;
				}

				@Override
				public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor)
				{
					superBuilder.addPropertyDescriptor(propertyDescriptor);
					properties.add(propertyDescriptor);
					if(propertyDescriptor.getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
					{
						declaredCallableMembers.add(propertyDescriptor);
					}
					allCallableMembers.add(propertyDescriptor);
					scopeForMemberResolution.addPropertyDescriptor(propertyDescriptor);
				}
			};
		}

		return builder;
	}
}