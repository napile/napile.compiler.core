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

package org.napile.compiler.lang.diagnostics;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.lexer.NapileKeywordToken;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.resolve.calls.inference.InferenceErrorData;
import org.napile.compiler.lang.types.NapileType;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.impl.source.tree.LeafPsiElement;

/**
 * For error messages, see DefaultErrorMessages and IdeErrorMessages.
 *
 * @author abreslav
 */
public interface Errors
{
	DiagnosticFactory1<NapileFile, Throwable> EXCEPTION_WHILE_ANALYZING = DiagnosticFactory1.create(Severity.ERROR);

	DiagnosticFactory1<NapileReferenceExpression, String> UNRESOLVED_REFERENCE = DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.ELEMENT_RANGE_OR_BRACKETS);

	//Elements with "INVISIBLE_REFERENCE" error are marked as unresolved, unlike elements with "INVISIBLE_MEMBER" error
	DiagnosticFactory2<NapileSimpleNameExpression, DeclarationDescriptor, DeclarationDescriptor> INVISIBLE_REFERENCE = DiagnosticFactory2.create(Severity.ERROR);
	DiagnosticFactory2<PsiElement, DeclarationDescriptor, DeclarationDescriptor> INVISIBLE_MEMBER = DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.CALL_ELEMENT);
	DiagnosticFactory2<PsiElement, DeclarationDescriptor, DeclarationDescriptor> IMMUTABLE_INVISIBLE_MEMBER = DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.CALL_ELEMENT);

	DiagnosticFactory1<PsiElement, String> REDECLARATION = DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.POSITION_REDECLARATION);
	DiagnosticFactory1<PsiElement, String> NAME_SHADOWING = DiagnosticFactory1.create(Severity.WARNING, PositioningStrategies.POSITION_REDECLARATION);

	DiagnosticFactory2<PsiElement, NapileType, NapileType> TYPE_MISMATCH = DiagnosticFactory2.create(Severity.ERROR);
	DiagnosticFactory1<PsiElement, Collection<NapileKeywordToken>> INCOMPATIBLE_MODIFIERS = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<PsiElement, NapileKeywordToken> ILLEGAL_MODIFIER = DiagnosticFactory1.create(Severity.ERROR);

	DiagnosticFactory2<PsiElement, NapileKeywordToken, NapileKeywordToken> REDUNDANT_MODIFIER_WITH_MODIFIER = DiagnosticFactory2.create(Severity.WARNING);

	DiagnosticFactory0<PsiElement> REDUNDANT_MODIFIER_IN_GETTER = DiagnosticFactory0.create(Severity.WARNING);
	DiagnosticFactory0<PsiElement> REDUNDANT_MODIFIER = DiagnosticFactory0.create(Severity.WARNING);
	DiagnosticFactory1<PsiElement, TextRange> VALID_STRING_ESCAPE = DiagnosticFactory1.create(Severity.INFO, PositioningStrategies.TEXT_RANGE1);
	DiagnosticFactory2<PsiElement, TextRange, String> INVALID_STRING_ESCAPE = DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.TEXT_RANGE2);

	DiagnosticFactory0<PsiElement> NAPILE_LANG_CLASS_IMPORT_EXPECTED = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<PsiElement> NAPILE_LANG_TYPE_IMPORT_EXPECTED = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileInjectionExpression> UNKNOWN_INJECTION = DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.NAME_IDENTIFIER);

	DiagnosticFactory0<NapileExpression> TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileReturnExpression> RETURN_NOT_ALLOWED = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileSimpleNameExpression> LABEL_NAME_CLASH = DiagnosticFactory0.create(Severity.WARNING);
	DiagnosticFactory0<NapileSimpleNameExpression> EXPRESSION_EXPECTED_NAMESPACE_FOUND = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory1<NapileSimpleNameExpression, DeclarationDescriptor> CANNOT_IMPORT_FROM_ELEMENT = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<NapileSimpleNameExpression, DeclarationDescriptor> CANNOT_BE_IMPORTED = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory0<NapileExpression> USELESS_HIDDEN_IMPORT = DiagnosticFactory0.create(Severity.WARNING);
	DiagnosticFactory0<NapileExpression> USELESS_SIMPLE_IMPORT = DiagnosticFactory0.create(Severity.WARNING);
	DiagnosticFactory1<NapileReferenceExpression, String> TARGET_IS_DEPRECATED = DiagnosticFactory1.create(Severity.WARNING);

	DiagnosticFactory0<NapileCallParameterAsVariable> CANNOT_INFER_PARAMETER_TYPE = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory0<NapileElement> NO_BACKING_FIELD_ABSTRACT_PROPERTY = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileElement> NO_BACKING_FIELD_CUSTOM_ACCESSORS = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileElement> INACCESSIBLE_BACKING_FIELD = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileElement> NOT_PROPERTY_BACKING_FIELD = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory0<PsiElement> MIXING_NAMED_AND_POSITIONED_ARGUMENTS = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileReferenceExpression> ARGUMENT_PASSED_TWICE = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory1<NapileReferenceExpression, String> NAMED_PARAMETER_NOT_FOUND = DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.ELEMENT_RANGE_OR_BRACKETS);
	DiagnosticFactory1<NapileReferenceExpression, String> EXPECTED_METHOD_NOT_FOUND = DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.ELEMENT_RANGE_OR_BRACKETS);
	DiagnosticFactory0<NapileExpression> VARARG_OUTSIDE_PARENTHESES = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<LeafPsiElement> NON_VARARG_SPREAD = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory0<NapileExpression> MANY_FUNCTION_LITERAL_ARGUMENTS = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileVariable> PROPERTY_WITH_NO_TYPE_NO_INITIALIZER = DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.NAMED_ELEMENT);

	DiagnosticFactory0<PsiElement> INSTANCE_CALL_FROM_STATIC_CONTEXT = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<PsiElement> STATIC_CALL_EXPECT_INSTANCE_CALL = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory0<NapileVariable> MUST_BE_INITIALIZED = DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.NAMED_ELEMENT);
	DiagnosticFactory0<NapileVariable> MUST_BE_INITIALIZED_OR_BE_ABSTRACT = DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.NAMED_ELEMENT);

	DiagnosticFactory0<NapileExpression> PROPERTY_INITIALIZER_NO_BACKING_FIELD = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory2<NapileModifierListOwner, String, ClassDescriptor> ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS = DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.ABSTRACT_MODIFIER);
	DiagnosticFactory2<NapileMethod, String, ClassDescriptor> ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS = DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.ABSTRACT_MODIFIER);
	DiagnosticFactory1<PsiElement, SimpleMethodDescriptor> NATIVE_OR_ABSTRACT_METHOD_WITH_BODY = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<NapileMethod, SimpleMethodDescriptor> NON_ABSTRACT_OR_NATIVE_METHOD_WITH_NO_BODY = DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.NAMED_ELEMENT);
	DiagnosticFactory1<NapileMethod, SimpleMethodDescriptor> MACRO_MUST_BE_DECLARATED_AS_LOCAL = DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.NAMED_ELEMENT);
	DiagnosticFactory1<NapileModifierListOwner, SimpleMethodDescriptor> NON_MEMBER_ABSTRACT_FUNCTION = DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.ABSTRACT_MODIFIER);

	DiagnosticFactory1<NapileMethod, SimpleMethodDescriptor> NON_MEMBER_FUNCTION_NO_BODY = DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.NAMED_ELEMENT);

	// properties
	DiagnosticFactory1<NapileVariable, VariableDescriptor> VARIABLE_WITH_INITIALIZER_AND_LAZY_PROPERTY = DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.NAMED_ELEMENT);
	DiagnosticFactory1<NapileMethod, MethodDescriptor> GET_PROPERTY_WITH_LAZY_PROPERTY = DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.NAMED_ELEMENT);
	DiagnosticFactory1<NapileMethod, MethodDescriptor> SET_PROPERTY_WITH_FINAL_VARIABLE = DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.NAMED_ELEMENT);

	DiagnosticFactory0<NapileNamedDeclaration> PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE = DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.NAMED_ELEMENT);

	DiagnosticFactory0<NapileTypeReference> MANY_CALLS_TO_THIS = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory1<NapileModifierListOwner, CallableMemberDescriptor> NOTHING_TO_OVERRIDE = DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.OVERRIDE_MODIFIER);
	DiagnosticFactory3<PsiNameIdentifierOwner, CallableMemberDescriptor, CallableMemberDescriptor, DeclarationDescriptor> VIRTUAL_MEMBER_HIDDEN = DiagnosticFactory3.create(Severity.ERROR, PositioningStrategies.NAMED_ELEMENT);
	DiagnosticFactory3<NapileModifierListOwner, CallableMemberDescriptor, CallableDescriptor, DeclarationDescriptor> CANNOT_OVERRIDE_INVISIBLE_MEMBER = DiagnosticFactory3.create(Severity.ERROR, PositioningStrategies.OVERRIDE_MODIFIER);
	DiagnosticFactory0<NapileDeclaration> CANNOT_INFER_VISIBILITY = DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.DECLARATION);

	DiagnosticFactory1<NapileSimpleNameExpression, VariableDescriptor> UNINITIALIZED_VARIABLE = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<NapileSimpleNameExpression, CallParameterDescriptor> UNINITIALIZED_PARAMETER = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<NapileVariable, VariableDescriptor> UNUSED_VARIABLE = DiagnosticFactory1.create(Severity.WARNING, PositioningStrategies.NAME_IDENTIFIER);
	DiagnosticFactory1<NapileCallParameterAsVariable, VariableDescriptor> UNUSED_PARAMETER = DiagnosticFactory1.create(Severity.WARNING, PositioningStrategies.NAME_IDENTIFIER);
	DiagnosticFactory1<NapileNamedDeclaration, DeclarationDescriptor> ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE = DiagnosticFactory1.create(Severity.WARNING, PositioningStrategies.NAME_IDENTIFIER);
	DiagnosticFactory1<NapileExpression, DeclarationDescriptor> VARIABLE_WITH_REDUNDANT_INITIALIZER = DiagnosticFactory1.create(Severity.WARNING);
	DiagnosticFactory2<NapileElement, NapileElement, DeclarationDescriptor> UNUSED_VALUE = DiagnosticFactory2.create(Severity.WARNING);
	DiagnosticFactory1<NapileElement, NapileElement> UNUSED_CHANGED_VALUE = DiagnosticFactory1.create(Severity.WARNING);
	DiagnosticFactory0<NapileElement> UNUSED_EXPRESSION = DiagnosticFactory0.create(Severity.WARNING);
	DiagnosticFactory0<NapileAnonymMethodExpression> UNUSED_FUNCTION_LITERAL = DiagnosticFactory0.create(Severity.WARNING);

	DiagnosticFactory1<NapileExpression, DeclarationDescriptor> VAL_REASSIGNMENT = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<NapileExpression, DeclarationDescriptor> INITIALIZATION_BEFORE_DECLARATION = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory0<NapileExpression> VARIABLE_EXPECTED = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory1<NapileExpression, DeclarationDescriptor> INITIALIZATION_USING_BACKING_FIELD_CUSTOM_SETTER = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<NapileExpression, DeclarationDescriptor> INITIALIZATION_USING_BACKING_FIELD_OPEN_SETTER = DiagnosticFactory1.create(Severity.ERROR);

	DiagnosticFactory1<NapileSimpleNameExpression, DeclarationDescriptor> FUNCTION_PARAMETERS_OF_INLINE_FUNCTION = DiagnosticFactory1.create(Severity.ERROR);

	DiagnosticFactory0<NapileElement> UNREACHABLE_CODE = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory0<PsiElement> NO_CONSTRUCTOR = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileExpression> NOT_A_CLASS = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory1<PsiElement, NapileType> MISSED_SUPER_CALL = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory0<NapileTypeReference> INVALID_SUPER_CALL = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory0<NapileTypeReference> LOCAL_EXTENSION_PROPERTY = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory0<NapileArrayAccessExpressionImpl> NO_GET_METHOD = DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.ARRAY_ACCESS);
	DiagnosticFactory0<NapileArrayAccessExpressionImpl> NO_SET_METHOD = DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.ARRAY_ACCESS);

	DiagnosticFactory0<NapileSimpleNameExpression> EQUALS_MISSING = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileBinaryExpression> ASSIGNMENT_IN_EXPRESSION_CONTEXT = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory1<NapileSuperExpression, String> SUPER_IS_NOT_AN_EXPRESSION = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory0<NapileDeclaration> DECLARATION_IN_ILLEGAL_CONTEXT = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileExpression> SETTER_PARAMETER_WITH_DEFAULT_VALUE = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileThisExpression> NO_THIS = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileSuperExpression> SUPER_NOT_AVAILABLE = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileSuperExpression> AMBIGUOUS_SUPER = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileExpression> ABSTRACT_SUPER_CALL = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileTypeReference> NOT_A_SUPERTYPE = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<PsiElement> TYPE_ARGUMENTS_REDUNDANT_IN_SUPER_QUALIFIER = DiagnosticFactory0.create(Severity.WARNING);
	DiagnosticFactory0<NapileSimpleNameExpression> USELESS_CAST_STATIC_ASSERT_IS_FINE = DiagnosticFactory0.create(Severity.WARNING);
	DiagnosticFactory0<NapileSimpleNameExpression> USELESS_CAST = DiagnosticFactory0.create(Severity.WARNING);
	DiagnosticFactory0<NapileSimpleNameExpression> CAST_NEVER_SUCCEEDS = DiagnosticFactory0.create(Severity.WARNING);
	DiagnosticFactory2<NapileTypeReference, NapileType, NapileType> WRONG_SETTER_PARAMETER_TYPE = DiagnosticFactory2.create(Severity.ERROR);
	DiagnosticFactory2<NapileTypeReference, NapileType, NapileType> WRONG_GETTER_RETURN_TYPE = DiagnosticFactory2.create(Severity.ERROR);
	DiagnosticFactory0<PsiElement> NO_GENERICS_IN_SUPERTYPE_SPECIFIER = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory0<NapileExpression> ITERATOR_MISSING = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<? extends CallableDescriptor>>> ITERATOR_AMBIGUITY = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<PsiElement, Collection<? extends MethodDescriptor>> AMBIGUOUS_LINK_METHOD = DiagnosticFactory1.create(Severity.ERROR);

	DiagnosticFactory1<NapileSimpleNameExpression, NapileType> COMPARE_TO_TYPE_MISMATCH = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<NapileExpression, NapileType> CALLEE_NOT_A_FUNCTION = DiagnosticFactory1.create(Severity.ERROR);

	DiagnosticFactory0<NapileReturnExpression> RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileDeclarationWithBody> NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY = DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.DECLARATION_WITH_BODY);
	DiagnosticFactory1<NapileExpression, NapileType> RETURN_TYPE_MISMATCH = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<NapileExpression, NapileType> EXPECTED_TYPE_MISMATCH = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<NapileBinaryExpression, NapileType> ASSIGNMENT_TYPE_MISMATCH = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<NapileExpression, NapileType> IMPLICIT_CAST_TO_UNIT_OR_ANY = DiagnosticFactory1.create(Severity.WARNING);
	DiagnosticFactory1<NapileExpression, NapileExpression> EXPRESSION_EXPECTED = DiagnosticFactory1.create(Severity.ERROR);

	DiagnosticFactory2<NapileTypeReference, NapileType, NapileType> UPPER_BOUND_VIOLATED = DiagnosticFactory2.create(Severity.ERROR);
	DiagnosticFactory0<NapileTypeReference> CONSTRUCTORS_EXPECTED = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory1<NapileTypeReference, NapileType> FINAL_UPPER_BOUND = DiagnosticFactory1.create(Severity.WARNING);
	DiagnosticFactory1<NapileExpression, NapileType> USELESS_ELVIS = DiagnosticFactory1.create(Severity.WARNING);
	DiagnosticFactory1<PsiElement, TypeParameterDescriptor> CONFLICTING_UPPER_BOUNDS = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<PsiElement, TypeParameterDescriptor> CONFLICTING_CLASS_OBJECT_UPPER_BOUNDS = DiagnosticFactory1.create(Severity.ERROR);

	DiagnosticFactory1<PsiElement, CallableDescriptor> TOO_MANY_ARGUMENTS = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<PsiElement, String> ERROR_COMPILE_TIME_VALUE = DiagnosticFactory1.create(Severity.ERROR);

	DiagnosticFactory0<NapileWhenEntry> ELSE_MISPLACED_IN_WHEN = DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.ELSE_ENTRY);

	DiagnosticFactory0<NapileWhenExpression> NO_ELSE_IN_WHEN = new DiagnosticFactory0<NapileWhenExpression>(Severity.ERROR, PositioningStrategies.WHEN_EXPRESSION);
	DiagnosticFactory0<NapileWhenConditionInRange> TYPE_MISMATCH_IN_RANGE = new DiagnosticFactory0<NapileWhenConditionInRange>(Severity.ERROR, PositioningStrategies.WHEN_CONDITION_IN_RANGE);
	DiagnosticFactory0<PsiElement> CYCLIC_INHERITANCE_HIERARCHY = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory0<NapileTypeReference> SUPERTYPE_APPEARS_TWICE = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileTypeReference> FINAL_SUPERTYPE = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileTypeReference> TRAITED_CLASS_CANT_EXTEND_CLASS_WITH_CONSTRUCTORS = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileTypeReference> FROM_TRAITED_CLASS_CANT_CALL_CONSTRUCTOR = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory1<NapileExpression, String> ILLEGAL_SELECTOR = DiagnosticFactory1.create(Severity.ERROR);

	DiagnosticFactory0<NapileCallParameterAsVariable> VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileExpression> BREAK_OR_CONTINUE_OUTSIDE_A_LOOP = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory1<NapileExpression, String> NOT_A_LOOP_LABEL = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<NapileReturnExpression, String> NOT_A_RETURN_LABEL = DiagnosticFactory1.create(Severity.ERROR);

	DiagnosticFactory0<NapileNullableType> NULLABLE_SUPERTYPE = DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.NULLABLE_TYPE);
	DiagnosticFactory1<PsiElement, NapileType> UNSAFE_CALL = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory0<NapileSimpleNameExpression> AMBIGUOUS_LABEL = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory1<PsiElement, String> UNSUPPORTED = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<PsiElement, NapileType> UNNECESSARY_SAFE_CALL = DiagnosticFactory1.create(Severity.WARNING);
	DiagnosticFactory1<PsiElement, NapileType> UNNECESSARY_NOT_NULL_ASSERTION = DiagnosticFactory1.create(Severity.WARNING);
	DiagnosticFactory2<NapileExpression, NapileType, String> AUTOCAST_IMPOSSIBLE = DiagnosticFactory2.create(Severity.ERROR);

	DiagnosticFactory2<NapileTypeReference, NapileType, NapileType> TYPE_MISMATCH_IN_FOR_LOOP = DiagnosticFactory2.create(Severity.ERROR);
	DiagnosticFactory1<NapileElement, NapileType> TYPE_MISMATCH_IN_CONDITION = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory2<NapileTypeReference, NapileType, NapileType> TYPE_MISMATCH_IN_BINDING_PATTERN = DiagnosticFactory2.create(Severity.ERROR);
	DiagnosticFactory2<NapileElement, NapileType, NapileType> INCOMPATIBLE_TYPES = DiagnosticFactory2.create(Severity.ERROR);
	DiagnosticFactory0<NapileWhenCondition> EXPECTED_CONDITION = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory1<NapileElement, NapileType> CANNOT_CHECK_FOR_ERASED = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory2<NapileBinaryExpressionWithTypeRHS, NapileType, NapileType> UNCHECKED_CAST = DiagnosticFactory2.create(Severity.WARNING);

	DiagnosticFactory3<NapileElement, TypeParameterDescriptor, ClassDescriptor, Collection<NapileType>> INCONSISTENT_TYPE_PARAMETER_VALUES = DiagnosticFactory3.create(Severity.ERROR);

	DiagnosticFactory3<NapileBinaryExpression, NapileSimpleNameExpression, NapileType, NapileType> EQUALITY_NOT_APPLICABLE = DiagnosticFactory3.create(Severity.ERROR);

	DiagnosticFactory2<NapileBinaryExpression, NapileBinaryExpression, Boolean> SENSELESS_COMPARISON = DiagnosticFactory2.create(Severity.WARNING);
	DiagnosticFactory0<NapileElement> SENSELESS_NULL_IN_WHEN = DiagnosticFactory0.create(Severity.WARNING);

	DiagnosticFactory2<PsiElement, CallableMemberDescriptor, DeclarationDescriptor> OVERRIDING_FINAL_MEMBER = DiagnosticFactory2.create(Severity.ERROR);
	DiagnosticFactory3<NapileModifierListOwner, Visibility, CallableMemberDescriptor, DeclarationDescriptor> CANNOT_WEAKEN_ACCESS_PRIVILEGE = DiagnosticFactory3.create(Severity.ERROR, PositioningStrategies.VISIBILITY_MODIFIER);
	DiagnosticFactory3<NapileModifierListOwner, Visibility, CallableMemberDescriptor, DeclarationDescriptor> CANNOT_CHANGE_ACCESS_PRIVILEGE = DiagnosticFactory3.create(Severity.ERROR, PositioningStrategies.VISIBILITY_MODIFIER);

	DiagnosticFactory2<NapileNamedDeclaration, CallableMemberDescriptor, CallableMemberDescriptor> RETURN_TYPE_MISMATCH_ON_OVERRIDE = DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION_RETURN_TYPE);

	DiagnosticFactory2<NapileVariable, VariableDescriptorImpl, VariableDescriptorImpl> VAR_OVERRIDDEN_BY_VAL = DiagnosticFactory2.create(Severity.ERROR, new PositioningStrategy<NapileVariable>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull NapileVariable property)
		{
			return markNode(property.getVarOrValNode());
		}
	});

	DiagnosticFactory2<PsiElement, NapileClassLike, CallableMemberDescriptor> ABSTRACT_MEMBER_NOT_IMPLEMENTED = DiagnosticFactory2.create(Severity.ERROR);

	DiagnosticFactory2<PsiElement, NapileClassLike, CallableMemberDescriptor> MANY_IMPL_MEMBER_NOT_IMPLEMENTED = DiagnosticFactory2.create(Severity.ERROR);

	DiagnosticFactory0<NapileCallParameterAsVariable> DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE = DiagnosticFactory0.create(Severity.ERROR, PositioningStrategies.PARAMETER_DEFAULT_VALUE);
	DiagnosticFactory1<NapileCallParameterAsVariable, CallParameterDescriptor> MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<NapileClassLike, CallParameterDescriptor> MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE = DiagnosticFactory1.create(Severity.ERROR, PositioningStrategies.NAME_IDENTIFIER);
	DiagnosticFactory2<NapileCallParameterAsVariable, ClassDescriptor, CallParameterDescriptor> PARAMETER_NAME_CHANGED_ON_OVERRIDE = DiagnosticFactory2.create(Severity.WARNING, PositioningStrategies.NAME_IDENTIFIER);
	DiagnosticFactory2<NapileClassLike, Collection<? extends CallableMemberDescriptor>, Integer> DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES = DiagnosticFactory2.create(Severity.WARNING, PositioningStrategies.NAME_IDENTIFIER);

	DiagnosticFactory2<NapileDeclaration, CallableMemberDescriptor, String> CONFLICTING_OVERLOADS = DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.DECLARATION);

	DiagnosticFactory2<NapileReferenceExpression, NapileExpression, NapileType> FUNCTION_EXPECTED = DiagnosticFactory2.create(Severity.ERROR);
	DiagnosticFactory2<NapileExpression, NapileExpression, Boolean> FUNCTION_CALL_EXPECTED = DiagnosticFactory2.create(Severity.ERROR, PositioningStrategies.CALL_EXPRESSION);

	DiagnosticFactory3<NapileExpression, String, NapileType, NapileType> RESULT_TYPE_MISMATCH = DiagnosticFactory3.create(Severity.ERROR);
	DiagnosticFactory3<NapileReferenceExpression, String, String, String> UNSAFE_INFIX_CALL = DiagnosticFactory3.create(Severity.ERROR);

	DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<? extends CallableDescriptor>>> OVERLOAD_RESOLUTION_AMBIGUITY = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<? extends CallableDescriptor>>> NONE_APPLICABLE = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<PsiElement, CallParameterDescriptor> NO_VALUE_FOR_PARAMETER = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory0<NapileReferenceExpression> NO_RECEIVER_ADMITTED = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory1<NapileSimpleNameExpression, ClassifierDescriptor> NO_CLASS_OBJECT = DiagnosticFactory1.create(Severity.ERROR);

	DiagnosticFactory0<PsiElement> CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS = DiagnosticFactory0.create(Severity.ERROR);

	DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory1<PsiElement, InferenceErrorData> TYPE_INFERENCE_UPPER_BOUND_VIOLATED = DiagnosticFactory1.create(Severity.ERROR);
	DiagnosticFactory2<PsiElement, NapileType, NapileType> TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH = DiagnosticFactory2.create(Severity.ERROR);
	Collection<AbstractDiagnosticFactory> TYPE_INFERENCE_ERRORS = Lists.<AbstractDiagnosticFactory>newArrayList(TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS, TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH, TYPE_INFERENCE_UPPER_BOUND_VIOLATED, TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH);

	DiagnosticFactory1<NapileElement, Integer> WRONG_NUMBER_OF_TYPE_ARGUMENTS = DiagnosticFactory1.create(Severity.ERROR);

	DiagnosticFactory0<NapileExpression> DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED = DiagnosticFactory0.create(Severity.WARNING);

	// annotations
	DiagnosticFactory1<NapileAnnotation, String> NOT_AN_ANNOTATION_CLASS = DiagnosticFactory1.create(Severity.ERROR);

	DiagnosticFactory0<NapileAnnotation> DUPLICATE_ANNOTATION = DiagnosticFactory0.create(Severity.ERROR);
	DiagnosticFactory0<NapileAnnotation> NONE_APPLICABLE_ANNOTATION = DiagnosticFactory0.create(Severity.ERROR);


	// This field is needed to make the Initializer class load (interfaces cannot have static initializers)
	@SuppressWarnings("UnusedDeclaration")
	Initializer __initializer = Initializer.INSTANCE;

	class Initializer
	{
		static
		{
			for(Field field : Errors.class.getFields())
			{
				if(Modifier.isStatic(field.getModifiers()))
				{
					try
					{
						Object value = field.get(null);
						if(value instanceof AbstractDiagnosticFactory)
						{
							AbstractDiagnosticFactory factory = (AbstractDiagnosticFactory) value;
							factory.setName(field.getName());
						}
					}
					catch(IllegalAccessException e)
					{
						throw new IllegalStateException(e);
					}
				}
			}
		}

		private static final Initializer INSTANCE = new Initializer();

		private Initializer()
		{
		}
	}
}
