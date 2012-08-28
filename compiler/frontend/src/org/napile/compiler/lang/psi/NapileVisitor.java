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

package org.napile.compiler.lang.psi;

import com.intellij.psi.PsiElementVisitor;

/**
 * @author svtk
 */
public class NapileVisitor<R, D> extends PsiElementVisitor
{
	public R visitJetElement(NapileElement element, D data)
	{
		visitElement(element);
		return null;
	}

	public R visitDeclaration(NapileDeclaration dcl, D data)
	{
		return visitExpression(dcl, data);
	}

	public R visitClass(NapileClass klass, D data)
	{
		return visitNamedDeclaration(klass, data);
	}

	public R visitConstructor(NapileConstructor constructor, D data)
	{
		return visitDeclaration(constructor, data);
	}

	public R visitNamedFunction(NapileNamedFunction function, D data)
	{
		return visitNamedDeclaration(function, data);
	}

	public R visitProperty(NapileProperty property, D data)
	{
		return visitNamedDeclaration(property, data);
	}

	public R visitTypedef(NapileTypedef typedef, D data)
	{
		return visitNamedDeclaration(typedef, data);
	}

	public R visitJetFile(NapileFile file, D data)
	{
		visitFile(file);
		return null;
	}

	public R visitImportDirective(NapileImportDirective importDirective, D data)
	{
		return visitJetElement(importDirective, data);
	}

	public R visitClassBody(NapileClassBody classBody, D data)
	{
		return visitJetElement(classBody, data);
	}

	public R visitNamespaceBody(NapileNamespaceBody body, D data)
	{
		return visitJetElement(body, data);
	}

	public R visitModifierList(NapileModifierList list, D data)
	{
		return visitJetElement(list, data);
	}

	public R visitAnnotationList(NapileAnnotationList annotationList, D data)
	{
		return visitJetElement(annotationList, data);
	}

	public R visitAnnotationEntry(NapileAnnotationEntry annotationEntry, D data)
	{
		return visitJetElement(annotationEntry, data);
	}

	public R visitTypeParameterList(NapileTypeParameterList list, D data)
	{
		return visitJetElement(list, data);
	}

	public R visitTypeParameter(NapileTypeParameter parameter, D data)
	{
		return visitNamedDeclaration(parameter, data);
	}

	public R visitEnumEntry(NapileEnumEntry enumEntry, D data)
	{
		return visitJetElement(enumEntry, data);
	}

	public R visitParameterList(NapileParameterList list, D data)
	{
		return visitJetElement(list, data);
	}

	public R visitParameter(NapileParameter parameter, D data)
	{
		return visitNamedDeclaration(parameter, data);
	}

	public R visitDelegationSpecifierList(NapileDelegationSpecifierList list, D data)
	{
		return visitJetElement(list, data);
	}

	public R visitDelegationSpecifier(NapileDelegationSpecifier specifier, D data)
	{
		return visitJetElement(specifier, data);
	}

	public R visitDelegationToSuperCallSpecifier(NapileDelegatorToSuperCall call, D data)
	{
		return visitDelegationSpecifier(call, data);
	}

	public R visitDelegationToSuperClassSpecifier(NapileDelegatorToSuperClass specifier, D data)
	{
		return visitDelegationSpecifier(specifier, data);
	}

	public R visitDelegationToThisCall(NapileDelegatorToThisCall thisCall, D data)
	{
		return visitDelegationSpecifier(thisCall, data);
	}

	public R visitTypeReference(NapileTypeReference typeReference, D data)
	{
		return visitJetElement(typeReference, data);
	}

	public R visitValueArgumentList(NapileValueArgumentList list, D data)
	{
		return visitJetElement(list, data);
	}

	public R visitArgument(NapileValueArgument argument, D data)
	{
		return visitJetElement(argument, data);
	}

	public R visitExpression(NapileExpression expression, D data)
	{
		return visitJetElement(expression, data);
	}

	public R visitLoopExpression(NapileLoopExpression loopExpression, D data)
	{
		return visitExpression(loopExpression, data);
	}

	public R visitConstantExpression(NapileConstantExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitSimpleNameExpression(NapileSimpleNameExpression expression, D data)
	{
		return visitReferenceExpression(expression, data);
	}

	public R visitReferenceExpression(NapileReferenceExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitTupleExpression(NapileTupleExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitPrefixExpression(NapilePrefixExpression expression, D data)
	{
		return visitUnaryExpression(expression, data);
	}

	public R visitPostfixExpression(NapilePostfixExpression expression, D data)
	{
		return visitUnaryExpression(expression, data);
	}

	public R visitUnaryExpression(NapileUnaryExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitBinaryExpression(NapileBinaryExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	//    public R visitNewExpression(JetNewExpression expression, D data) {
	//        return visitExpression(expression, data);
	//    }
	//
	public R visitReturnExpression(NapileReturnExpression expression, D data)
	{
		return visitLabelQualifiedExpression(expression, data);
	}

	public R visitLabelQualifiedExpression(NapileLabelQualifiedExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitThrowExpression(NapileThrowExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitBreakExpression(NapileBreakExpression expression, D data)
	{
		return visitLabelQualifiedExpression(expression, data);
	}

	public R visitContinueExpression(NapileContinueExpression expression, D data)
	{
		return visitLabelQualifiedExpression(expression, data);
	}

	public R visitIfExpression(NapileIfExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitWhenExpression(NapileWhenExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitTryExpression(NapileTryExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitForExpression(NapileForExpression expression, D data)
	{
		return visitLoopExpression(expression, data);
	}

	public R visitWhileExpression(NapileWhileExpression expression, D data)
	{
		return visitLoopExpression(expression, data);
	}

	public R visitDoWhileExpression(NapileDoWhileExpression expression, D data)
	{
		return visitLoopExpression(expression, data);
	}

	public R visitFunctionLiteralExpression(NapileFunctionLiteralExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitAnnotatedExpression(NapileAnnotatedExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitCallExpression(NapileCallExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitArrayAccessExpression(NapileArrayAccessExpression expression, D data)
	{
		return visitReferenceExpression(expression, data);
	}

	public R visitQualifiedExpression(NapileQualifiedExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitHashQualifiedExpression(NapileHashQualifiedExpression expression, D data)
	{
		return visitQualifiedExpression(expression, data);
	}

	public R visitDotQualifiedExpression(NapileDotQualifiedExpression expression, D data)
	{
		return visitQualifiedExpression(expression, data);
	}

	public R visitSafeQualifiedExpression(NapileSafeQualifiedExpression expression, D data)
	{
		return visitQualifiedExpression(expression, data);
	}

	public R visitObjectLiteralExpression(NapileObjectLiteralExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitRootNamespaceExpression(NapileRootNamespaceExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitBlockExpression(NapileBlockExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitIdeTemplate(NapileIdeTemplate expression, D data)
	{
		return null;
	}

	public R visitCatchSection(NapileCatchClause catchClause, D data)
	{
		return visitJetElement(catchClause, data);
	}

	public R visitFinallySection(NapileFinallySection finallySection, D data)
	{
		return visitJetElement(finallySection, data);
	}

	public R visitTypeArgumentList(NapileTypeArgumentList typeArgumentList, D data)
	{
		return visitJetElement(typeArgumentList, data);
	}

	public R visitThisExpression(NapileThisExpression expression, D data)
	{
		return visitLabelQualifiedExpression(expression, data);
	}

	public R visitSuperExpression(NapileSuperExpression expression, D data)
	{
		return visitLabelQualifiedExpression(expression, data);
	}

	public R visitParenthesizedExpression(NapileParenthesizedExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitInitializerList(NapileInitializerList list, D data)
	{
		return visitJetElement(list, data);
	}

	public R visitAnonymousInitializer(NapileClassInitializer initializer, D data)
	{
		return visitDeclaration(initializer, data);
	}

	public R visitPropertyAccessor(NapilePropertyAccessor accessor, D data)
	{
		return visitDeclaration(accessor, data);
	}

	public R visitTypeConstraintList(NapileTypeConstraintList list, D data)
	{
		return visitJetElement(list, data);
	}

	public R visitTypeConstraint(NapileTypeConstraint constraint, D data)
	{
		return visitJetElement(constraint, data);
	}

	private R visitTypeElement(NapileTypeElement type, D data)
	{
		return visitJetElement(type, data);
	}

	public R visitUserType(NapileUserType type, D data)
	{
		return visitTypeElement(type, data);
	}

	public R visitTupleType(NapileTupleType type, D data)
	{
		return visitTypeElement(type, data);
	}

	public R visitFunctionType(NapileFunctionType type, D data)
	{
		return visitTypeElement(type, data);
	}

	public R visitSelfType(NapileSelfType type, D data)
	{
		return visitTypeElement(type, data);
	}

	public R visitBinaryWithTypeRHSExpression(NapileBinaryExpressionWithTypeRHS expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitStringTemplateExpression(NapileStringTemplateExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitNamedDeclaration(NapileNamedDeclaration declaration, D data)
	{
		return visitDeclaration(declaration, data);
	}

	public R visitNullableType(NapileNullableType nullableType, D data)
	{
		return visitTypeElement(nullableType, data);
	}

	public R visitWhenEntry(NapileWhenEntry jetWhenEntry, D data)
	{
		return visitJetElement(jetWhenEntry, data);
	}

	public R visitIsExpression(NapileIsExpression expression, D data)
	{
		return visitExpression(expression, data);
	}

	public R visitWhenConditionIsPattern(NapileWhenConditionIsPattern condition, D data)
	{
		return visitJetElement(condition, data);
	}

	public R visitWhenConditionInRange(NapileWhenConditionInRange condition, D data)
	{
		return visitJetElement(condition, data);
	}

	public R visitWhenConditionExpression(NapileWhenConditionWithExpression condition, D data)
	{
		return visitJetElement(condition, data);
	}

	public R visitTypePattern(NapileTypePattern pattern, D data)
	{
		return visitPattern(pattern, data);
	}

	public R visitWildcardPattern(NapileWildcardPattern pattern, D data)
	{
		return visitPattern(pattern, data);
	}

	public R visitExpressionPattern(NapileExpressionPattern pattern, D data)
	{
		return visitPattern(pattern, data);
	}

	public R visitTuplePattern(NapileTuplePattern pattern, D data)
	{
		return visitPattern(pattern, data);
	}

	private R visitPattern(NapilePattern pattern, D data)
	{
		return visitJetElement(pattern, data);
	}

	public R visitDecomposerPattern(NapileDecomposerPattern pattern, D data)
	{
		return visitPattern(pattern, data);
	}

	public R visitObjectDeclaration(NapileObjectDeclaration declaration, D data)
	{
		return visitNamedDeclaration(declaration, data);
	}

	public R visitObjectDeclarationName(NapileObjectDeclarationName declarationName, D data)
	{
		return visitNamedDeclaration(declarationName, data);
	}

	public R visitBindingPattern(NapileBindingPattern pattern, D data)
	{
		return visitPattern(pattern, data);
	}

	public R visitStringTemplateEntry(NapileStringTemplateEntry entry, D data)
	{
		return visitJetElement(entry, data);
	}

	public R visitStringTemplateEntryWithExpression(NapileStringTemplateEntryWithExpression entry, D data)
	{
		return visitStringTemplateEntry(entry, data);
	}

	public R visitBlockStringTemplateEntry(NapileBlockStringTemplateEntry entry, D data)
	{
		return visitStringTemplateEntryWithExpression(entry, data);
	}

	public R visitSimpleNameStringTemplateEntry(NapileSimpleNameStringTemplateEntry entry, D data)
	{
		return visitStringTemplateEntryWithExpression(entry, data);
	}

	public R visitLiteralStringTemplateEntry(NapileLiteralStringTemplateEntry entry, D data)
	{
		return visitStringTemplateEntry(entry, data);
	}

	public R visitEscapeStringTemplateEntry(NapileEscapeStringTemplateEntry entry, D data)
	{
		return visitStringTemplateEntry(entry, data);
	}
}
