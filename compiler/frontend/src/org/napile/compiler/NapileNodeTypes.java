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

/*
 * @author max
 */
package org.napile.compiler;

import org.napile.compiler.lang.psi.stubs.elements.JetStubElementTypes;
import org.napile.compiler.plugin.JetLanguage;
import org.napile.compiler.lang.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;

public interface NapileNodeTypes
{
	IFileElementType JET_FILE = new IFileElementType(JetLanguage.INSTANCE);

	IElementType CLASS = JetStubElementTypes.CLASS;
	IElementType METHOD = JetStubElementTypes.METHOD;
	IElementType PROPERTY = JetStubElementTypes.PROPERTY;

	NapileNodeType ANONYM_CLASS = new NapileNodeType("ANONYM_CLASS", NapileAnonymClass.class);
	NapileNodeType OBJECT_DECLARATION_NAME = new NapileNodeType("OBJECT_DECLARATION_NAME", NapileObjectDeclarationName.class);

	NapileNodeType CONSTRUCTOR = new NapileNodeType("CONSTRUCTOR", NapileConstructor.class);
	IElementType ENUM_ENTRY = JetStubElementTypes.ENUM_ENTRY;
	NapileNodeType ANONYMOUS_INITIALIZER = new NapileNodeType("ANONYMOUS_INITIALIZER", NapileClassInitializer.class);

	IElementType TYPE_PARAMETER_LIST = JetStubElementTypes.TYPE_PARAMETER_LIST;
	IElementType TYPE_PARAMETER = JetStubElementTypes.TYPE_PARAMETER;

	NapileNodeType DELEGATION_SPECIFIER_LIST = new NapileNodeType("DELEGATION_SPECIFIER_LIST", NapileDelegationSpecifierList.class);

	NapileNodeType DELEGATOR_SUPER_CALL = new NapileNodeType("DELEGATOR_SUPER_CALL", NapileDelegatorToSuperCall.class);
	NapileNodeType DELEGATOR_SUPER_CLASS = new NapileNodeType("DELEGATOR_SUPER_CLASS", NapileDelegatorToSuperClass.class);
	NapileNodeType CONSTRUCTOR_CALLEE = new NapileNodeType("CONSTRUCTOR_CALLEE", NapileConstructorCalleeExpression.class);
	IElementType VALUE_PARAMETER_LIST = JetStubElementTypes.VALUE_PARAMETER_LIST;
	IElementType VALUE_PARAMETER = JetStubElementTypes.VALUE_PARAMETER;

	NapileNodeType CLASS_BODY = new NapileNodeType("CLASS_BODY", NapileClassBody.class);
	NapileNodeType IMPORT_DIRECTIVE = new NapileNodeType("IMPORT_DIRECTIVE", NapileImportDirective.class);
	NapileNodeType MODIFIER_LIST = new NapileNodeType("MODIFIER_LIST", NapileModifierList.class);
	NapileNodeType ANNOTATION_LIST = new NapileNodeType("ANNOTATION_LIST", NapileAnnotationList.class);
	NapileNodeType ANNOTATION_ENTRY = new NapileNodeType("ANNOTATION_ENTRY", NapileAnnotationEntry.class);

	NapileNodeType TYPE_ARGUMENT_LIST = new NapileNodeType("TYPE_ARGUMENT_LIST", NapileTypeArgumentList.class);
	NapileNodeType VALUE_ARGUMENT_LIST = new NapileNodeType("VALUE_ARGUMENT_LIST", NapileValueArgumentList.class);
	NapileNodeType VALUE_ARGUMENT = new NapileNodeType("VALUE_ARGUMENT", NapileValueArgument.class);
	NapileNodeType VALUE_ARGUMENT_NAME = new NapileNodeType("VALUE_ARGUMENT_NAME", NapileValueArgumentName.class);
	NapileNodeType TYPE_REFERENCE = new NapileNodeType("TYPE_REFERENCE", NapileTypeReference.class);

	NapileNodeType USER_TYPE = new NapileNodeType("USER_TYPE", NapileUserType.class);
	NapileNodeType FUNCTION_TYPE = new NapileNodeType("FUNCTION_TYPE", NapileFunctionType.class);
	NapileNodeType SELF_TYPE = new NapileNodeType("SELF_TYPE", NapileSelfType.class);
	NapileNodeType NULLABLE_TYPE = new NapileNodeType("NULLABLE_TYPE", NapileNullableType.class);

	// TODO: review
	NapileNodeType PROPERTY_ACCESSOR = new NapileNodeType("PROPERTY_ACCESSOR", NapilePropertyAccessor.class);
	NapileNodeType INITIALIZER_LIST = new NapileNodeType("INITIALIZER_LIST", NapileInitializerList.class);
	NapileNodeType THIS_CALL = new NapileNodeType("THIS_CALL", NapileDelegatorToThisCall.class);
	NapileNodeType THIS_CONSTRUCTOR_REFERENCE = new NapileNodeType("THIS_CONSTRUCTOR_REFERENCE", NapileThisReferenceExpression.class);
	NapileNodeType TYPE_CONSTRAINT_LIST = new NapileNodeType("TYPE_CONSTRAINT_LIST", NapileTypeConstraintList.class);
	NapileNodeType TYPE_CONSTRAINT = new NapileNodeType("TYPE_CONSTRAINT", NapileTypeConstraint.class);

	// TODO: Not sure if we need separate NT for each kind of constants
	NapileNodeType NULL = new NapileNodeType("NULL", NapileConstantExpression.class);
	NapileNodeType BOOLEAN_CONSTANT = new NapileNodeType("BOOLEAN_CONSTANT", NapileConstantExpression.class);
	NapileNodeType FLOAT_CONSTANT = new NapileNodeType("FLOAT_CONSTANT", NapileConstantExpression.class);
	NapileNodeType CHARACTER_CONSTANT = new NapileNodeType("CHARACTER_CONSTANT", NapileConstantExpression.class);
	NapileNodeType INTEGER_CONSTANT = new NapileNodeType("INTEGER_CONSTANT", NapileConstantExpression.class);

	NapileNodeType STRING_TEMPLATE = new NapileNodeType("STRING_TEMPLATE", NapileStringTemplateExpression.class);
	NapileNodeType LONG_STRING_TEMPLATE_ENTRY = new NapileNodeType("LONG_STRING_TEMPLATE_ENTRY", NapileBlockStringTemplateEntry.class);
	NapileNodeType SHORT_STRING_TEMPLATE_ENTRY = new NapileNodeType("SHORT_STRING_TEMPLATE_ENTRY", NapileSimpleNameStringTemplateEntry.class);
	NapileNodeType LITERAL_STRING_TEMPLATE_ENTRY = new NapileNodeType("LITERAL_STRING_TEMPLATE_ENTRY", NapileLiteralStringTemplateEntry.class);
	NapileNodeType ESCAPE_STRING_TEMPLATE_ENTRY = new NapileNodeType("ESCAPE_STRING_TEMPLATE_ENTRY", NapileEscapeStringTemplateEntry.class);

	NapileNodeType PARENTHESIZED = new NapileNodeType("PARENTHESIZED", NapileParenthesizedExpression.class);
	NapileNodeType RETURN = new NapileNodeType("RETURN", NapileReturnExpression.class);
	NapileNodeType THROW = new NapileNodeType("THROW", NapileThrowExpression.class);
	NapileNodeType CONTINUE = new NapileNodeType("CONTINUE", NapileContinueExpression.class);
	NapileNodeType BREAK = new NapileNodeType("BREAK", NapileBreakExpression.class);
	NapileNodeType IF = new NapileNodeType("IF", NapileIfExpression.class);
	NapileNodeType CONDITION = new NapileNodeType("CONDITION", NapileContainerNode.class);
	NapileNodeType THEN = new NapileNodeType("THEN", NapileContainerNode.class);
	NapileNodeType ELSE = new NapileNodeType("ELSE", NapileContainerNode.class);
	NapileNodeType TRY = new NapileNodeType("TRY", NapileTryExpression.class);
	NapileNodeType CATCH = new NapileNodeType("CATCH", NapileCatchClause.class);
	NapileNodeType FINALLY = new NapileNodeType("FINALLY", NapileFinallySection.class);
	NapileNodeType FOR = new NapileNodeType("FOR", NapileForExpression.class);
	NapileNodeType WHILE = new NapileNodeType("WHILE", NapileWhileExpression.class);
	NapileNodeType DO_WHILE = new NapileNodeType("DO_WHILE", NapileDoWhileExpression.class);
	NapileNodeType LOOP_PARAMETER = new NapileNodeType("LOOP_PARAMETER", NapileParameter.class); // TODO: Do we need separate type?
	NapileNodeType LOOP_RANGE = new NapileNodeType("LOOP_RANGE", NapileContainerNode.class);
	NapileNodeType BODY = new NapileNodeType("BODY", NapileContainerNode.class);
	NapileNodeType BLOCK = new NapileNodeType("BLOCK", NapileBlockExpression.class);
	NapileNodeType FUNCTION_LITERAL_EXPRESSION = new NapileNodeType("FUNCTION_LITERAL_EXPRESSION", NapileFunctionLiteralExpression.class);
	NapileNodeType FUNCTION_LITERAL = new NapileNodeType("FUNCTION_LITERAL", NapileFunctionLiteral.class);
	NapileNodeType ANNOTATED_EXPRESSION = new NapileNodeType("ANNOTATED_EXPRESSION", NapileAnnotatedExpression.class);

	NapileNodeType REFERENCE_EXPRESSION = new NapileNodeType("REFERENCE_EXPRESSION", NapileSimpleNameExpression.class);
	NapileNodeType OPERATION_REFERENCE = new NapileNodeType("OPERATION_REFERENCE", NapileSimpleNameExpression.class);
	NapileNodeType LABEL_REFERENCE = new NapileNodeType("LABEL_REFERENCE", NapileSimpleNameExpression.class);

	NapileNodeType LABEL_QUALIFIER = new NapileNodeType("LABEL_QUALIFIER", NapileContainerNode.class);

	NapileNodeType THIS_EXPRESSION = new NapileNodeType("THIS_EXPRESSION", NapileThisExpression.class);
	NapileNodeType SUPER_EXPRESSION = new NapileNodeType("SUPER_EXPRESSION", NapileSuperExpression.class);
	NapileNodeType BINARY_EXPRESSION = new NapileNodeType("BINARY_EXPRESSION", NapileBinaryExpression.class);
	NapileNodeType BINARY_WITH_TYPE = new NapileNodeType("BINARY_WITH_TYPE", NapileBinaryExpressionWithTypeRHS.class);
	NapileNodeType BINARY_WITH_PATTERN = new NapileNodeType("BINARY_WITH_PATTERN", NapileIsExpression.class); // TODO:
	NapileNodeType PREFIX_EXPRESSION = new NapileNodeType("PREFIX_EXPRESSION", NapilePrefixExpression.class);
	NapileNodeType POSTFIX_EXPRESSION = new NapileNodeType("POSTFIX_EXPRESSION", NapilePostfixExpression.class);
	NapileNodeType CALL_EXPRESSION = new NapileNodeType("CALL_EXPRESSION", NapileCallExpression.class);
	NapileNodeType ARRAY_ACCESS_EXPRESSION = new NapileNodeType("ARRAY_ACCESS_EXPRESSION", NapileArrayAccessExpression.class);
	NapileNodeType INDICES = new NapileNodeType("INDICES", NapileContainerNode.class);
	NapileNodeType DOT_QUALIFIED_EXPRESSION = new NapileNodeType("DOT_QUALIFIED_EXPRESSION", NapileDotQualifiedExpression.class);
	NapileNodeType HASH_QUALIFIED_EXPRESSION = new NapileNodeType("HASH_QUALIFIED_EXPRESSION", NapileHashQualifiedExpression.class);
	NapileNodeType SAFE_ACCESS_EXPRESSION = new NapileNodeType("SAFE_ACCESS_EXPRESSION", NapileSafeQualifiedExpression.class);
	//    NapileNodeType PREDICATE_EXPRESSION      = new NapileNodeType("PREDICATE_EXPRESSION", JetPredicateExpression.class);

	NapileNodeType OBJECT_LITERAL = new NapileNodeType("OBJECT_LITERAL", NapileObjectLiteralExpression.class);
	NapileNodeType ROOT_NAMESPACE = new NapileNodeType("ROOT_NAMESPACE", NapileRootNamespaceExpression.class);

	NapileNodeType EXPRESSION_PATTERN = new NapileNodeType("EXPRESSION_PATTERN", NapileExpressionPattern.class);
	NapileNodeType TYPE_PATTERN = new NapileNodeType("TYPE_PATTERN", NapileTypePattern.class);
	NapileNodeType BINDING_PATTERN = new NapileNodeType("BINDING_PATTERN", NapileBindingPattern.class);

	NapileNodeType WHEN = new NapileNodeType("WHEN", NapileWhenExpression.class);
	NapileNodeType WHEN_ENTRY = new NapileNodeType("WHEN_ENTRY", NapileWhenEntry.class);

	NapileNodeType WHEN_CONDITION_IN_RANGE = new NapileNodeType("WHEN_CONDITION_IN_RANGE", NapileWhenConditionInRange.class);
	NapileNodeType WHEN_CONDITION_IS_PATTERN = new NapileNodeType("WHEN_CONDITION_IS_PATTERN", NapileWhenConditionIsPattern.class);
	NapileNodeType WHEN_CONDITION_EXPRESSION = new NapileNodeType("WHEN_CONDITION_WITH_EXPRESSION", NapileWhenConditionWithExpression.class);

	NapileNodeType NAMESPACE_HEADER = new NapileNodeType("NAMESPACE_HEADER", NapileNamespaceHeader.class);

	NapileNodeType IDE_TEMPLATE_EXPRESSION = new NapileNodeType("IDE_TEMPLATE_EXPRESSION", NapileIdeTemplate.class);
}
