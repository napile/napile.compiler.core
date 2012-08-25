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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.ClassKind;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.FunctionDescriptor;
import org.napile.compiler.lang.descriptors.NamespaceDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileClassOrObject;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileDelegationSpecifierListOwner;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.DescriptorResolver;
import org.napile.compiler.lang.resolve.OverrideResolver;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.DeferredType;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.util.lazy.LazyValue;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;

/**
 * @author abreslav
 */
public class LazyClassMemberScope extends AbstractLazyMemberScope<LazyClassDescriptor, ClassMemberDeclarationProvider>
{

	private interface MemberExtractor<T extends CallableMemberDescriptor>
	{
		MemberExtractor<FunctionDescriptor> EXTRACT_FUNCTIONS = new MemberExtractor<FunctionDescriptor>()
		{
			@NotNull
			@Override
			public Collection<FunctionDescriptor> extract(@NotNull JetType extractFrom, @NotNull Name name)
			{
				return extractFrom.getMemberScope().getFunctions(name);
			}
		};

		MemberExtractor<PropertyDescriptor> EXTRACT_PROPERTIES = new MemberExtractor<PropertyDescriptor>()
		{
			@NotNull
			@Override
			public Collection<PropertyDescriptor> extract(@NotNull JetType extractFrom, @NotNull Name name)
			{
				//noinspection unchecked
				return (Collection) extractFrom.getMemberScope().getProperties(name);
			}
		};

		@NotNull
		Collection<T> extract(@NotNull JetType extractFrom, @NotNull Name name);
	}

	private Map<NapileDelegationSpecifierListOwner, ConstructorDescriptor> constructorDescriptors = null;

	public LazyClassMemberScope(@NotNull ResolveSession resolveSession, @NotNull ClassMemberDeclarationProvider declarationProvider, @NotNull LazyClassDescriptor thisClass)
	{
		super(resolveSession, declarationProvider, thisClass);
	}

	@NotNull
	@Override
	protected JetScope getScopeForMemberDeclarationResolution(NapileDeclaration declaration)
	{
		if(declaration instanceof NapileProperty)
		{
			return thisDescriptor.getScopeForPropertyInitializerResolution();
		}
		return thisDescriptor.getScopeForMemberDeclarationResolution();
	}

	private <D extends CallableMemberDescriptor> void generateFakeOverrides(@NotNull Name name, @NotNull Collection<D> fromSupertypes, @NotNull final Collection<D> result, @NotNull final Class<? extends D> exactDescriptorClass)
	{
		OverrideResolver.generateOverridesInFunctionGroup(name, fromSupertypes, Lists.newArrayList(result), thisDescriptor, new OverrideResolver.DescriptorSink()
		{
			@Override
			public void addToScope(@NotNull CallableMemberDescriptor fakeOverride)
			{
				assert exactDescriptorClass.isInstance(fakeOverride) : "Wrong descriptor type in an override: " +
						fakeOverride +
						" while expecting " +
						exactDescriptorClass.getSimpleName();
				//noinspection unchecked
				result.add((D) fakeOverride);
			}

			@Override
			public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent)
			{
				BindingTrace trace = resolveSession.getTrace();
				NapileDeclaration declaration = (NapileDeclaration) BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), fromCurrent);
				assert declaration != null : "fromCurrent can not be a fake override";
				trace.report(Errors.CONFLICTING_OVERLOADS.on(declaration, fromCurrent, fromCurrent.getContainingDeclaration().getName().getName()));
			}
		});
	}

	@NotNull
	@Override
	public Set<FunctionDescriptor> getFunctions(@NotNull Name name)
	{
		// TODO: this should be handled by lazy function descriptors
		Set<FunctionDescriptor> functions = super.getFunctions(name);
		for(FunctionDescriptor functionDescriptor : functions)
		{
			if(functionDescriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
				continue;
			PsiElement element = BindingContextUtils.callableDescriptorToDeclaration(resolveSession.getTrace().getBindingContext(), functionDescriptor);
			OverrideResolver.resolveUnknownVisibilityForMember((NapileDeclaration) element, functionDescriptor, resolveSession.getTrace());
		}
		return functions;
	}

	@Override
	protected void getNonDeclaredFunctions(@NotNull Name name, @NotNull final Set<FunctionDescriptor> result)
	{
		Collection<FunctionDescriptor> fromSupertypes = Lists.newArrayList();
		for(JetType supertype : thisDescriptor.getTypeConstructor().getSupertypes())
		{
			fromSupertypes.addAll(supertype.getMemberScope().getFunctions(name));
		}

		generateFakeOverrides(name, fromSupertypes, result, FunctionDescriptor.class);
	}

	@NotNull
	@Override
	public Set<VariableDescriptor> getProperties(@NotNull Name name)
	{
		// TODO: this should be handled by lazy property descriptors
		Set<VariableDescriptor> properties = super.getProperties(name);
		for(VariableDescriptor variableDescriptor : properties)
		{
			PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variableDescriptor;
			if(propertyDescriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
				continue;
			PsiElement element = BindingContextUtils.callableDescriptorToDeclaration(resolveSession.getTrace().getBindingContext(), propertyDescriptor);
			OverrideResolver.resolveUnknownVisibilityForMember((NapileDeclaration) element, propertyDescriptor, resolveSession.getTrace());
		}
		return properties;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void getNonDeclaredProperties(@NotNull Name name, @NotNull final Set<VariableDescriptor> result)
	{
		// Enum entries
		NapileClassOrObject classOrObjectDeclaration = declarationProvider.getClassOrObjectDeclaration(name);
		/*if(classOrObjectDeclaration instanceof NapileEnumEntry)
		{
			// TODO: This code seems to be wrong, but it mimics the present behavior of eager resolve
			NapileEnumEntry jetEnumEntry = (NapileEnumEntry) classOrObjectDeclaration;
			if(!jetEnumEntry.hasPrimaryConstructor())
			{
				VariableDescriptor propertyDescriptor = resolveSession.getInjector().getDescriptorResolver().resolveObjectDeclarationAsPropertyDescriptor(thisDescriptor, jetEnumEntry, resolveSession.getClassDescriptor(jetEnumEntry), resolveSession.getTrace());
				result.add(propertyDescriptor);
			}
		}   */

		// Members from supertypes
		Collection<PropertyDescriptor> fromSupertypes = Lists.newArrayList();
		for(JetType supertype : thisDescriptor.getTypeConstructor().getSupertypes())
		{
			fromSupertypes.addAll((Set) supertype.getMemberScope().getProperties(name));
		}

		generateFakeOverrides(name, fromSupertypes, (Set) result, PropertyDescriptor.class);
	}

	@Override
	protected void addExtraDescriptors()
	{
		for(JetType supertype : thisDescriptor.getTypeConstructor().getSupertypes())
		{
			for(DeclarationDescriptor descriptor : supertype.getMemberScope().getAllDescriptors())
			{
				if(descriptor instanceof FunctionDescriptor)
				{
					getFunctions(descriptor.getName());
				}
				else if(descriptor instanceof PropertyDescriptor)
				{
					getProperties(descriptor.getName());
				}
				// Nothing else is inherited
			}
		}
	}

	@Override
	public NamespaceDescriptor getNamespace(@NotNull Name name)
	{
		return null;
	}

	@NotNull
	@Override
	public ReceiverDescriptor getImplicitReceiver()
	{
		return thisDescriptor.getImplicitReceiver();
	}

	@NotNull
	public Map<NapileDelegationSpecifierListOwner, ConstructorDescriptor> getConstructors()
	{
		if(constructorDescriptors == null)
		{
			constructorDescriptors = new LinkedHashMap<NapileDelegationSpecifierListOwner, ConstructorDescriptor>();
			if(EnumSet.of(ClassKind.CLASS, ClassKind.OBJECT, ClassKind.ENUM_CLASS).contains(thisDescriptor.getKind()))
			{
				NapileClassOrObject classOrObject = declarationProvider.getOwnerInfo().getCorrespondingClassOrObject();
				if(thisDescriptor.getKind() != ClassKind.OBJECT)
				{
					NapileClass napileClass = (NapileClass) classOrObject;
					for(NapileConstructor constructor : napileClass.getConstructors())
					{
						ConstructorDescriptor constructorDescriptor = resolveSession.getInjector().getDescriptorResolver().resolveConstructorDescriptor(thisDescriptor.getScopeForClassHeaderResolution(), thisDescriptor, constructor, resolveSession.getTrace());

						constructorDescriptors.put(constructor, constructorDescriptor);

						setDeferredReturnType(constructorDescriptor);
					}
				}
				else
				{
					ConstructorDescriptor constructor = DescriptorResolver.createConstructorForObject(classOrObject, thisDescriptor, resolveSession.getTrace());
					setDeferredReturnType(constructor);
				}
			}
		}
		return constructorDescriptors;
	}

	private void setDeferredReturnType(@NotNull ConstructorDescriptor descriptor)
	{
		descriptor.setReturnType(DeferredType.create(resolveSession.getTrace(), new LazyValue<JetType>()
		{
			@Override
			protected JetType compute()
			{
				return thisDescriptor.getDefaultType();
			}
		}));
	}

	@Override
	public String toString()
	{
		// Do not add details here, they may compromise the laziness during debugging
		return "lazy scope for class " + thisDescriptor.getName();
	}
}
