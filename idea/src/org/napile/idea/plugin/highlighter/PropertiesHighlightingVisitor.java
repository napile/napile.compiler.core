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

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileEnumEntry;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.psi.NapileRetellEntry;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileThisExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lexer.NapileTokens;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;

class PropertiesHighlightingVisitor extends AfterAnalysisHighlightingVisitor
{
	PropertiesHighlightingVisitor(AnnotationHolder holder, BindingContext bindingContext)
	{
		super(holder, bindingContext);
	}

	@Override
	public void visitSimpleNameExpression(NapileSimpleNameExpression expression)
	{
		if(expression.getParent() instanceof NapileThisExpression)
		{
			return;
		}
		DeclarationDescriptor target = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
		if(!(target instanceof VariableDescriptor))
		{
			return;
		}

		highlightProperty(expression, (VariableDescriptor) target, false);
		if(expression.getReferencedNameElementType() == NapileTokens.FIELD_IDENTIFIER)
			JetPsiChecker.highlightName(holder, expression, JetHighlightingColors.BACKING_FIELD_ACCESS);
	}

	@Override
	public void visitProperty(@NotNull NapileProperty property)
	{
		PsiElement nameIdentifier = property.getNameIdentifier();
		if(nameIdentifier == null)
			return;
		VariableDescriptor propertyDescriptor = bindingContext.get(BindingContext.VARIABLE, property);
		if(propertyDescriptor instanceof PropertyDescriptor)
		{
			Boolean backingFieldRequired = bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) propertyDescriptor);
			highlightProperty(nameIdentifier, propertyDescriptor, Boolean.TRUE.equals(backingFieldRequired));
		}

		super.visitProperty(property);
	}

	@Override
	public void visitEnumEntry(@NotNull NapileEnumEntry enumEntry)
	{
		PsiElement nameIdentifier = enumEntry.getNameIdentifier();
		if(nameIdentifier == null)
			return;

		VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, enumEntry);
		if(variableDescriptor != null)
			highlightProperty(nameIdentifier, variableDescriptor, Boolean.FALSE);

		super.visitEnumEntry(enumEntry);
	}

	@Override
	public void visitRetellEntry(@NotNull NapileRetellEntry retellEntry)
	{
		PsiElement nameIdentifier = retellEntry.getNameIdentifier();
		if(nameIdentifier == null)
			return;

		VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, retellEntry);
		if(variableDescriptor != null)
			highlightProperty(nameIdentifier, variableDescriptor, Boolean.FALSE);

		super.visitRetellEntry(retellEntry);
	}

	private void highlightProperty(@NotNull PsiElement elementToHighlight, @NotNull VariableDescriptor descriptor, boolean withBackingField)
	{
		JetPsiChecker.highlightName(holder, elementToHighlight, JetHighlightingColors.getAttributes(descriptor));

		if(withBackingField)
			holder.createInfoAnnotation(elementToHighlight, "This property has a backing field").setTextAttributes(JetHighlightingColors.PROPERTY_WITH_BACKING_FIELD);
	}
}
