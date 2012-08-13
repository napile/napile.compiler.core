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

package org.jetbrains.jet.plugin.quickfix;

import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS;
import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_FUNCTION_WITH_BODY;
import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_MEMBER_NOT_IMPLEMENTED;
import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS;
import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS;
import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_PROPERTY_NOT_IN_CLASS;
import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_PROPERTY_WITH_GETTER;
import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_PROPERTY_WITH_INITIALIZER;
import static org.jetbrains.jet.lang.diagnostics.Errors.ABSTRACT_PROPERTY_WITH_SETTER;
import static org.jetbrains.jet.lang.diagnostics.Errors.CANNOT_CHANGE_ACCESS_PRIVILEGE;
import static org.jetbrains.jet.lang.diagnostics.Errors.CANNOT_WEAKEN_ACCESS_PRIVILEGE;
import static org.jetbrains.jet.lang.diagnostics.Errors.FUNCTION_CALL_EXPECTED;
import static org.jetbrains.jet.lang.diagnostics.Errors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY;
import static org.jetbrains.jet.lang.diagnostics.Errors.ILLEGAL_MODIFIER;
import static org.jetbrains.jet.lang.diagnostics.Errors.INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER;
import static org.jetbrains.jet.lang.diagnostics.Errors.INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER;
import static org.jetbrains.jet.lang.diagnostics.Errors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED;
import static org.jetbrains.jet.lang.diagnostics.Errors.MUST_BE_INITIALIZED_OR_BE_ABSTRACT;
import static org.jetbrains.jet.lang.diagnostics.Errors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY;
import static org.jetbrains.jet.lang.diagnostics.Errors.NON_MEMBER_ABSTRACT_FUNCTION;
import static org.jetbrains.jet.lang.diagnostics.Errors.NON_MEMBER_FUNCTION_NO_BODY;
import static org.jetbrains.jet.lang.diagnostics.Errors.NOTHING_TO_OVERRIDE;
import static org.jetbrains.jet.lang.diagnostics.Errors.PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE;
import static org.jetbrains.jet.lang.diagnostics.Errors.REDUNDANT_MODIFIER;
import static org.jetbrains.jet.lang.diagnostics.Errors.REDUNDANT_MODIFIER_IN_GETTER;
import static org.jetbrains.jet.lang.diagnostics.Errors.SUPERTYPE_NOT_INITIALIZED_DEFAULT;
import static org.jetbrains.jet.lang.diagnostics.Errors.TRAIT_CAN_NOT_BE_FINAL;
import static org.jetbrains.jet.lang.diagnostics.Errors.UNNECESSARY_NOT_NULL_ASSERTION;
import static org.jetbrains.jet.lang.diagnostics.Errors.UNNECESSARY_SAFE_CALL;
import static org.jetbrains.jet.lang.diagnostics.Errors.UNRESOLVED_REFERENCE;
import static org.jetbrains.jet.lang.diagnostics.Errors.UNSAFE_CALL;
import static org.jetbrains.jet.lang.diagnostics.Errors.USELESS_CAST;
import static org.jetbrains.jet.lang.diagnostics.Errors.USELESS_CAST_STATIC_ASSERT_IS_FINE;
import static org.jetbrains.jet.lang.diagnostics.Errors.USELESS_ELVIS;
import static org.jetbrains.jet.lang.diagnostics.Errors.VAL_REASSIGNMENT;
import static org.jetbrains.jet.lang.diagnostics.Errors.VAL_WITH_SETTER;
import static org.jetbrains.jet.lang.diagnostics.Errors.VIRTUAL_MEMBER_HIDDEN;
import static org.jetbrains.jet.lang.diagnostics.Errors.WRONG_GETTER_RETURN_TYPE;
import static org.jetbrains.jet.lang.diagnostics.Errors.WRONG_SETTER_PARAMETER_TYPE;
import static org.jetbrains.jet.lexer.JetTokens.ABSTRACT_KEYWORD;
import static org.jetbrains.jet.lexer.JetTokens.FINAL_KEYWORD;
import static org.jetbrains.jet.lexer.JetTokens.OVERRIDE_KEYWORD;

import java.util.Collection;

import org.jetbrains.jet.lang.diagnostics.AbstractDiagnosticFactory;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.plugin.codeInsight.ImplementMethodsHandler;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.codeInsight.intention.IntentionAction;

/**
 * @author svtk
 */
public class QuickFixes
{

	private static final Multimap<AbstractDiagnosticFactory, JetIntentionActionFactory> factories = HashMultimap.create();
	private static final Multimap<AbstractDiagnosticFactory, IntentionAction> actions = HashMultimap.create();

	public static Collection<JetIntentionActionFactory> getActionFactories(AbstractDiagnosticFactory diagnosticFactory)
	{
		return factories.get(diagnosticFactory);
	}

	public static Collection<IntentionAction> getActions(AbstractDiagnosticFactory diagnosticFactory)
	{
		return actions.get(diagnosticFactory);
	}

	private QuickFixes()
	{
	}

	static
	{
		JetIntentionActionFactory removeAbstractModifierFactory = RemoveModifierFix.createRemoveModifierFromListOwnerFactory(ABSTRACT_KEYWORD);
		JetIntentionActionFactory addAbstractModifierFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD);

		factories.put(ABSTRACT_PROPERTY_IN_PRIMARY_CONSTRUCTOR_PARAMETERS, removeAbstractModifierFactory);
		factories.put(ABSTRACT_PROPERTY_NOT_IN_CLASS, removeAbstractModifierFactory);

		JetIntentionActionFactory removePartsFromPropertyFactory = RemovePartsFromPropertyFix.createFactory();
		factories.put(ABSTRACT_PROPERTY_WITH_INITIALIZER, removeAbstractModifierFactory);
		factories.put(ABSTRACT_PROPERTY_WITH_INITIALIZER, removePartsFromPropertyFactory);

		factories.put(ABSTRACT_PROPERTY_WITH_GETTER, removeAbstractModifierFactory);
		factories.put(ABSTRACT_PROPERTY_WITH_GETTER, removePartsFromPropertyFactory);

		factories.put(ABSTRACT_PROPERTY_WITH_SETTER, removeAbstractModifierFactory);
		factories.put(ABSTRACT_PROPERTY_WITH_SETTER, removePartsFromPropertyFactory);

		factories.put(MUST_BE_INITIALIZED_OR_BE_ABSTRACT, addAbstractModifierFactory);

		JetIntentionActionFactory addAbstractToClassFactory = AddModifierFix.createFactory(ABSTRACT_KEYWORD, JetClass.class);
		factories.put(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
		factories.put(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

		JetIntentionActionFactory removeFunctionBodyFactory = RemoveFunctionBodyFix.createFactory();
		factories.put(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, removeAbstractModifierFactory);
		factories.put(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS, addAbstractToClassFactory);

		factories.put(ABSTRACT_FUNCTION_WITH_BODY, removeAbstractModifierFactory);
		factories.put(ABSTRACT_FUNCTION_WITH_BODY, removeFunctionBodyFactory);

		JetIntentionActionFactory addFunctionBodyFactory = AddFunctionBodyFix.createFactory();
		factories.put(NON_ABSTRACT_FUNCTION_WITH_NO_BODY, addAbstractModifierFactory);
		factories.put(NON_ABSTRACT_FUNCTION_WITH_NO_BODY, addFunctionBodyFactory);

		factories.put(NON_MEMBER_ABSTRACT_FUNCTION, removeAbstractModifierFactory);
		factories.put(NON_MEMBER_FUNCTION_NO_BODY, addFunctionBodyFactory);

		factories.put(NOTHING_TO_OVERRIDE, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(OVERRIDE_KEYWORD));
		factories.put(VIRTUAL_MEMBER_HIDDEN, AddModifierFix.createFactory(OVERRIDE_KEYWORD));

		factories.put(USELESS_CAST_STATIC_ASSERT_IS_FINE, ReplaceOperationInBinaryExpressionFix.createChangeCastToStaticAssertFactory());
		factories.put(USELESS_CAST, RemoveRightPartOfBinaryExpressionFix.createRemoveCastFactory());

		JetIntentionActionFactory changeAccessorTypeFactory = ChangeAccessorTypeFix.createFactory();
		factories.put(WRONG_SETTER_PARAMETER_TYPE, changeAccessorTypeFactory);
		factories.put(WRONG_GETTER_RETURN_TYPE, changeAccessorTypeFactory);

		factories.put(USELESS_ELVIS, RemoveRightPartOfBinaryExpressionFix.createRemoveElvisOperatorFactory());

		JetIntentionActionFactory removeRedundantModifierFactory = RemoveModifierFix.createRemoveModifierFactory(true);
		factories.put(REDUNDANT_MODIFIER, removeRedundantModifierFactory);
		factories.put(TRAIT_CAN_NOT_BE_FINAL, RemoveModifierFix.createRemoveModifierFromListOwnerFactory(FINAL_KEYWORD));

		JetIntentionActionFactory removeModifierFactory = RemoveModifierFix.createRemoveModifierFactory();
		factories.put(GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY, removeModifierFactory);
		factories.put(REDUNDANT_MODIFIER_IN_GETTER, removeRedundantModifierFactory);
		factories.put(ILLEGAL_MODIFIER, removeModifierFactory);

		JetIntentionActionFactory changeToBackingFieldFactory = ChangeToBackingFieldFix.createFactory();
		factories.put(INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER, changeToBackingFieldFactory);
		factories.put(INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER, changeToBackingFieldFactory);

		JetIntentionActionFactory unresolvedReferenceFactory = ImportClassAndFunFix.createFactory();
		factories.put(UNRESOLVED_REFERENCE, unresolvedReferenceFactory);

		factories.put(SUPERTYPE_NOT_INITIALIZED_DEFAULT, ChangeToConstructorInvocationFix.createFactory());
		factories.put(FUNCTION_CALL_EXPECTED, ChangeToFunctionInvocationFix.createFactory());

		factories.put(CANNOT_CHANGE_ACCESS_PRIVILEGE, ChangeVisibilityModifierFix.createFactory());
		factories.put(CANNOT_WEAKEN_ACCESS_PRIVILEGE, ChangeVisibilityModifierFix.createFactory());

		ImplementMethodsHandler implementMethodsHandler = new ImplementMethodsHandler();
		actions.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, implementMethodsHandler);
		actions.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED, implementMethodsHandler);

		ChangeVariableMutabilityFix changeVariableMutabilityFix = new ChangeVariableMutabilityFix();
		actions.put(VAL_WITH_SETTER, changeVariableMutabilityFix);
		actions.put(VAL_REASSIGNMENT, changeVariableMutabilityFix);

		actions.put(UNNECESSARY_SAFE_CALL, ReplaceCallFix.toDotCallFromSafeCall());
		actions.put(UNSAFE_CALL, ReplaceCallFix.toSafeCall());

		actions.put(UNSAFE_CALL, ExclExclCallFix.introduceExclExclCall());
		actions.put(UNNECESSARY_NOT_NULL_ASSERTION, ExclExclCallFix.removeExclExclCall());

		actions.put(PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE, new SpecifyTypeExplicitlyFix());
	}
}
