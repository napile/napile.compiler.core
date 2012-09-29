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

package org.napile.compiler.lang.resolve.processors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileDelegationSpecifier;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.lang.types.JetType;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author svtk
 */
public class DeclarationsChecker
{
	@NotNull
	private BindingTrace trace;

	@Inject
	public void setTrace(@NotNull BindingTrace trace)
	{
		this.trace = trace;
	}

	public void process(@NotNull BodiesResolveContext bodiesResolveContext)
	{
		for(Map.Entry<NapileConstructor, ConstructorDescriptor> entry : bodiesResolveContext.getConstructors().entrySet())
		{
			NapileConstructor constructor = entry.getKey();
			ConstructorDescriptor constructorDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(constructor))
				continue;

			checkConstructor(constructor, constructorDescriptor);
		}

		for(Map.Entry<NapileProperty, PropertyDescriptor> entry : bodiesResolveContext.getProperties().entrySet())
		{
			NapileProperty property = entry.getKey();
			PropertyDescriptor propertyDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(property))
				continue;

			checkProperty(property, propertyDescriptor);
		}
	}

	private void checkProperty(NapileProperty property, PropertyDescriptor propertyDescriptor)
	{
		DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
		ClassDescriptor classDescriptor = (containingDeclaration instanceof ClassDescriptor) ? (ClassDescriptor) containingDeclaration : null;

		checkPropertyAbstractness(property, propertyDescriptor, classDescriptor);

		checkPropertyInitializer(property, propertyDescriptor, classDescriptor);
	}

	private void checkPropertyAbstractness(NapileProperty property, PropertyDescriptor propertyDescriptor, ClassDescriptor classDescriptor)
	{
		/*NapilePropertyAccessor getter = property.getGetter();
		NapilePropertyAccessor setter = property.getSetter();
		NapileModifierList modifierList = property.getModifierList();
		ASTNode abstractNode = modifierList != null ? modifierList.getModifierNode(NapileTokens.ABSTRACT_KEYWORD) : null;

		if(abstractNode != null)
		{ //has abstract modifier
			if(classDescriptor == null)
			{
				trace.report(Errors.ABSTRACT_PROPERTY_NOT_IN_CLASS.on(property));
				return;
			}
			if(!(classDescriptor.getModality() == Modality.ABSTRACT) && classDescriptor.getKind() != ClassKind.ENUM_CLASS)
			{
				NapileClass classElement = (NapileClass) BindingContextUtils.classDescriptorToDeclaration(trace.getBindingContext(), classDescriptor);
				String name = property.getName();
				trace.report(Errors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.on(property, name != null ? name : "", classDescriptor));
				return;
			}
		}

		if(propertyDescriptor.getModality() == Modality.ABSTRACT)
		{
			JetType returnType = propertyDescriptor.getReturnType();
			if(returnType instanceof DeferredType)
			{
				returnType = ((DeferredType) returnType).getActualType();
			}

			NapileExpression initializer = property.getInitializer();
			if(initializer != null)
			{
				trace.report(Errors.ABSTRACT_PROPERTY_WITH_INITIALIZER.on(initializer));
			}
			if(getter != null && getter.getBodyExpression() != null)
			{
				trace.report(Errors.ABSTRACT_PROPERTY_WITH_GETTER.on(getter));
			}
			if(setter != null && setter.getBodyExpression() != null)
			{
				trace.report(Errors.ABSTRACT_PROPERTY_WITH_SETTER.on(setter));
			}
		}  */
	}

	private void checkPropertyInitializer(NapileProperty property, PropertyDescriptor propertyDescriptor, @NotNull ClassDescriptor classDescriptor)
	{
		/*NapilePropertyAccessor getter = property.getGetter();
		NapilePropertyAccessor setter = property.getSetter();
		boolean hasAccessorImplementation = (getter != null && getter.getBodyExpression() != null) || (setter != null && setter.getBodyExpression() != null);

		if(propertyDescriptor.getModality() == Modality.ABSTRACT)
		{
			if(property.getInitializer() == null && property.getPropertyTypeRef() == null)
			{
				trace.report(Errors.PROPERTY_WITH_NO_TYPE_NO_INITIALIZER.on(property));
			}
			return;
		}

		NapileExpression initializer = property.getInitializer();
		boolean backingFieldRequired = trace.getBindingContext().safeGet(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor);

		if(initializer == null)
		{
			boolean error = false;
			if(backingFieldRequired && !trace.getBindingContext().safeGet(BindingContext.IS_INITIALIZED, propertyDescriptor))
			{
				error = true;
				if(hasAccessorImplementation || propertyDescriptor.isStatic())
					trace.report(Errors.MUST_BE_INITIALIZED.on(property));
				else
					trace.report(Errors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT.on(property));
			}

			if(!error && property.getPropertyTypeRef() == null)
				trace.report(Errors.PROPERTY_WITH_NO_TYPE_NO_INITIALIZER.on(property));
		}
		else if(!backingFieldRequired)
		{
			trace.report(Errors.PROPERTY_INITIALIZER_NO_BACKING_FIELD.on(initializer));
		}   */
	}

	private void checkConstructor(NapileConstructor constructor, ConstructorDescriptor constructorDescriptor)
	{
		NapileClass parent = PsiTreeUtil.getParentOfType(constructor, NapileClass.class);

		assert parent != null;

		//TODO [VISTALL] rework for this(a : Int) : this()
		Map<NapileTypeReference, JetType> classSpecifiers = makeTypeList2(parent.getExtendTypeList());
		Map<NapileTypeReference, JetType> constructorSpecifiers = makeTypeList(constructor.getDelegationSpecifiers());

		for(Map.Entry<NapileTypeReference, JetType> classEntry : classSpecifiers.entrySet())
		{
			if(!constructorSpecifiers.values().contains(classEntry.getValue()))
				trace.report(Errors.MISSED_SUPER_CALL.on(constructor.getNameIdentifier(), classEntry.getValue()));
		}

		for(Map.Entry<NapileTypeReference, JetType> constructorEntry : constructorSpecifiers.entrySet())
		{
			if(!classSpecifiers.values().contains(constructorEntry.getValue()))
				trace.report(Errors.INVALID_SUPER_CALL.on(constructorEntry.getKey()));
		}
	}

	@NotNull
	private Map<NapileTypeReference, JetType> makeTypeList2(@NotNull List<NapileTypeReference> list)
	{
		Map<NapileTypeReference, JetType> types = new LinkedHashMap<NapileTypeReference, JetType>(list.size());
		for(NapileTypeReference typeReference : list)
		{
			JetType type = trace.get(BindingContext.TYPE, typeReference);
			types.put(typeReference, type);
		}

		return types;
	}

	@NotNull
	private Map<NapileTypeReference, JetType> makeTypeList(@NotNull List<NapileDelegationSpecifier> list)
	{
		Map<NapileTypeReference, JetType> types = new LinkedHashMap<NapileTypeReference, JetType>(list.size());
		for(NapileDelegationSpecifier delegationSpecifier : list)
		{
			JetType type = trace.get(BindingContext.TYPE, delegationSpecifier.getTypeReference());
			types.put(delegationSpecifier.getTypeReference(), type);
		}

		return types;
	}
}
