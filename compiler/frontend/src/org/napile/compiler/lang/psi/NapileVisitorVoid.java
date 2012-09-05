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
 * @author max
 */
public class NapileVisitorVoid extends PsiElementVisitor
{
	public void visitJetElement(NapileElement element)
	{
		visitElement(element);
	}

	public void visitDeclaration(NapileDeclaration dcl)
	{
		visitExpression(dcl);
	}

	public void visitClass(NapileClass klass)
	{
		visitNamedDeclaration(klass);
	}

	public void visitConstructor(NapileConstructor constructor)
	{
		visitDeclaration(constructor);
	}

	public void visitNamedMethod(NapileNamedFunction function)
	{
		visitNamedDeclaration(function);
	}

	public void visitProperty(NapileProperty property)
	{
		visitNamedDeclaration(property);
	}

	public void visitJetFile(NapileFile file)
	{
		visitFile(file);
	}

	public void visitImportDirective(NapileImportDirective importDirective)
	{
		visitJetElement(importDirective);
	}

	public void visitClassBody(NapileClassBody classBody)
	{
		visitJetElement(classBody);
	}

	public void visitNamespaceBody(NapileNamespaceBody body)
	{
		visitJetElement(body);
	}

	public void visitModifierList(NapileModifierList list)
	{
		visitJetElement(list);
	}

	public void visitAnnotationList(NapileAnnotationList annotationList)
	{
		visitJetElement(annotationList);
	}

	public void visitAnnotationEntry(NapileAnnotationEntry annotationEntry)
	{
		visitJetElement(annotationEntry);
	}

	public void visitTypeParameterList(NapileTypeParameterList list)
	{
		visitJetElement(list);
	}

	public void visitTypeParameter(NapileTypeParameter parameter)
	{
		visitNamedDeclaration(parameter);
	}

	public void visitEnumEntry(NapileEnumEntry enumEntry)
	{
		visitJetElement(enumEntry);
	}

	public void visitRetellEntry(NapileRetellEntry retellEntry)
	{
		visitJetElement(retellEntry);
	}

	public void visitParameterList(NapileParameterList list)
	{
		visitJetElement(list);
	}

	public void visitParameter(NapileParameter parameter)
	{
		visitNamedDeclaration(parameter);
	}

	public void visitDelegationSpecifierList(NapileDelegationSpecifierList list)
	{
		visitJetElement(list);
	}

	public void visitDelegationSpecifier(NapileDelegationSpecifier specifier)
	{
		visitJetElement(specifier);
	}

	public void visitDelegationToSuperCallSpecifier(NapileDelegatorToSuperCall call)
	{
		visitDelegationSpecifier(call);
	}

	public void visitDelegationToSuperClassSpecifier(NapileDelegatorToSuperClass specifier)
	{
		visitDelegationSpecifier(specifier);
	}

	public void visitDelegationToThisCall(NapileDelegatorToThisCall thisCall)
	{
		visitDelegationSpecifier(thisCall);
	}

	public void visitTypeReference(NapileTypeReference typeReference)
	{
		visitJetElement(typeReference);
	}

	public void visitValueArgumentList(NapileValueArgumentList list)
	{
		visitJetElement(list);
	}

	public void visitArgument(NapileValueArgument argument)
	{
		visitJetElement(argument);
	}

	public void visitExpression(NapileExpression expression)
	{
		visitJetElement(expression);
	}

	public void visitLoopExpression(NapileLoopExpression loopExpression)
	{
		visitExpression(loopExpression);
	}

	public void visitConstantExpression(NapileConstantExpression expression)
	{
		visitExpression(expression);
	}

	public void visitSimpleNameExpression(NapileSimpleNameExpression expression)
	{
		visitReferenceExpression(expression);
	}

	public void visitReferenceExpression(NapileReferenceExpression expression)
	{
		visitExpression(expression);
	}

	public void visitPrefixExpression(NapilePrefixExpression expression)
	{
		visitUnaryExpression(expression);
	}

	public void visitPostfixExpression(NapilePostfixExpression expression)
	{
		visitUnaryExpression(expression);
	}

	public void visitUnaryExpression(NapileUnaryExpression expression)
	{
		visitExpression(expression);
	}

	public void visitBinaryExpression(NapileBinaryExpression expression)
	{
		visitExpression(expression);
	}

	//    public void visitNewExpression(JetNewExpression expression) {
	//        visitExpression(expression);
	//    }
	//
	public void visitReturnExpression(NapileReturnExpression expression)
	{
		visitLabelQualifiedExpression(expression);
	}

	public void visitLabelQualifiedExpression(NapileLabelQualifiedExpression expression)
	{
		visitExpression(expression);
	}

	public void visitThrowExpression(NapileThrowExpression expression)
	{
		visitExpression(expression);
	}

	public void visitBreakExpression(NapileBreakExpression expression)
	{
		visitLabelQualifiedExpression(expression);
	}

	public void visitContinueExpression(NapileContinueExpression expression)
	{
		visitLabelQualifiedExpression(expression);
	}

	public void visitIfExpression(NapileIfExpression expression)
	{
		visitExpression(expression);
	}

	public void visitWhenExpression(NapileWhenExpression expression)
	{
		visitExpression(expression);
	}

	public void visitTryExpression(NapileTryExpression expression)
	{
		visitExpression(expression);
	}

	public void visitForExpression(NapileForExpression expression)
	{
		visitLoopExpression(expression);
	}

	public void visitWhileExpression(NapileWhileExpression expression)
	{
		visitLoopExpression(expression);
	}

	public void visitDoWhileExpression(NapileDoWhileExpression expression)
	{
		visitLoopExpression(expression);
	}

	public void visitFunctionLiteralExpression(NapileFunctionLiteralExpression expression)
	{
		visitExpression(expression);
	}

	public void visitAnnotatedExpression(NapileAnnotatedExpression expression)
	{
		visitExpression(expression);
	}

	public void visitCallExpression(NapileCallExpression expression)
	{
		visitExpression(expression);
	}

	public void visitArrayAccessExpression(NapileArrayAccessExpression expression)
	{
		visitReferenceExpression(expression);
	}

	public void visitQualifiedExpression(NapileQualifiedExpression expression)
	{
		visitExpression(expression);
	}

	public void visitHashQualifiedExpression(NapileHashQualifiedExpression expression)
	{
		visitQualifiedExpression(expression);
	}

	public void visitDotQualifiedExpression(NapileDotQualifiedExpression expression)
	{
		visitQualifiedExpression(expression);
	}

	public void visitSafeQualifiedExpression(NapileSafeQualifiedExpression expression)
	{
		visitQualifiedExpression(expression);
	}

	public void visitObjectLiteralExpression(NapileObjectLiteralExpression expression)
	{
		visitExpression(expression);
	}

	public void visitRootNamespaceExpression(NapileRootNamespaceExpression expression)
	{
		visitExpression(expression);
	}

	public void visitBlockExpression(NapileBlockExpression expression)
	{
		visitExpression(expression);
	}

	public void visitIdeTemplate(NapileIdeTemplate expression)
	{
	}

	public void visitCatchSection(NapileCatchClause catchClause)
	{
		visitJetElement(catchClause);
	}

	public void visitFinallySection(NapileFinallySection finallySection)
	{
		visitJetElement(finallySection);
	}

	public void visitTypeArgumentList(NapileTypeArgumentList typeArgumentList)
	{
		visitJetElement(typeArgumentList);
	}

	public void visitThisExpression(NapileThisExpression expression)
	{
		visitLabelQualifiedExpression(expression);
	}

	public void visitSuperExpression(NapileSuperExpression expression)
	{
		visitLabelQualifiedExpression(expression);
	}

	public void visitParenthesizedExpression(NapileParenthesizedExpression expression)
	{
		visitExpression(expression);
	}

	public void visitInitializerList(NapileInitializerList list)
	{
		visitJetElement(list);
	}

	public void visitAnonymousInitializer(NapileClassInitializer initializer)
	{
		visitDeclaration(initializer);
	}

	public void visitPropertyAccessor(NapilePropertyAccessor accessor)
	{
		visitDeclaration(accessor);
	}

	public void visitTypeConstraintList(NapileTypeConstraintList list)
	{
		visitJetElement(list);
	}

	public void visitTypeConstraint(NapileTypeConstraint constraint)
	{
		visitJetElement(constraint);
	}

	private void visitTypeElement(NapileTypeElement type)
	{
		visitJetElement(type);
	}

	public void visitUserType(NapileUserType type)
	{
		visitTypeElement(type);
	}

	public void visitFunctionType(NapileFunctionType type)
	{
		visitTypeElement(type);
	}

	public void visitSelfType(NapileSelfType type)
	{
		visitTypeElement(type);
	}

	public void visitBinaryWithTypeRHSExpression(NapileBinaryExpressionWithTypeRHS expression)
	{
		visitExpression(expression);
	}

	public void visitStringTemplateExpression(NapileStringTemplateExpression expression)
	{
		visitExpression(expression);
	}

	public void visitNamedDeclaration(NapileNamedDeclaration declaration)
	{
		visitDeclaration(declaration);
	}

	public void visitNullableType(NapileNullableType nullableType)
	{
		visitTypeElement(nullableType);
	}

	public void visitWhenEntry(NapileWhenEntry jetWhenEntry)
	{
		visitJetElement(jetWhenEntry);
	}

	public void visitIsExpression(NapileIsExpression expression)
	{
		visitExpression(expression);
	}

	public void visitWhenConditionIsPattern(NapileWhenConditionIsPattern condition)
	{
		visitJetElement(condition);
	}

	public void visitWhenConditionInRange(NapileWhenConditionInRange condition)
	{
		visitJetElement(condition);
	}

	public void visitWhenConditionWithExpression(NapileWhenConditionWithExpression condition)
	{
		visitJetElement(condition);
	}

	public void visitTypePattern(NapileTypePattern pattern)
	{
		visitPattern(pattern);
	}

	public void visitExpressionPattern(NapileExpressionPattern pattern)
	{
		visitPattern(pattern);
	}

	private void visitPattern(NapilePattern pattern)
	{
		visitJetElement(pattern);
	}

	public void visitAnonymClass(NapileAnonymClass declaration)
	{
		visitNamedDeclaration(declaration);
	}

	public void visitObjectDeclarationName(NapileObjectDeclarationName declaration)
	{
		visitNamedDeclaration(declaration);
	}

	public void visitBindingPattern(NapileBindingPattern pattern)
	{
		visitPattern(pattern);
	}

	public void visitStringTemplateEntry(NapileStringTemplateEntry entry)
	{
		visitJetElement(entry);
	}

	public void visitStringTemplateEntryWithExpression(NapileStringTemplateEntryWithExpression entry)
	{
		visitStringTemplateEntry(entry);
	}

	public void visitBlockStringTemplateEntry(NapileBlockStringTemplateEntry entry)
	{
		visitStringTemplateEntryWithExpression(entry);
	}

	public void visitSimpleNameStringTemplateEntry(NapileSimpleNameStringTemplateEntry entry)
	{
		visitStringTemplateEntryWithExpression(entry);
	}

	public void visitLiteralStringTemplateEntry(NapileLiteralStringTemplateEntry entry)
	{
		visitStringTemplateEntry(entry);
	}

	public void visitEscapeStringTemplateEntry(NapileEscapeStringTemplateEntry entry)
	{
		visitStringTemplateEntry(entry);
	}
}
