/*
 * Copyright 2010-2013 napile.org
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

package org.napile.compiler.lang.resolve.processors;

import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.psi.NapileAnonymClass;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.JetType;

/**
 * @author VISTALL
 * @date 14:59/12.01.13
 */
public class AnonymClassResolver
{
	@NotNull
	private OverrideResolver overrideResolver;

	@NotNull
	private BodyResolver bodyResolver;

	@NotNull
	private DescriptorResolver descriptorResolver;

	@NotNull
	private DeclarationResolver declarationResolver;

	@Inject
	public void setOverrideResolver(@NotNull OverrideResolver overrideResolver)
	{
		this.overrideResolver = overrideResolver;
	}

	@Inject
	public void setBodyResolver(@NotNull BodyResolver bodyResolver)
	{
		this.bodyResolver = bodyResolver;
	}

	@Inject
	public void setDescriptorResolver(@NotNull DescriptorResolver descriptorResolver)
	{
		this.descriptorResolver = descriptorResolver;
	}

	@Inject
	public void setDeclarationResolver(@NotNull DeclarationResolver declarationResolver)
	{
		this.declarationResolver = declarationResolver;
	}

	@NotNull
	public MutableClassDescriptor resolveAnonymClass(@NotNull DeclarationDescriptor owner, @NotNull final JetScope scope, @NotNull final BindingTrace bindingTrace, @NotNull NapileAnonymClass anonymClass)
	{
		final MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(owner, scope, ClassKind.ANONYM_CLASS, anonymClass.getNameAsSafeName(), Collections.<AnnotationDescriptor>emptyList(), false);

		bodyResolver.getContext().getAnonymous().put(anonymClass, mutableClassDescriptor);

		ConstructorDescriptor constructorDescriptor = new ConstructorDescriptor(mutableClassDescriptor, Collections.<AnnotationDescriptor>emptyList(), false);
		constructorDescriptor.initialize(Collections.<TypeParameterDescriptor>emptyList(), Collections.<CallParameterDescriptor>emptyList(), Visibility.PUBLIC);
		mutableClassDescriptor.addConstructor(constructorDescriptor);

		bindingTrace.record(BindingContext.CONSTRUCTOR, anonymClass, constructorDescriptor);

		bindingTrace.record(BindingContext.CLASS, anonymClass, mutableClassDescriptor);

		for(JetType type : descriptorResolver.resolveSupertypes(mutableClassDescriptor.getScopeForSupertypeResolution(), anonymClass, bindingTrace))
			mutableClassDescriptor.addSupertype(type);

		mutableClassDescriptor.setVisibility(Visibility.PUBLIC);
		mutableClassDescriptor.setModality(Modality.FINAL);
		mutableClassDescriptor.setTypeParameterDescriptors(Collections.<TypeParameterDescriptor>emptyList());
		mutableClassDescriptor.createTypeConstructor();

		declarationResolver.resolveInsideDeclarations(anonymClass, mutableClassDescriptor.getScopeForMemberResolution(), mutableClassDescriptor);

		bodyResolver.resolveDelegationSpecifierList(anonymClass, mutableClassDescriptor, mutableClassDescriptor.getScopeForSupertypeResolution(), true);

		for(NapileDeclaration declaration : anonymClass.getDeclarations())
		{
			declaration.accept(new NapileVisitorVoid()
			{
				@Override
				public void visitVariable(NapileVariable property)
				{
					NapileExpression initializer = property.getInitializer();
					if(initializer != null)
						bodyResolver.resolvePropertyInitializer(property, bindingTrace.safeGet(BindingContext.VARIABLE, property), initializer, mutableClassDescriptor.getScopeForMemberResolution());
				}

				@Override
				public void visitNamedMethodOrMacro(NapileNamedMethodOrMacro method)
				{
					bodyResolver.resolveBody(bindingTrace, method, bindingTrace.safeGet(BindingContext.METHOD, method), mutableClassDescriptor.getScopeForMemberResolution(), false);
				}
			});
		}
		overrideResolver.doGenerateOverridesInAClass(mutableClassDescriptor);

		overrideResolver.checkOverridesInAClass(mutableClassDescriptor, anonymClass);

		Collection<CallableMemberDescriptor> members = mutableClassDescriptor.getAllCallableMembers();
		for(CallableMemberDescriptor member : members)
			overrideResolver.checkOverridesForParameters(member);
		return mutableClassDescriptor;
	}
}
