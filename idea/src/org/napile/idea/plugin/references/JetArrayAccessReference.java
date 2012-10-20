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

package org.napile.idea.plugin.references;

import static org.napile.compiler.lang.resolve.BindingContext.INDEXED_LVALUE_GET;
import static org.napile.compiler.lang.resolve.BindingContext.INDEXED_LVALUE_SET;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.psi.NapileArrayAccessExpressionImpl;
import org.napile.compiler.lang.psi.NapileContainerNode;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.idea.plugin.project.WholeProjectAnalyzerFacade;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.MultiRangeReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;

/**
 * @author yole
 */
class JetArrayAccessReference extends JetPsiReference implements MultiRangeReference
{
	private NapileArrayAccessExpressionImpl expression;

	public static PsiReference[] create(NapileArrayAccessExpressionImpl expression)
	{
		NapileContainerNode indicesNode = expression.getIndicesNode();
		return indicesNode == null ? PsiReference.EMPTY_ARRAY : new PsiReference[]{new JetArrayAccessReference(expression)};
	}

	public JetArrayAccessReference(@NotNull NapileArrayAccessExpressionImpl expression)
	{
		super(expression);
		this.expression = expression;
	}

	@Override
	public TextRange getRangeInElement()
	{
		return getElement().getTextRange().shiftRight(-getElement().getTextOffset());
	}

	@Override
	protected PsiElement doResolve()
	{
		BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((NapileFile) getElement().getContainingFile()).getBindingContext();
		ResolvedCall<MethodDescriptor> getFunction = bindingContext.get(INDEXED_LVALUE_GET, expression);
		ResolvedCall<MethodDescriptor> setFunction = bindingContext.get(INDEXED_LVALUE_SET, expression);
		if(getFunction != null && setFunction != null)
		{
			return null; // Call doMultiResolve
		}
		return super.doResolve();
	}

	@Override
	protected ResolveResult[] doMultiResolve()
	{
		BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((NapileFile) getElement().getContainingFile()).getBindingContext();
		ResolvedCall<MethodDescriptor> getFunction = bindingContext.get(INDEXED_LVALUE_GET, expression);
		ResolvedCall<MethodDescriptor> setFunction = bindingContext.get(INDEXED_LVALUE_SET, expression);
		if(getFunction == null || setFunction == null)
		{
			return new ResolveResult[0];
		}
		PsiElement getFunctionElement = BindingContextUtils.callableDescriptorToDeclaration(bindingContext, getFunction.getResultingDescriptor());
		assert getFunctionElement != null;
		PsiElement setFunctionElement = BindingContextUtils.callableDescriptorToDeclaration(bindingContext, setFunction.getResultingDescriptor());
		assert setFunctionElement != null;
		return new ResolveResult[]{
				new PsiElementResolveResult(getFunctionElement, true),
				new PsiElementResolveResult(setFunctionElement, true)
		};
	}

	@Override
	public List<TextRange> getRanges()
	{
		List<TextRange> list = new ArrayList<TextRange>();

		NapileContainerNode indices = expression.getIndicesNode();
		TextRange textRange = indices.getNode().findChildByType(NapileTokens.LBRACKET).getTextRange();
		TextRange lBracketRange = textRange.shiftRight(-expression.getTextOffset());

		list.add(lBracketRange);

		ASTNode rBracket = indices.getNode().findChildByType(NapileTokens.RBRACKET);
		if(rBracket != null)
		{
			textRange = rBracket.getTextRange();
			TextRange rBracketRange = textRange.shiftRight(-expression.getTextOffset());
			list.add(rBracketRange);
		}

		return list;
	}
}
