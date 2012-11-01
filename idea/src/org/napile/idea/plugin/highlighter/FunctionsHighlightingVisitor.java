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

package org.napile.idea.plugin.highlighter;

import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileNamedMethod;
import org.napile.compiler.lang.psi.NapileTypeReference;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;

/**
 * @author Evgeny Gerashchenko
 * @since 4/2/12
 */
public class FunctionsHighlightingVisitor extends AfterAnalysisHighlightingVisitor
{
	public FunctionsHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext)
	{
		super(holder, bindingContext);
	}

	@Override
	public void visitNamedMethod(NapileNamedMethod function)
	{
		PsiElement nameIdentifier = function.getNameIdentifier();
		if(nameIdentifier != null)
		{
			JetPsiChecker.highlightName(holder, nameIdentifier, JetHighlightingColors.FUNCTION_DECLARATION);
		}

		super.visitNamedMethod(function);
	}

	@Override
	public void visitDelegationToSuperCallSpecifier(NapileDelegationToSuperCall call)
	{
		NapileConstructorCalleeExpression calleeExpression = call.getCalleeExpression();
		NapileTypeReference typeRef = calleeExpression.getTypeReference();
		if(typeRef != null)
		{
			NapileTypeElement typeElement = typeRef.getTypeElement();
			if(typeElement instanceof NapileUserType)
			{
				NapileSimpleNameExpression nameExpression = ((NapileUserType) typeElement).getReferenceExpression();
				if(nameExpression != null)
				{
					JetPsiChecker.highlightName(holder, nameExpression, JetHighlightingColors.CONSTRUCTOR_CALL);
				}
			}
		}
		super.visitDelegationToSuperCallSpecifier(call);
	}

	@Override
	public void visitCallExpression(NapileCallExpression expression)
	{
		NapileExpression callee = expression.getCalleeExpression();
		if(callee instanceof NapileReferenceExpression)
		{
			DeclarationDescriptor calleeDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, (NapileReferenceExpression) callee);
			if(calleeDescriptor != null)
			{
				if(calleeDescriptor instanceof ConstructorDescriptor)
				{
					JetPsiChecker.highlightName(holder, callee, JetHighlightingColors.CONSTRUCTOR_CALL);
				}
				else if(calleeDescriptor instanceof MethodDescriptor)
				{
					MethodDescriptor fun = (MethodDescriptor) calleeDescriptor;
					JetPsiChecker.highlightName(holder, callee, JetHighlightingColors.METHOD_CALL);
					if(fun.isStatic())
						JetPsiChecker.highlightName(holder, callee, JetHighlightingColors.STATIC_METHOD_CALL);
				}
			}
		}

		super.visitCallExpression(expression);
	}
}
