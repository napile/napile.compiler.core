/*
 * Copyright 2010-2012 napile.org
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.lexer.JetTokens;
import org.napile.compiler.lexer.NapileKeywordToken;
import org.napile.compiler.lexer.NapileToken;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;

/**
 * @author VISTALL
 * @date 19:52/28.08.12
 */
public class ModifiersChecker
{
	// not create new list every checks - it good for compiler, but as plugin create leaks
	@NotNull
	private static final List<NapileKeywordToken> INVALID_MODIFIERS_FOR_CLASS = Arrays.asList(JetTokens.OVERRIDE_KEYWORD, JetTokens.NATIVE_KEYWORD);
	@NotNull
	private static final List<NapileKeywordToken> INVALID_MODIFIERS_FOR_CONSTRUCTOR = Arrays.asList(JetTokens.ABSTRACT_KEYWORD, JetTokens.NATIVE_KEYWORD, JetTokens.STATIC_KEYWORD, JetTokens.OVERRIDE_KEYWORD, JetTokens.FINAL_KEYWORD);

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
			NapileClass clazz = entry.getKey();
			//MutableClassDescriptor classDescriptor = entry.getValue();
			if(!bodiesResolveContext.completeAnalysisNeeded(clazz))
				continue;

			if(clazz.getParent() instanceof NapileFile)
			{
				checkIllegalInThisContextModifiers(clazz, Arrays.asList(JetTokens.HERITABLE_KEYWORD));
				checkRedundant(clazz, Arrays.asList(JetTokens.STATIC_KEYWORD));
			}

			checkIllegalInThisContextModifiers(clazz, INVALID_MODIFIERS_FOR_CLASS);
			checkModalityModifiers(clazz);
		}

		for(Map.Entry<NapileNamedFunction, SimpleMethodDescriptor> entry : bodiesResolveContext.getMethods().entrySet())
		{
			NapileNamedFunction function = entry.getKey();
			SimpleMethodDescriptor functionDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(function))
				continue;

			checkModalityModifiers(function);
			checkMethod(function, functionDescriptor);
			checkDeclaredTypeInPublicMember(function, functionDescriptor);
		}

		for(Map.Entry<NapileConstructor, ConstructorDescriptor> entry : bodiesResolveContext.getConstructors().entrySet())
		{
			NapileConstructor constructor = entry.getKey();
			//ConstructorDescriptor constructorDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(constructor))
				continue;

			checkIllegalInThisContextModifiers(constructor, INVALID_MODIFIERS_FOR_CONSTRUCTOR);
			checkModalityModifiers(constructor);
		}

		for(Map.Entry<NapileProperty, PropertyDescriptor> entry : bodiesResolveContext.getProperties().entrySet())
		{
			NapileProperty property = entry.getKey();
			PropertyDescriptor propertyDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(property))
				continue;

			checkModalityModifiers(property);
			checkDeclaredTypeInPublicMember(property, propertyDescriptor);
			checkAccessors(property, propertyDescriptor);
		}
	}

	protected void checkMethod(NapileNamedFunction method, SimpleMethodDescriptor methodDescriptor)
	{
		DeclarationDescriptor containingDescriptor = methodDescriptor.getContainingDeclaration();
		ASTNode abstractModifier = method.getModifierNode(JetTokens.ABSTRACT_KEYWORD);
		ASTNode nativeModifier = method.getModifierNode(JetTokens.NATIVE_KEYWORD);

		checkDeclaredTypeInPublicMember(method, methodDescriptor);

		ClassDescriptor classDescriptor = (ClassDescriptor) containingDescriptor;
		boolean inEnum = classDescriptor.getKind() == ClassKind.ENUM_CLASS;
		boolean inAbstractClass = classDescriptor.getModality() == Modality.ABSTRACT;
		if(abstractModifier != null && !inAbstractClass && !inEnum)
			trace.report(Errors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.on(method, methodDescriptor.getName().getName(), classDescriptor));

		if(method.getBodyExpression() != null && (abstractModifier != null || nativeModifier != null))
			trace.report(Errors.NATIVE_OR_ABSTRACT_METHOD_WITH_BODY.on(abstractModifier == null ? nativeModifier.getPsi() : abstractModifier.getPsi(), methodDescriptor));

		if(method.getBodyExpression() == null && abstractModifier == null && nativeModifier == null)
			trace.report(Errors.NON_ABSTRACT_OR_NATIVE_METHOD_WITH_NO_BODY.on(method, methodDescriptor));
	}

	private void checkDeclaredTypeInPublicMember(NapileNamedDeclaration member, CallableMemberDescriptor memberDescriptor)
	{
		boolean hasDeferredType;
		if(member instanceof NapileProperty)
			hasDeferredType = ((NapileProperty) member).getPropertyTypeRef() == null && DescriptorResolver.hasBody((NapileProperty) member);
		else
		{
			assert member instanceof NapileMethod;
			NapileMethod function = (NapileMethod) member;
			hasDeferredType = function.getReturnTypeRef() == null && function.getBodyExpression() != null && !function.hasBlockBody();
		}

		if(memberDescriptor.getVisibility().isPublicAPI() && memberDescriptor.getOverriddenDescriptors().size() == 0 && hasDeferredType)
			trace.report(Errors.PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE.on(member));
	}

	@SuppressWarnings("unchecked")
	private void checkModalityModifiers(@NotNull NapileDeclaration declaration)
	{
		checkCompatibility(declaration, Lists.newArrayList(JetTokens.ABSTRACT_KEYWORD, JetTokens.FINAL_KEYWORD), Lists.<NapileToken>newArrayList(JetTokens.ABSTRACT_KEYWORD));

		checkCompatibility(declaration, Lists.newArrayList(JetTokens.ABSTRACT_KEYWORD, JetTokens.NATIVE_KEYWORD));
	}

	private void checkCompatibility(@NotNull NapileDeclaration declaration, Collection<NapileKeywordToken> availableModifiers, Collection<NapileToken>... availableCombinations)
	{
		Collection<NapileKeywordToken> presentModifiers = Sets.newLinkedHashSet();
		for(NapileKeywordToken modifier : availableModifiers)
			if(declaration.hasModifier(modifier))
				presentModifiers.add(modifier);

		if(presentModifiers.size() == 1)
			return;

		for(Collection<NapileToken> combination : availableCombinations)
			if(presentModifiers.containsAll(combination) && combination.containsAll(presentModifiers))
				return;

		for(NapileKeywordToken token : presentModifiers)
			trace.report(Errors.INCOMPATIBLE_MODIFIERS.on(declaration.getModifierNode(token).getPsi(), presentModifiers));
	}

	private void checkRedundant(@NotNull NapileDeclaration declaration, Collection<NapileKeywordToken> availableModifiers)
	{
		Collection<NapileKeywordToken> presentModifiers = Sets.newLinkedHashSet();
		for(NapileKeywordToken modifier : availableModifiers)
			if(declaration.hasModifier(modifier))
				presentModifiers.add(modifier);

		for(NapileKeywordToken token : presentModifiers)
			trace.report(Errors.REDUNDANT_MODIFIER.on(declaration.getModifierNode(token).getPsi()));
	}

	public void checkAccessors(NapileProperty property, PropertyDescriptor propertyDescriptor)
	{
		for(NapilePropertyAccessor accessor : property.getAccessors())
			checkIllegalInThisContextModifiers(accessor, Sets.newHashSet(JetTokens.ABSTRACT_KEYWORD, JetTokens.FINAL_KEYWORD, JetTokens.OVERRIDE_KEYWORD));

		NapilePropertyAccessor getter = property.getGetter();
		PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getGetter();
		NapileModifierList getterModifierList = getter != null ? getter.getModifierList() : null;
		if(getterModifierList != null && getterDescriptor != null)
		{
			Map<NapileKeywordToken, ASTNode> nodes = getNodesCorrespondingToModifiers(getterModifierList, Sets.newHashSet(JetTokens.COVERED_KEYWORD, JetTokens.LOCAL_KEYWORD));
			if(getterDescriptor.getVisibility() != propertyDescriptor.getVisibility())
				for(ASTNode node : nodes.values())
					trace.report(Errors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY.on(node.getPsi()));
			else
				for(ASTNode node : nodes.values())
					trace.report(Errors.REDUNDANT_MODIFIER_IN_GETTER.on(node.getPsi()));
		}
	}

	public void checkIllegalInThisContextModifiers(@NotNull NapileDeclaration declaration, Collection<NapileKeywordToken> illegalModifiers)
	{
		for(NapileKeywordToken modifier : illegalModifiers)
			if(declaration.hasModifier(modifier))
				trace.report(Errors.ILLEGAL_MODIFIER.on(declaration.getModifierNode(modifier).getPsi(), modifier));
	}

	@NotNull
	public static Map<NapileKeywordToken, ASTNode> getNodesCorrespondingToModifiers(@NotNull NapileModifierList modifierList, Collection<NapileKeywordToken> possibleModifiers)
	{
		Map<NapileKeywordToken, ASTNode> nodes = new HashMap<NapileKeywordToken, ASTNode>(possibleModifiers.size());
		for(NapileKeywordToken modifier : possibleModifiers)
			if(modifierList.hasModifier(modifier))
				nodes.put(modifier, modifierList.getModifierNode(modifier));
		return nodes;
	}
}
