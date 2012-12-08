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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.RedeclarationHandler;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.resolve.scopes.receivers.ClassReceiver;

/**
 * @author abreslav
 */
public class MutableClassDescriptor extends MutableClassDescriptorLite
{
	private final Set<ConstructorDescriptor> constructors = new HashSet<ConstructorDescriptor>();
	private final Set<ConstructorDescriptor> staticConstructors = new LinkedHashSet<ConstructorDescriptor>();
	private final Set<CallableMemberDescriptor> declaredCallableMembers = new HashSet<CallableMemberDescriptor>();
	private final Set<CallableMemberDescriptor> allCallableMembers = new HashSet<CallableMemberDescriptor>(); // includes fake overrides
	private final Set<VariableDescriptor> variables = new HashSet<VariableDescriptor>();
	private final Set<MethodDescriptor> methods = new HashSet<MethodDescriptor>();

	private final WritableScope scopeForMemberResolution;
	// This scope contains type parameters but does not contain inner classes
	private final WritableScope scopeForSupertypeResolution;

	private final WritableScope staticScope;

	public MutableClassDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope outerScope, ClassKind kind, Name name, boolean isStatic)
	{
		super(containingDeclaration, kind, isStatic);

		RedeclarationHandler redeclarationHandler = RedeclarationHandler.DO_NOTHING;

		setScopeForMemberLookup(new WritableScopeImpl(JetScope.EMPTY, this, redeclarationHandler, "MemberLookup").changeLockLevel(WritableScope.LockLevel.BOTH));
		this.scopeForSupertypeResolution = new WritableScopeImpl(outerScope, this, redeclarationHandler, "SupertypeResolution").changeLockLevel(WritableScope.LockLevel.BOTH);
		this.staticScope = new WritableScopeImpl(outerScope, this, redeclarationHandler, "StatisScope").changeLockLevel(WritableScope.LockLevel.BOTH);
		this.scopeForMemberResolution = new WritableScopeImpl(scopeForSupertypeResolution, this, redeclarationHandler, "MemberResolution").changeLockLevel(WritableScope.LockLevel.BOTH);

		setName(name);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	public void addConstructor(@NotNull ConstructorDescriptor constructorDescriptor)
	{
		if(constructorDescriptor.getContainingDeclaration() != this)
			throw new IllegalStateException("invalid containing declaration of constructor");

		constructors.add(constructorDescriptor);
		if(defaultType != null)
			constructorDescriptor.setReturnType(getDefaultType());
	}

	@NotNull
	@Override
	public JetScope getStaticOuterScope()
	{
		return staticScope;
	}

	@NotNull
	@Override
	public Set<ConstructorDescriptor> getConstructors()
	{
		return constructors;
	}

	@NotNull
	public Set<MethodDescriptor> getMethods()
	{
		return methods;
	}

	@NotNull
	public Set<VariableDescriptor> getVariables()
	{
		return variables;
	}

	@NotNull
	public Set<ConstructorDescriptor> getStaticConstructors()
	{
		return staticConstructors;
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

		scopeForSupertypeResolution.changeLockLevel(WritableScope.LockLevel.READING);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void createTypeConstructor()
	{
		super.createTypeConstructor();
		for(MethodDescriptor methodDescriptor : getConstructors())
			((ConstructorDescriptor) methodDescriptor).setReturnType(getDefaultType());
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

	@Override
	public void lockScopes()
	{
		super.lockScopes();
		scopeForSupertypeResolution.changeLockLevel(WritableScope.LockLevel.READING);
		scopeForMemberResolution.changeLockLevel(WritableScope.LockLevel.READING);
	}

	private DescriptorBuilder builder = null;

	@Override
	public DescriptorBuilder getBuilder()
	{
		if(builder == null)
		{
			final DescriptorBuilder superBuilder = super.getBuilder();
			builder = new DescriptorBuilderDummy()
			{
				@NotNull
				@Override
				public DeclarationDescriptor getOwnerForChildren()
				{
					return superBuilder.getOwnerForChildren();
				}

				@Override
				public void addAnonymClassDescriptor(@NotNull MutableClassDescriptorLite objectDescriptor)
				{
					superBuilder.addAnonymClassDescriptor(objectDescriptor);
				}

				@Override
				public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor)
				{
					superBuilder.addClassifierDescriptor(classDescriptor);
					scopeForMemberResolution.addClassifierDescriptor(classDescriptor);
				}

				@Override
				public void addMethodDescriptor(@NotNull MethodDescriptor functionDescriptor)
				{
					superBuilder.addMethodDescriptor(functionDescriptor);
					methods.add(functionDescriptor);
					if(functionDescriptor.getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
					{
						declaredCallableMembers.add(functionDescriptor);
					}
					allCallableMembers.add(functionDescriptor);
					scopeForMemberResolution.addMethodDescriptor(functionDescriptor);
					if(functionDescriptor.isStatic())
						staticScope.addMethodDescriptor(functionDescriptor);
				}

				@Override
				public void addVariableDescriptor(@NotNull VariableDescriptor propertyDescriptor)
				{
					superBuilder.addVariableDescriptor(propertyDescriptor);
					variables.add(propertyDescriptor);
					if(propertyDescriptor.getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
						declaredCallableMembers.add(propertyDescriptor);
					allCallableMembers.add(propertyDescriptor);
					scopeForMemberResolution.addPropertyDescriptor(propertyDescriptor);

					if(propertyDescriptor.isStatic())
						staticScope.addPropertyDescriptor(propertyDescriptor);
				}

				@Override
				public void addConstructorDescriptor(@NotNull ConstructorDescriptor constructorDescriptor)
				{
					addConstructor(constructorDescriptor);

					allCallableMembers.add(constructorDescriptor);
				}

				@Override
				public void addStaticConstructorDescriptor(@NotNull ConstructorDescriptor constructorDescriptor)
				{
					getStaticConstructors().add(constructorDescriptor);
				}
			};
		}

		return builder;
	}

}
