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

package org.jetbrains.jet.lang.resolve;

import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS;
import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_FUNCTION_WITH_BODY;
import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS;
import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_PROPERTY_NOT_IN_CLASS;
import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_PROPERTY_WITH_GETTER;
import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_PROPERTY_WITH_INITIALIZER;
import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_PROPERTY_WITH_SETTER;
import static org.jetbrains.jet.lang.diagnostics.Errors.ENUM_ENTRY_ILLEGAL_TYPE;
import static org.jetbrains.jet.lang.diagnostics.Errors.ENUM_ENTRY_SHOULD_BE_INITIALIZED;
import static org.jetbrains.jet.lang.diagnostics.Errors.MUST_BE_INITIALIZED;
import static org.jetbrains.jet.lang.diagnostics.Errors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT;
import static org.jetbrains.jet.lang.diagnostics.Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY;
import static org.jetbrains.jet.lang.diagnostics.Errors.NON_MEMBER_ABSTRACT_FUNCTION;
import static org.jetbrains.jet.lang.diagnostics.Errors.NON_MEMBER_FUNCTION_NO_BODY;
import static org.jetbrains.jet.lang.diagnostics.Errors.PROPERTY_INITIALIZER_NO_BACKING_FIELD;
import static org.jetbrains.jet.lang.diagnostics.Errors.PROPERTY_WITH_NO_TYPE_NO_INITIALIZER;
import static org.jetbrains.jet.lang.diagnostics.Errors.PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE;
import static org.jetbrains.jet.lang.resolve.BindingContext.TYPE;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptorFromSource;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.descriptors.MutableClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyGetterDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.DeferredType;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Pair;

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
		Map<JetClass, MutableClassDescriptor> classes = bodiesResolveContext.getClasses();
		for(Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet())
		{
			JetClass aClass = entry.getKey();
			MutableClassDescriptor classDescriptor = entry.getValue();
			if(!bodiesResolveContext.completeAnalysisNeeded(aClass))
				continue;

			checkClass(aClass, classDescriptor);
			checkModifiers(aClass.getModifierList(), classDescriptor);
		}

		for(Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : bodiesResolveContext.getObjects().entrySet())
		{
			JetObjectDeclaration objectDeclaration = entry.getKey();
			MutableClassDescriptor objectDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(objectDeclaration))
				continue;
			checkObject(objectDeclaration, objectDescriptor);
		}

		for(Map.Entry<JetNamedFunction, SimpleFunctionDescriptor> entry : bodiesResolveContext.getFunctions().entrySet())
		{
			JetNamedFunction function = entry.getKey();
			SimpleFunctionDescriptor functionDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(function))
				continue;
			checkFunction(function, functionDescriptor);
			checkModifiers(function.getModifierList(), functionDescriptor);
		}

		for(Map.Entry<NapileConstructor, ConstructorDescriptor> entry : bodiesResolveContext.getConstructors().entrySet())
		{
			NapileConstructor constructor = entry.getKey();
			ConstructorDescriptor constructorDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(constructor))
				continue;
			checkConstructor(constructor, constructorDescriptor);
			checkModifiers(constructor.getModifierList(), constructorDescriptor);
		}

		for(Map.Entry<JetProperty, PropertyDescriptor> entry : bodiesResolveContext.getProperties().entrySet())
		{
			JetProperty property = entry.getKey();
			PropertyDescriptor propertyDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(property))
				continue;
			checkProperty(property, propertyDescriptor);
			checkModifiers(property.getModifierList(), propertyDescriptor);
		}
	}

	private void checkClass(JetClass aClass, MutableClassDescriptor classDescriptor)
	{
		checkEnum(aClass, classDescriptor);
	}


	private void checkObject(JetObjectDeclaration objectDeclaration, MutableClassDescriptor classDescriptor)
	{
		checkIllegalInThisContextModifiers(objectDeclaration.getModifierList(), Sets.newHashSet(JetTokens.ABSTRACT_KEYWORD, JetTokens.OVERRIDE_KEYWORD));
	}


	private void checkProperty(JetProperty property, PropertyDescriptor propertyDescriptor)
	{
		DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
		ClassDescriptor classDescriptor = (containingDeclaration instanceof ClassDescriptor) ? (ClassDescriptor) containingDeclaration : null;
		checkPropertyAbstractness(property, propertyDescriptor, classDescriptor);
		checkPropertyInitializer(property, propertyDescriptor, classDescriptor);
		checkAccessors(property, propertyDescriptor);
		checkDeclaredTypeInPublicMember(property, propertyDescriptor);
	}

	private void checkDeclaredTypeInPublicMember(JetNamedDeclaration member, CallableMemberDescriptor memberDescriptor)
	{
		boolean hasDeferredType;
		if(member instanceof JetProperty)
		{
			hasDeferredType = ((JetProperty) member).getPropertyTypeRef() == null && DescriptorResolver.hasBody((JetProperty) member);
		}
		else
		{
			assert member instanceof NapileMethod;
			NapileMethod function = (NapileMethod) member;
			hasDeferredType = function.getReturnTypeRef() == null && function.getBodyExpression() != null && !function.hasBlockBody();
		}
		if((memberDescriptor.getVisibility().isPublicAPI()) &&
				memberDescriptor.getOverriddenDescriptors().size() == 0 &&
				hasDeferredType)
		{
			trace.report(PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE.on(member));
		}
	}

	private void checkPropertyAbstractness(JetProperty property, PropertyDescriptor propertyDescriptor, ClassDescriptor classDescriptor)
	{
		JetPropertyAccessor getter = property.getGetter();
		JetPropertyAccessor setter = property.getSetter();
		JetModifierList modifierList = property.getModifierList();
		ASTNode abstractNode = modifierList != null ? modifierList.getModifierNode(JetTokens.ABSTRACT_KEYWORD) : null;

		if(abstractNode != null)
		{ //has abstract modifier
			if(classDescriptor == null)
			{
				trace.report(ABSTRACT_PROPERTY_NOT_IN_CLASS.on(property));
				return;
			}
			if(!(classDescriptor.getModality() == Modality.ABSTRACT) && classDescriptor.getKind() != ClassKind.ENUM_CLASS)
			{
				JetClass classElement = (JetClass) BindingContextUtils.classDescriptorToDeclaration(trace.getBindingContext(), classDescriptor);
				String name = property.getName();
				trace.report(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.on(property, name != null ? name : "", classDescriptor));
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

			JetExpression initializer = property.getInitializer();
			if(initializer != null)
			{
				trace.report(ABSTRACT_PROPERTY_WITH_INITIALIZER.on(initializer));
			}
			if(getter != null && getter.getBodyExpression() != null)
			{
				trace.report(ABSTRACT_PROPERTY_WITH_GETTER.on(getter));
			}
			if(setter != null && setter.getBodyExpression() != null)
			{
				trace.report(ABSTRACT_PROPERTY_WITH_SETTER.on(setter));
			}
		}
	}

	private void checkPropertyInitializer(JetProperty property, PropertyDescriptor propertyDescriptor, ClassDescriptor classDescriptor)
	{
		JetPropertyAccessor getter = property.getGetter();
		JetPropertyAccessor setter = property.getSetter();
		boolean hasAccessorImplementation = (getter != null && getter.getBodyExpression() != null) || (setter != null && setter.getBodyExpression() != null);

		if(propertyDescriptor.getModality() == Modality.ABSTRACT)
		{
			if(property.getInitializer() == null && property.getPropertyTypeRef() == null)
			{
				trace.report(PROPERTY_WITH_NO_TYPE_NO_INITIALIZER.on(property));
			}
			return;
		}

		JetExpression initializer = property.getInitializer();
		boolean backingFieldRequired = trace.getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor);

		if(initializer == null)
		{
			boolean error = false;
			if(backingFieldRequired && !trace.getBindingContext().get(BindingContext.IS_INITIALIZED, propertyDescriptor))
			{
				if(classDescriptor == null || hasAccessorImplementation)
				{
					error = true;
					trace.report(MUST_BE_INITIALIZED.on(property));
				}
				else
				{
					error = true;
					trace.report(MUST_BE_INITIALIZED_OR_BE_ABSTRACT.on(property));
				}
			}
			if(!error && property.getPropertyTypeRef() == null)
			{
				trace.report(PROPERTY_WITH_NO_TYPE_NO_INITIALIZER.on(property));
			}
			return;
		}

		else if(!backingFieldRequired)
		{
			trace.report(PROPERTY_INITIALIZER_NO_BACKING_FIELD.on(initializer));
		}
	}

	protected void checkFunction(JetNamedFunction function, SimpleFunctionDescriptor functionDescriptor)
	{
		DeclarationDescriptor containingDescriptor = functionDescriptor.getContainingDeclaration();
		boolean hasAbstractModifier = function.hasModifier(JetTokens.ABSTRACT_KEYWORD);
		checkDeclaredTypeInPublicMember(function, functionDescriptor);
		if(containingDescriptor instanceof ClassDescriptor)
		{
			ClassDescriptor classDescriptor = (ClassDescriptor) containingDescriptor;
			boolean inEnum = classDescriptor.getKind() == ClassKind.ENUM_CLASS;
			boolean inAbstractClass = classDescriptor.getModality() == Modality.ABSTRACT;
			if(hasAbstractModifier && !inAbstractClass && !inEnum)
			{
				JetClass classElement = (JetClass) BindingContextUtils.classDescriptorToDeclaration(trace.getBindingContext(), classDescriptor);
				trace.report(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.on(function, functionDescriptor.getName().getName(), classDescriptor));
			}
			if(function.getBodyExpression() != null && hasAbstractModifier)
			{
				trace.report(ABSTRACT_FUNCTION_WITH_BODY.on(function, functionDescriptor));
			}
			if(function.getBodyExpression() == null && !hasAbstractModifier)
			{
				trace.report(NON_ABSTRACT_FUNCTION_WITH_NO_BODY.on(function, functionDescriptor));
			}
			return;
		}
		if(hasAbstractModifier)
		{
			trace.report(NON_MEMBER_ABSTRACT_FUNCTION.on(function, functionDescriptor));
		}
		if(function.getBodyExpression() == null && !hasAbstractModifier)
		{
			trace.report(NON_MEMBER_FUNCTION_NO_BODY.on(function, functionDescriptor));
		}
	}

	private void checkConstructor(NapileConstructor constructor, ConstructorDescriptor constructorDescriptor)
	{
		//TODO [VISTALL]
	}

	private void checkAccessors(JetProperty property, PropertyDescriptor propertyDescriptor)
	{
		for(JetPropertyAccessor accessor : property.getAccessors())
		{
			checkIllegalInThisContextModifiers(accessor.getModifierList(), Sets.newHashSet(JetTokens.ABSTRACT_KEYWORD, JetTokens.FINAL_KEYWORD, JetTokens.OVERRIDE_KEYWORD));
		}
		JetPropertyAccessor getter = property.getGetter();
		PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getGetter();
		JetModifierList getterModifierList = getter != null ? getter.getModifierList() : null;
		if(getterModifierList != null && getterDescriptor != null)
		{
			Map<JetKeywordToken, ASTNode> nodes = getNodesCorrespondingToModifiers(getterModifierList, Sets.newHashSet(JetTokens.PUBLIC_KEYWORD, JetTokens.COVERED_KEYWORD, JetTokens.LOCAL_KEYWORD, JetTokens.INTERNAL_KEYWORD));
			if(getterDescriptor.getVisibility() != propertyDescriptor.getVisibility())
			{
				for(ASTNode node : nodes.values())
				{
					trace.report(Errors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY.on(node.getPsi()));
				}
			}
			else
			{
				for(ASTNode node : nodes.values())
				{
					trace.report(Errors.REDUNDANT_MODIFIER_IN_GETTER.on(node.getPsi()));
				}
			}
		}
	}

	private void checkModifiers(@Nullable JetModifierList modifierList, @NotNull DeclarationDescriptor descriptor)
	{
		checkModalityModifiers(modifierList);
		checkVisibilityModifiers(modifierList, descriptor);
	}

	private void checkModalityModifiers(@Nullable JetModifierList modifierList)
	{
		if(modifierList == null)
			return;

		checkCompatibility(modifierList, Lists.newArrayList(JetTokens.ABSTRACT_KEYWORD, JetTokens.FINAL_KEYWORD), Lists.<JetToken>newArrayList(JetTokens.ABSTRACT_KEYWORD));
	}

	private void checkVisibilityModifiers(@Nullable JetModifierList modifierList, @NotNull DeclarationDescriptor descriptor)
	{
		if(modifierList == null)
			return;

		DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
		if(containingDeclaration instanceof NamespaceDescriptor)
		{
			if(modifierList.hasModifier(JetTokens.HERITABLE_KEYWORD))
			{
				trace.report(Errors.PACKAGE_MEMBER_CANNOT_BE_HERITABLE.on(modifierList.getModifierNode(JetTokens.HERITABLE_KEYWORD).getPsi()));
			}
		}

		checkCompatibility(modifierList, Lists.newArrayList(JetTokens.LOCAL_KEYWORD, JetTokens.COVERED_KEYWORD, JetTokens.PUBLIC_KEYWORD, JetTokens.INTERNAL_KEYWORD));
	}

	private void checkCompatibility(@Nullable JetModifierList modifierList, Collection<JetKeywordToken> availableModifiers, Collection<JetToken>... availableCombinations)
	{
		if(modifierList == null)
			return;
		Collection<JetKeywordToken> presentModifiers = Sets.newLinkedHashSet();
		for(JetKeywordToken modifier : availableModifiers)
		{
			if(modifierList.hasModifier(modifier))
			{
				presentModifiers.add(modifier);
			}
		}
		if(presentModifiers.size() == 1)
		{
			return;
		}
		for(Collection<JetToken> combination : availableCombinations)
		{
			if(presentModifiers.containsAll(combination) && combination.containsAll(presentModifiers))
			{
				return;
			}
		}
		for(JetKeywordToken token : presentModifiers)
		{
			trace.report(Errors.INCOMPATIBLE_MODIFIERS.on(modifierList.getModifierNode(token).getPsi(), presentModifiers));
		}
	}

	private void checkRedundantModifier(@NotNull JetModifierList modifierList, Pair<JetKeywordToken, JetKeywordToken>... redundantBundles)
	{
		for(Pair<JetKeywordToken, JetKeywordToken> tokenPair : redundantBundles)
		{
			JetKeywordToken redundantModifier = tokenPair.getFirst();
			JetKeywordToken sufficientModifier = tokenPair.getSecond();
			if(modifierList.hasModifier(redundantModifier) && modifierList.hasModifier(sufficientModifier))
			{
				trace.report(Errors.REDUNDANT_MODIFIER.on(modifierList.getModifierNode(redundantModifier).getPsi(), redundantModifier, sufficientModifier));
			}
		}
	}

	private void checkIllegalInThisContextModifiers(@Nullable JetModifierList modifierList, Collection<JetKeywordToken> illegalModifiers)
	{
		if(modifierList == null)
			return;
		for(JetKeywordToken modifier : illegalModifiers)
		{
			if(modifierList.hasModifier(modifier))
			{
				trace.report(Errors.ILLEGAL_MODIFIER.on(modifierList.getModifierNode(modifier).getPsi(), modifier));
			}
		}
	}

	@NotNull
	public static Map<JetKeywordToken, ASTNode> getNodesCorrespondingToModifiers(@NotNull JetModifierList modifierList, Collection<JetKeywordToken> possibleModifiers)
	{
		Map<JetKeywordToken, ASTNode> nodes = Maps.newHashMap();
		for(JetKeywordToken modifier : possibleModifiers)
		{
			if(modifierList.hasModifier(modifier))
			{
				nodes.put(modifier, modifierList.getModifierNode(modifier));
			}
		}
		return nodes;
	}

	private void checkEnum(JetClass aClass, ClassDescriptor classDescriptor)
	{
		if(classDescriptor.getKind() != ClassKind.ENUM_ENTRY)
			return;

		DeclarationDescriptor declaration = classDescriptor.getContainingDeclaration().getContainingDeclaration();
		assert declaration instanceof ClassDescriptor;
		ClassDescriptorFromSource enumClass = (ClassDescriptorFromSource) declaration;
		assert enumClass.getKind() == ClassKind.ENUM_CLASS;

		List<JetDelegationSpecifier> delegationSpecifiers = aClass.getDelegationSpecifiers();
		ConstructorDescriptor constructor = enumClass.getUnsubstitutedPrimaryConstructor();
		assert constructor != null;
		if(!constructor.getValueParameters().isEmpty() && delegationSpecifiers.isEmpty())
		{
			trace.report(ENUM_ENTRY_SHOULD_BE_INITIALIZED.on(aClass, enumClass));
		}

		for(JetDelegationSpecifier delegationSpecifier : delegationSpecifiers)
		{
			JetTypeReference typeReference = delegationSpecifier.getTypeReference();
			if(typeReference != null)
			{
				JetType type = trace.getBindingContext().get(TYPE, typeReference);
				if(type != null)
				{
					JetType enumType = enumClass.getDefaultType();
					if(!type.getConstructor().equals(enumType.getConstructor()))
					{
						trace.report(ENUM_ENTRY_ILLEGAL_TYPE.on(typeReference, enumClass));
					}
				}
			}
		}
	}
}
