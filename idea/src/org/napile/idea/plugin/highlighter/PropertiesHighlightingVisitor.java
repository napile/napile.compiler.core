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
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileThisExpression;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.resolve.BindingContext;
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

		JetPsiChecker.highlightName(holder, expression, NapileHighlightingColors.getAttributes(target), target);
	}

	@Override
	public void visitVariable(@NotNull NapileVariable property)
	{
		PsiElement nameIdentifier = property.getNameIdentifier();
		if(nameIdentifier == null)
			return;
		VariableDescriptor propertyDescriptor = bindingContext.get(BindingContext.VARIABLE, property);
		if(propertyDescriptor != null)
			JetPsiChecker.highlightName(holder, nameIdentifier, NapileHighlightingColors.getAttributes(propertyDescriptor), propertyDescriptor);

		super.visitVariable(property);
	}
}
