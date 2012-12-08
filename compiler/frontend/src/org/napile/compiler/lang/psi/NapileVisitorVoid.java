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
		visitNamedDeclaration(constructor);
	}

	public void visitStaticConstructor(NapileStaticConstructor constructor)
	{
		visitNamedDeclaration(constructor);
	}

	public void visitNamedMethod(NapileNamedMethod method)
	{
		visitNamedMethodOrMacro(method);
	}

	public void visitNamedMacro(NapileNamedMacro macro)
	{
		visitNamedMethodOrMacro(macro);
	}

	public void visitNamedMethodOrMacro(NapileNamedMethodOrMacro method)
	{
		visitNamedDeclaration(method);
	}

	public void visitVariable(NapileVariable property)
	{
		visitNamedDeclaration(property);
	}

	public void visitNapileFile(NapileFile file)
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

	public void visitModifierList(NapileModifierList list)
	{
		visitJetElement(list);
	}

	public void visitAnnotation(NapileAnnotation annotationEntry)
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

	public void visitCallParameterList(NapileCallParameterList list)
	{
		visitJetElement(list);
	}

	public void visitCallParameterAsVariable(NapileCallParameterAsVariable parameter)
	{
		visitNamedDeclaration(parameter);
	}

	public void visitDelegationSpecifierList(NapileDelegationSpecifierList list)
	{
		visitJetElement(list);
	}

	public void visitDelegationToSuperCallSpecifier(NapileDelegationToSuperCall call)
	{
		visitJetElement(call);
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
		visitExpression(expression);
	}

	public void visitThrowExpression(NapileThrowExpression expression)
	{
		visitExpression(expression);
	}

	public void visitBreakExpression(NapileBreakExpression expression)
	{
		visitExpression(expression);
	}

	public void visitContinueExpression(NapileContinueExpression expression)
	{
		visitExpression(expression);
	}

	public void visitIfExpression(NapileIfExpression expression)
	{
		visitExpression(expression);
	}

	public void visitLabelExpression(NapileLabelExpression expression)
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

	public void visitAnonymMethodExpression(NapileAnonymMethodExpression expression)
	{
		visitExpression(expression);
	}

	public void visitCallExpression(NapileCallExpression expression)
	{
		visitExpression(expression);
	}

	public void visitArrayAccessExpression(NapileArrayAccessExpressionImpl expression)
	{
		visitReferenceExpression(expression);
	}

	public void visitQualifiedExpression(NapileQualifiedExpression expression)
	{
		visitExpression(expression);
	}

	public void visitLinkMethodExpression(NapileLinkMethodExpression expression)
	{
		visitExpression(expression);
	}

	public void visitDotQualifiedExpression(NapileDotQualifiedExpression expression)
	{
		visitQualifiedExpression(expression);
	}

	public void visitSafeQualifiedExpression(NapileSafeQualifiedExpression expression)
	{
		visitQualifiedExpression(expression);
	}

	public void visitAnonymClassExpression(NapileAnonymClassExpression expression)
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
		visitExpression(expression);
	}

	public void visitSuperExpression(NapileSuperExpression expression)
	{
		visitExpression(expression);
	}

	public void visitParenthesizedExpression(NapileParenthesizedExpression expression)
	{
		visitExpression(expression);
	}

	private void visitTypeElement(NapileTypeElement type)
	{
		visitJetElement(type);
	}

	public void visitUserType(NapileUserType type)
	{
		visitTypeElement(type);
	}

	public void visitFunctionType(NapileMethodType type)
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

	public void visitAnonymClass(NapileAnonymClass declaration)
	{
		visitDeclaration(declaration);
	}

	public void visitCallParameterAsReference(NapileCallParameterAsReference napileReferenceParameter)
	{
		visitJetElement(napileReferenceParameter);
	}

	public void visitClassOfExpression(NapileClassOfExpression classOfExpression)
	{
		visitExpression(classOfExpression);
	}

	public void visitTypeOfExpression(NapileTypeOfExpression typeOfExpression)
	{
		visitExpression(typeOfExpression);
	}

	public void visitInjectionExpression(NapileInjectionExpression napileCodeInjection)
	{
		visitExpression(napileCodeInjection);
	}

	public void visitArrayOfExpression(NapileArrayOfExpression arrayExpression)
	{
		visitExpression(arrayExpression);
	}
}
