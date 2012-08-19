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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.types.DeferredType;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lexer.NapileKeywordToken;
import org.napile.compiler.lexer.NapileToken;
import org.napile.compiler.lexer.JetTokens;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.psi.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Pair;
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
		Map<NapileClass, MutableClassDescriptor> classes = bodiesResolveContext.getClasses();
		for(Map.Entry<NapileClass, MutableClassDescriptor> entry : classes.entrySet())
		{
			NapileClass aClass = entry.getKey();
			MutableClassDescriptor classDescriptor = entry.getValue();
			if(!bodiesResolveContext.completeAnalysisNeeded(aClass))
				continue;

			checkClass(aClass, classDescriptor);
			checkModifiers(aClass.getModifierList(), classDescriptor);
		}

		for(Map.Entry<NapileObjectDeclaration, MutableClassDescriptor> entry : bodiesResolveContext.getObjects().entrySet())
		{
			NapileObjectDeclaration objectDeclaration = entry.getKey();
			MutableClassDescriptor objectDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(objectDeclaration))
				continue;
			checkObject(objectDeclaration, objectDescriptor);
		}

		for(Map.Entry<NapileNamedFunction, SimpleFunctionDescriptor> entry : bodiesResolveContext.getFunctions().entrySet())
		{
			NapileNamedFunction function = entry.getKey();
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

		for(Map.Entry<NapileProperty, PropertyDescriptor> entry : bodiesResolveContext.getProperties().entrySet())
		{
			NapileProperty property = entry.getKey();
			PropertyDescriptor propertyDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(property))
				continue;
			checkProperty(property, propertyDescriptor);
			checkModifiers(property.getModifierList(), propertyDescriptor);
		}
	}

	private void checkClass(NapileClass aClass, MutableClassDescriptor classDescriptor)
	{
		checkEnum(aClass, classDescriptor);
	}


	private void checkObject(NapileObjectDeclaration objectDeclaration, MutableClassDescriptor classDescriptor)
	{
		checkIllegalInThisContextModifiers(objectDeclaration.getModifierList(), Sets.newHashSet(JetTokens.ABSTRACT_KEYWORD, JetTokens.OVERRIDE_KEYWORD));
	}


	private void checkProperty(NapileProperty property, PropertyDescriptor propertyDescriptor)
	{
		DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
		ClassDescriptor classDescriptor = (containingDeclaration instanceof ClassDescriptor) ? (ClassDescriptor) containingDeclaration : null;
		checkPropertyAbstractness(property, propertyDescriptor, classDescriptor);
		checkPropertyInitializer(property, propertyDescriptor, classDescriptor);
		checkAccessors(property, propertyDescriptor);
		checkDeclaredTypeInPublicMember(property, propertyDescriptor);
	}

	private void checkDeclaredTypeInPublicMember(NapileNamedDeclaration member, CallableMemberDescriptor memberDescriptor)
	{
		boolean hasDeferredType;
		if(member instanceof NapileProperty)
		{
			hasDeferredType = ((NapileProperty) member).getPropertyTypeRef() == null && DescriptorResolver.hasBody((NapileProperty) member);
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
			trace.report(Errors.PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE.on(member));
		}
	}

	private void checkPropertyAbstractness(NapileProperty property, PropertyDescriptor propertyDescriptor, ClassDescriptor classDescriptor)
	{
		NapilePropertyAccessor getter = property.getGetter();
		NapilePropertyAccessor setter = property.getSetter();
		NapileModifierList modifierList = property.getModifierList();
		ASTNode abstractNode = modifierList != null ? modifierList.getModifierNode(JetTokens.ABSTRACT_KEYWORD) : null;

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
		}
	}

	private void checkPropertyInitializer(NapileProperty property, PropertyDescriptor propertyDescriptor, ClassDescriptor classDescriptor)
	{
		NapilePropertyAccessor getter = property.getGetter();
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
		boolean backingFieldRequired = trace.getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor);

		if(initializer == null)
		{
			boolean error = false;
			if(backingFieldRequired && !trace.getBindingContext().get(BindingContext.IS_INITIALIZED, propertyDescriptor))
			{
				if(classDescriptor == null || hasAccessorImplementation)
				{
					error = true;
					trace.report(Errors.MUST_BE_INITIALIZED.on(property));
				}
				else
				{
					error = true;
					trace.report(Errors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT.on(property));
				}
			}
			if(!error && property.getPropertyTypeRef() == null)
			{
				trace.report(Errors.PROPERTY_WITH_NO_TYPE_NO_INITIALIZER.on(property));
			}
			return;
		}

		else if(!backingFieldRequired)
		{
			trace.report(Errors.PROPERTY_INITIALIZER_NO_BACKING_FIELD.on(initializer));
		}
	}

	protected void checkFunction(NapileNamedFunction function, SimpleFunctionDescriptor functionDescriptor)
	{
		DeclarationDescriptor containingDescriptor = functionDescriptor.getContainingDeclaration();
		boolean hasAbstractModifier = function.hasModifier(JetTokens.ABSTRACT_KEYWORD);
		checkDeclaredTypeInPublicMember(function, functionDescriptor);

		ClassDescriptor classDescriptor = (ClassDescriptor) containingDescriptor;
		boolean inEnum = classDescriptor.getKind() == ClassKind.ENUM_CLASS;
		boolean inAbstractClass = classDescriptor.getModality() == Modality.ABSTRACT;
		if(hasAbstractModifier && !inAbstractClass && !inEnum)
		{
			NapileClass classElement = (NapileClass) BindingContextUtils.classDescriptorToDeclaration(trace.getBindingContext(), classDescriptor);
			trace.report(Errors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.on(function, functionDescriptor.getName().getName(), classDescriptor));
		}
		if(function.getBodyExpression() != null && hasAbstractModifier)
		{
			trace.report(Errors.ABSTRACT_FUNCTION_WITH_BODY.on(function, functionDescriptor));
		}
		if(function.getBodyExpression() == null && !hasAbstractModifier)
		{
			trace.report(Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY.on(function, functionDescriptor));
		}
	}

	private void checkConstructor(NapileConstructor constructor, ConstructorDescriptor constructorDescriptor)
	{
		checkIllegalInThisContextModifiers(constructor.getModifierList(), Sets.newHashSet(JetTokens.ABSTRACT_KEYWORD, JetTokens.FINAL_KEYWORD, JetTokens.OVERRIDE_KEYWORD, JetTokens.STATIC_KEYWORD));

		NapileClass parent = PsiTreeUtil.getParentOfType(constructor, NapileClass.class);

		assert parent != null;

		//TODO [VISTALL] rework for this(a : Int) : this()
		Map<NapileTypeReference, JetType> classSpecifiers = makeTypeList(parent.getDelegationSpecifiers());
		Map<NapileTypeReference, JetType> constructorSpecifiers = makeTypeList(constructor.getDelegationSpecifiers());

		for(Map.Entry<NapileTypeReference, JetType> classEntry : classSpecifiers.entrySet())
		{
			if(!constructorSpecifiers.values().contains(classEntry.getValue()))
				trace.report(Errors.MISSED_SUPER_CALL.on(constructor.getNameNode().getPsi(), classEntry.getValue()));
		}

		for(Map.Entry<NapileTypeReference, JetType> constructorEntry : constructorSpecifiers.entrySet())
		{
			if(!classSpecifiers.values().contains(constructorEntry.getValue()))
				trace.report(Errors.INVALID_SUPER_CALL.on(constructorEntry.getKey()));
		}
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

	private void checkAccessors(NapileProperty property, PropertyDescriptor propertyDescriptor)
	{
		for(NapilePropertyAccessor accessor : property.getAccessors())
		{
			checkIllegalInThisContextModifiers(accessor.getModifierList(), Sets.newHashSet(JetTokens.ABSTRACT_KEYWORD, JetTokens.FINAL_KEYWORD, JetTokens.OVERRIDE_KEYWORD));
		}
		NapilePropertyAccessor getter = property.getGetter();
		PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getGetter();
		NapileModifierList getterModifierList = getter != null ? getter.getModifierList() : null;
		if(getterModifierList != null && getterDescriptor != null)
		{
			Map<NapileKeywordToken, ASTNode> nodes = getNodesCorrespondingToModifiers(getterModifierList, Sets.newHashSet(JetTokens.COVERED_KEYWORD, JetTokens.LOCAL_KEYWORD));
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

	private void checkModifiers(@Nullable NapileModifierList modifierList, @NotNull DeclarationDescriptor descriptor)
	{
		checkModalityModifiers(modifierList);
		checkVisibilityModifiers(modifierList, descriptor);
	}

	private void checkModalityModifiers(@Nullable NapileModifierList modifierList)
	{
		if(modifierList == null)
			return;

		checkCompatibility(modifierList, Lists.newArrayList(JetTokens.ABSTRACT_KEYWORD, JetTokens.FINAL_KEYWORD), Lists.<NapileToken>newArrayList(JetTokens.ABSTRACT_KEYWORD));
	}

	private void checkVisibilityModifiers(@Nullable NapileModifierList modifierList, @NotNull DeclarationDescriptor descriptor)
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

		checkCompatibility(modifierList, Lists.newArrayList(JetTokens.LOCAL_KEYWORD, JetTokens.COVERED_KEYWORD));
	}

	private void checkCompatibility(@Nullable NapileModifierList modifierList, Collection<NapileKeywordToken> availableModifiers, Collection<NapileToken>... availableCombinations)
	{
		if(modifierList == null)
			return;
		Collection<NapileKeywordToken> presentModifiers = Sets.newLinkedHashSet();
		for(NapileKeywordToken modifier : availableModifiers)
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
		for(Collection<NapileToken> combination : availableCombinations)
		{
			if(presentModifiers.containsAll(combination) && combination.containsAll(presentModifiers))
			{
				return;
			}
		}
		for(NapileKeywordToken token : presentModifiers)
		{
			trace.report(Errors.INCOMPATIBLE_MODIFIERS.on(modifierList.getModifierNode(token).getPsi(), presentModifiers));
		}
	}

	private void checkRedundantModifier(@NotNull NapileModifierList modifierList, Pair<NapileKeywordToken, NapileKeywordToken>... redundantBundles)
	{
		for(Pair<NapileKeywordToken, NapileKeywordToken> tokenPair : redundantBundles)
		{
			NapileKeywordToken redundantModifier = tokenPair.getFirst();
			NapileKeywordToken sufficientModifier = tokenPair.getSecond();
			if(modifierList.hasModifier(redundantModifier) && modifierList.hasModifier(sufficientModifier))
			{
				trace.report(Errors.REDUNDANT_MODIFIER.on(modifierList.getModifierNode(redundantModifier).getPsi(), redundantModifier, sufficientModifier));
			}
		}
	}

	private void checkIllegalInThisContextModifiers(@Nullable NapileModifierList modifierList, Collection<NapileKeywordToken> illegalModifiers)
	{
		if(modifierList == null)
			return;
		for(NapileKeywordToken modifier : illegalModifiers)
		{
			if(modifierList.hasModifier(modifier))
			{
				trace.report(Errors.ILLEGAL_MODIFIER.on(modifierList.getModifierNode(modifier).getPsi(), modifier));
			}
		}
	}

	@NotNull
	public static Map<NapileKeywordToken, ASTNode> getNodesCorrespondingToModifiers(@NotNull NapileModifierList modifierList, Collection<NapileKeywordToken> possibleModifiers)
	{
		Map<NapileKeywordToken, ASTNode> nodes = Maps.newHashMap();
		for(NapileKeywordToken modifier : possibleModifiers)
		{
			if(modifierList.hasModifier(modifier))
			{
				nodes.put(modifier, modifierList.getModifierNode(modifier));
			}
		}
		return nodes;
	}

	private void checkEnum(NapileClass aClass, ClassDescriptor classDescriptor)
	{
		if(classDescriptor.getKind() != ClassKind.ENUM_ENTRY)
			return;

		DeclarationDescriptor declaration = classDescriptor.getContainingDeclaration().getContainingDeclaration();
		assert declaration instanceof ClassDescriptor;
		ClassDescriptorFromSource enumClass = (ClassDescriptorFromSource) declaration;
		assert enumClass.getKind() == ClassKind.ENUM_CLASS;

		List<NapileDelegationSpecifier> delegationSpecifiers = aClass.getDelegationSpecifiers();
		ConstructorDescriptor constructor = enumClass.getUnsubstitutedPrimaryConstructor();
		assert constructor != null;
		if(!constructor.getValueParameters().isEmpty() && delegationSpecifiers.isEmpty())
		{
			trace.report(Errors.ENUM_ENTRY_SHOULD_BE_INITIALIZED.on(aClass, enumClass));
		}

		for(NapileDelegationSpecifier delegationSpecifier : delegationSpecifiers)
		{
			NapileTypeReference typeReference = delegationSpecifier.getTypeReference();
			if(typeReference != null)
			{
				JetType type = trace.getBindingContext().get(BindingContext.TYPE, typeReference);
				if(type != null)
				{
					JetType enumType = enumClass.getDefaultType();
					if(!type.getConstructor().equals(enumType.getConstructor()))
					{
						trace.report(Errors.ENUM_ENTRY_ILLEGAL_TYPE.on(typeReference, enumClass));
					}
				}
			}
		}
	}
}
