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

package org.napile.idea.plugin.quickfix;

import static org.napile.idea.plugin.project.AnalyzeSingleFileUtil.getContextForSingleFile;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.psi.NapilePsiFactory;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.idea.plugin.JetBundle;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;

/**
 * @author svtk
 */
public class ChangeVariableMutabilityFix implements IntentionAction
{
	private boolean isVar;

	public ChangeVariableMutabilityFix(boolean isVar)
	{
		this.isVar = isVar;
	}

	public ChangeVariableMutabilityFix()
	{
		this(false);
	}

	@NotNull
	@Override
	public String getText()
	{
		return isVar ? JetBundle.message("make.variable.immutable") : JetBundle.message("make.variable.mutable");
	}

	@NotNull
	@Override
	public String getFamilyName()
	{
		return JetBundle.message("change.variable.mutability.family");
	}

	@Override
	public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file)
	{
		if(!(file instanceof NapileFile))
			return false;
		NapileProperty property = getCorrespondingProperty(editor, (NapileFile) file);
		return property != null && !property.isVar();
	}

	private static NapileProperty getCorrespondingProperty(Editor editor, NapileFile file)
	{
		final PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
		NapileProperty property = PsiTreeUtil.getParentOfType(elementAtCaret, NapileProperty.class);
		if(property != null)
			return property;
		NapileSimpleNameExpression simpleNameExpression = PsiTreeUtil.getParentOfType(elementAtCaret, NapileSimpleNameExpression.class);
		if(simpleNameExpression != null)
		{
			BindingContext bindingContext = getContextForSingleFile(file);
			VariableDescriptor descriptor = BindingContextUtils.extractVariableDescriptorIfAny(bindingContext, simpleNameExpression, true);
			if(descriptor != null)
			{
				PsiElement declaration = BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
				if(declaration instanceof NapileProperty)
				{
					return (NapileProperty) declaration;
				}
			}
		}
		return null;
	}

	@Override
	public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException
	{
		NapileProperty property = getCorrespondingProperty(editor, (NapileFile) file);
		assert property != null && !property.isVar();

		NapileProperty newElement = NapilePsiFactory.createProperty(project, property.getText().replaceFirst(property.isVar() ? "var" : "val", property.isVar() ? "val" : "var"));
		property.replace(newElement);
	}

	@Override
	public boolean startInWriteAction()
	{
		return true;
	}
}
