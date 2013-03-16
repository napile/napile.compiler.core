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

import static org.napile.compiler.lang.resolve.BindingContext.AUTOCAST;
import static org.napile.compiler.lang.resolve.BindingContext.AUTO_CREATED_IT;
import static org.napile.compiler.lang.resolve.BindingContext.CAPTURED_IN_CLOSURE;
import static org.napile.compiler.lang.resolve.BindingContext.REFERENCE_TARGET;
import static org.napile.compiler.lang.resolve.BindingContext.VARIABLE;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileCallParameterAsVariable;
import org.napile.compiler.lang.psi.NapileEnumValue;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileNamedDeclaration;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.render.DescriptorRenderer;
import org.napile.idea.plugin.editor.highlight.NapileHighlightingColors;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 21:25/26.02.13
 */
public class VariablesHighlightingVisitor extends PostHighlightVisitor
{
	public VariablesHighlightingVisitor(BindingContext context, List<HighlightInfo> holder)
	{
		super(context, holder);
	}

	@Override
	public void visitSimpleNameExpression(@NotNull NapileSimpleNameExpression expression)
	{
		DeclarationDescriptor target = bindingContext.get(REFERENCE_TARGET, expression);
		if(target == null)
			return;

		if(target instanceof VariableDescriptor)
		{
			if(Boolean.TRUE.equals(bindingContext.get(AUTO_CREATED_IT, (VariableDescriptor) target)))
				highlightInfo(expression, "Auto-generated variable", NapileHighlightingColors.AUTO_GENERATED_VAR);

			highlightVariable(expression, (VariableDescriptor) target);
		}

		super.visitSimpleNameExpression(expression);
	}

	@Override
	public void visitVariable(@NotNull NapileVariable property)
	{
		visitVariableDeclaration(property);
		super.visitVariable(property);
	}

	@Override
	public void visitEnumValue(@NotNull NapileEnumValue enumValue)
	{
		visitVariableDeclaration(enumValue);
		super.visitEnumValue(enumValue);
	}

	@Override
	public void visitCallParameterAsVariable(NapileCallParameterAsVariable parameter)
	{
		visitVariableDeclaration(parameter);
		super.visitCallParameterAsVariable(parameter);
	}

	@Override
	public void visitExpression(@NotNull NapileExpression expression)
	{
		JetType autoCast = bindingContext.get(AUTOCAST, expression);
		if(autoCast != null)
		{
			highlightInfo(expression, "Automatically cast to " + DescriptorRenderer.TEXT.renderType(autoCast) ,NapileHighlightingColors.AUTO_CASTED_VALUE);
		}
		super.visitExpression(expression);
	}

	private void visitVariableDeclaration(NapileNamedDeclaration declaration)
	{
		VariableDescriptor declarationDescriptor = bindingContext.get(VARIABLE, declaration);
		PsiElement nameIdentifier = declaration.getNameIdentifier();
		if(nameIdentifier != null && declarationDescriptor != null)
			highlightVariable(nameIdentifier, declarationDescriptor);
	}

	private void highlightVariable(@NotNull PsiElement elementToHighlight, @NotNull VariableDescriptor variableDescriptor)
	{
		if(Boolean.TRUE.equals(bindingContext.get(CAPTURED_IN_CLOSURE, variableDescriptor)))
		{
			String msg = variableDescriptor.isMutable() ? "Wrapped into a reference object to be modified when captured in a closure" : "Value captured in a closure";
			highlightInfo(elementToHighlight, msg, NapileHighlightingColors.WRAPPED_INTO_REF);
		}

		highlightName(elementToHighlight, NapileHighlightingColors.getAttributes(variableDescriptor), variableDescriptor);
	}
}
