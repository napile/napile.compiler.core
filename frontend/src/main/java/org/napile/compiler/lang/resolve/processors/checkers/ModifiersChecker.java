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

package org.napile.compiler.lang.resolve.processors.checkers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.Modality;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.descriptors.Visibility;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.lexer.NapileKeywordToken;
import org.napile.compiler.lang.lexer.NapileToken;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileConstructor;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileMethod;
import org.napile.compiler.lang.psi.NapileNamedDeclaration;
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;

/**
 * @author VISTALL
 * @since 19:52/28.08.12
 */
public class ModifiersChecker
{
	// not create new list every checks - it good for compiler, but as plugin create leaks
	@NotNull
	private static final List<NapileKeywordToken> VISIBILITY_MODIFIERS = Arrays.asList(NapileTokens.LOCAL_KEYWORD, NapileTokens.HERITABLE_KEYWORD, NapileTokens.COVERED_KEYWORD);
	@NotNull
	private static final List<NapileKeywordToken> INVALID_MODIFIERS_FOR_CLASS = Arrays.asList(NapileTokens.OVERRIDE_KEYWORD, NapileTokens.NATIVE_KEYWORD, NapileTokens.LAZY_KEYWORD);
	@NotNull
	private static final List<NapileKeywordToken> INVALID_MODIFIERS_FOR_CONSTRUCTOR = Arrays.asList(NapileTokens.ABSTRACT_KEYWORD, NapileTokens.NATIVE_KEYWORD, NapileTokens.OVERRIDE_KEYWORD, NapileTokens.FINAL_KEYWORD, NapileTokens.LAZY_KEYWORD);
	@NotNull
	private static final List<NapileKeywordToken> INVALID_MODIFIERS_FOR_METHOD = Arrays.asList(NapileTokens.LAZY_KEYWORD);

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
				checkIllegalInThisContextModifiers(clazz, Arrays.asList(NapileTokens.HERITABLE_KEYWORD));
				checkRedundant(clazz, Arrays.asList(NapileTokens.STATIC_KEYWORD));
			}

			checkIllegalInThisContextModifiers(clazz, INVALID_MODIFIERS_FOR_CLASS);
			checkModalityModifiers(clazz);
		}

		for(Map.Entry<NapileNamedMethodOrMacro, SimpleMethodDescriptor> entry : bodiesResolveContext.getMethods().entrySet())
		{
			NapileNamedMethodOrMacro function = entry.getKey();
			SimpleMethodDescriptor functionDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(function))
				continue;

			checkIllegalInThisContextModifiers(function, INVALID_MODIFIERS_FOR_METHOD);
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

		for(Map.Entry<NapileVariable, VariableDescriptor> entry : bodiesResolveContext.getVariables().entrySet())
		{
			NapileVariable property = entry.getKey();
			VariableDescriptor variableDescriptor = entry.getValue();

			if(!bodiesResolveContext.completeAnalysisNeeded(property))
				continue;

			checkModalityModifiers(property);
			checkDeclaredTypeInPublicMember(property, variableDescriptor);
		}
	}

	protected void checkMethod(NapileNamedMethodOrMacro method, SimpleMethodDescriptor methodDescriptor)
	{
		DeclarationDescriptor containingDescriptor = methodDescriptor.getContainingDeclaration();
		ASTNode abstractModifier = method.getModifierNode(NapileTokens.ABSTRACT_KEYWORD);
		ASTNode nativeModifier = method.getModifierNode(NapileTokens.NATIVE_KEYWORD);

		ClassDescriptor classDescriptor = (ClassDescriptor) containingDescriptor;
		boolean inAbstractClass = classDescriptor.getModality() == Modality.ABSTRACT;
		if(abstractModifier != null && !inAbstractClass)
			trace.report(Errors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.on(method, methodDescriptor.getName().getName(), classDescriptor));

		if(method.getBodyExpression() != null && (abstractModifier != null || nativeModifier != null))
			trace.report(Errors.NATIVE_OR_ABSTRACT_METHOD_WITH_BODY.on(abstractModifier == null ? nativeModifier.getPsi() : abstractModifier.getPsi(), methodDescriptor));

		if(method.getBodyExpression() == null && methodDescriptor.getModality() != Modality.ABSTRACT && !methodDescriptor.isNative())
			trace.report(Errors.NON_ABSTRACT_OR_NATIVE_METHOD_WITH_NO_BODY.on(method, methodDescriptor));

		if(methodDescriptor.isMacro() && methodDescriptor.getVisibility() != Visibility.LOCAL)
			trace.report(Errors.MACRO_MUST_BE_DECLARATED_AS_LOCAL.on(method, methodDescriptor));
	}

	private void checkDeclaredTypeInPublicMember(NapileNamedDeclaration member, CallableMemberDescriptor memberDescriptor)
	{
		Visibility visibility = null;

		boolean hasDeferredType;
		if(member instanceof NapileVariable)
		{
			hasDeferredType = ((NapileVariable) member).getType() == null && ((NapileVariable) member).getInitializer() != null;
			visibility = Visibility.resolve(member);
		}
		else
		{
			assert member instanceof NapileMethod;
			NapileMethod function = (NapileMethod) member;
			hasDeferredType = function.getReturnTypeRef() == null && function.getBodyExpression() != null && !function.hasBlockBody();
			visibility = memberDescriptor.getVisibility();
		}

		if(visibility.isPublicAPI() && memberDescriptor.getOverriddenDescriptors().size() == 0 && hasDeferredType)
			trace.report(Errors.PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE.on(member));
	}

	@SuppressWarnings("unchecked")
	private void checkModalityModifiers(@NotNull NapileDeclaration declaration)
	{
		checkCompatibility(declaration, Lists.newArrayList(NapileTokens.ABSTRACT_KEYWORD, NapileTokens.FINAL_KEYWORD), Lists.<NapileToken>newArrayList(NapileTokens.ABSTRACT_KEYWORD));

		checkCompatibility(declaration, Lists.newArrayList(NapileTokens.ABSTRACT_KEYWORD, NapileTokens.NATIVE_KEYWORD));

		checkCompatibility(declaration, VISIBILITY_MODIFIERS);
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
		{
			final ASTNode modifierNode = declaration.getModifierNode(token);
			if(modifierNode == null)
				continue;
			trace.report(Errors.INCOMPATIBLE_MODIFIERS.on(modifierNode.getPsi(), presentModifiers));
		}
	}

	private void checkRedundant(@NotNull NapileDeclaration declaration, Collection<NapileKeywordToken> availableModifiers)
	{
		Collection<NapileKeywordToken> presentModifiers = Sets.newLinkedHashSet();
		for(NapileKeywordToken modifier : availableModifiers)
			if(declaration.hasModifier(modifier))
				presentModifiers.add(modifier);

		for(NapileKeywordToken token : presentModifiers)
		{
			final ASTNode modifierNode = declaration.getModifierNode(token);
			if(modifierNode == null)
				continue;
			trace.report(Errors.REDUNDANT_MODIFIER.on(modifierNode.getPsi()));
		}
	}

	public void checkIllegalInThisContextModifiers(@NotNull NapileDeclaration declaration, Collection<NapileKeywordToken> illegalModifiers)
	{
		for(NapileKeywordToken modifier : illegalModifiers)
			if(declaration.hasModifier(modifier))
			{
				final ASTNode modifierNode = declaration.getModifierNode(modifier);
				if(modifierNode == null)
					continue;
				trace.report(Errors.ILLEGAL_MODIFIER.on(modifierNode.getPsi(), modifier));
			}
	}
}
