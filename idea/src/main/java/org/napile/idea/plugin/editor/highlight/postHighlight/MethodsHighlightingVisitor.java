/*
 * Copyright 2010-2013 napile.org
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

package org.napile.idea.plugin.editor.highlight.postHighlight;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.idea.plugin.editor.highlight.NapileHighlightingColors;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 21:51/26.02.13
 */
public class MethodsHighlightingVisitor extends PostHighlightVisitor
{
	public MethodsHighlightingVisitor(BindingTrace context, Collection<HighlightInfo> holder)
	{
		super(context, holder);
	}

	@Override
	public void visitNamedMethodOrMacro(NapileNamedMethodOrMacro function)
	{
		PsiElement nameIdentifier = function.getNameIdentifier();
		if(nameIdentifier != null)
		{
			highlightName(nameIdentifier, NapileHighlightingColors.METHOD_DECLARATION, null);
		}

		super.visitNamedMethodOrMacro(function);
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
					highlightName(nameExpression, NapileHighlightingColors.CONSTRUCTOR_CALL, null);
				}
			}
		}
		super.visitDelegationToSuperCallSpecifier(call);
	}

	@Override
	public void visitCallExpression(NapileCallExpression expression)
	{
		highlightMethodName(expression.getCalleeExpression());

		super.visitCallExpression(expression);
	}

	@Override
	public void visitLinkMethodExpression(NapileLinkMethodExpression expression)
	{
		highlightMethodName(expression.getTarget());
		super.visitLinkMethodExpression(expression);
	}

	private void highlightMethodName(@Nullable NapileElement highlightTarget)
	{
		if(!(highlightTarget instanceof NapileReferenceExpression))
		{
			return;
		}
		DeclarationDescriptor calleeDescriptor = bindingTrace.get(BindingTraceKeys.REFERENCE_TARGET, (NapileReferenceExpression) highlightTarget);
		if(calleeDescriptor != null)
		{
			if(calleeDescriptor instanceof ConstructorDescriptor)
			{
				highlightName(highlightTarget, NapileHighlightingColors.CONSTRUCTOR_CALL, calleeDescriptor);
			}
			else if(calleeDescriptor instanceof MethodDescriptor)
			{
				MethodDescriptor fun = (MethodDescriptor) calleeDescriptor;
				highlightName(highlightTarget, NapileHighlightingColors.METHOD_CALL, fun);
				if(fun.isStatic())
					highlightName(highlightTarget, NapileHighlightingColors.STATIC_METHOD_CALL, fun);
				if(fun.isMacro())
					highlightName(highlightTarget, NapileHighlightingColors.MACRO_CALL, fun);
			}
		}
	}
}
