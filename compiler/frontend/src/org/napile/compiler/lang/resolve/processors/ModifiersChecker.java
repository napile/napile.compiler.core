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

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.NamespaceDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.PropertyGetterDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.NapileNamedFunction;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.psi.NapilePropertyAccessor;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.lexer.JetTokens;
import org.napile.compiler.lexer.NapileKeywordToken;
import org.napile.compiler.lexer.NapileToken;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Pair;

/**
 * @author VISTALL
 * @date 19:52/28.08.12
 */
public class ModifiersChecker
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

			checkModifiers(aClass.getModifierList(), classDescriptor);
		}

		for(Map.Entry<NapileNamedFunction, SimpleMethodDescriptor> entry : bodiesResolveContext.getMethods().entrySet())
		{
			NapileNamedFunction function = entry.getKey();
			SimpleMethodDescriptor functionDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(function))
				continue;

			checkModifiers(function.getModifierList(), functionDescriptor);
		}

		for(Map.Entry<NapileConstructor, ConstructorDescriptor> entry : bodiesResolveContext.getConstructors().entrySet())
		{
			NapileConstructor constructor = entry.getKey();
			ConstructorDescriptor constructorDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(constructor))
				continue;

			checkModifiers(constructor.getModifierList(), constructorDescriptor);
		}

		for(Map.Entry<NapileProperty, PropertyDescriptor> entry : bodiesResolveContext.getProperties().entrySet())
		{
			NapileProperty property = entry.getKey();
			PropertyDescriptor propertyDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(property))
				continue;

			checkModifiers(property.getModifierList(), propertyDescriptor);
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
				trace.report(Errors.PACKAGE_MEMBER_CANNOT_BE_HERITABLE.on(modifierList.getModifierNode(JetTokens.HERITABLE_KEYWORD).getPsi()));
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

	public void checkAccessors(NapileProperty property, PropertyDescriptor propertyDescriptor)
	{
		for(NapilePropertyAccessor accessor : property.getAccessors())
			checkIllegalInThisContextModifiers(accessor.getModifierList(), Sets.newHashSet(JetTokens.ABSTRACT_KEYWORD, JetTokens.FINAL_KEYWORD, JetTokens.OVERRIDE_KEYWORD));

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

	public void checkIllegalInThisContextModifiers(@Nullable NapileModifierList modifierList, Collection<NapileKeywordToken> illegalModifiers)
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

}
