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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.psi.NapileParameter;
import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.psi.NapilePropertyAccessor;
import org.napile.compiler.lang.psi.NapilePsiFactory;
import org.napile.compiler.lang.psi.NapileTypeReference;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.idea.plugin.JetBundle;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;

/**
 * @author svtk
 */
public class ChangeAccessorTypeFix extends JetIntentionAction<NapilePropertyAccessor>
{
	public ChangeAccessorTypeFix(@NotNull NapilePropertyAccessor element)
	{
		super(element);
	}

	@Nullable
	private JetType getPropertyType()
	{
		NapileProperty property = PsiTreeUtil.getParentOfType(element, NapileProperty.class);
		if(property == null)
			return null;
		return QuickFixUtil.getDeclarationReturnType(property);
	}

	@Override
	public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file)
	{
		JetType type = getPropertyType();
		return super.isAvailable(project, editor, file) && type != null && !ErrorUtils.isErrorType(type);
	}

	@NotNull
	@Override
	public String getText()
	{
		JetType type = getPropertyType();
		return element.isGetter() ? JetBundle.message("change.getter.type", type) : JetBundle.message("change.setter.type", type);
	}

	@NotNull
	@Override
	public String getFamilyName()
	{
		return JetBundle.message("change.accessor.type");
	}

	@Override
	public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException
	{
		JetType type = getPropertyType();
		if(type == null)
			return;
		NapilePropertyAccessor newElement = (NapilePropertyAccessor) element.copy();
		NapileTypeReference newTypeReference = NapilePsiFactory.createType(project, type.toString());

		if(element.isGetter())
		{
			NapileTypeReference returnTypeReference = newElement.getReturnTypeReference();
			assert returnTypeReference != null;
			CodeEditUtil.replaceChild(newElement.getNode(), returnTypeReference.getNode(), newTypeReference.getNode());
		}
		else
		{
			NapileParameter parameter = newElement.getParameter();
			assert parameter != null;
			NapileTypeReference typeReference = parameter.getTypeReference();
			assert typeReference != null;
			CodeEditUtil.replaceChild(parameter.getNode(), typeReference.getNode(), newTypeReference.getNode());
		}
		element.replace(newElement);
	}

	public static JetIntentionActionFactory createFactory()
	{
		return new JetIntentionActionFactory()
		{
			@Override
			public JetIntentionAction<NapilePropertyAccessor> createAction(Diagnostic diagnostic)
			{
				NapilePropertyAccessor accessor = QuickFixUtil.getParentElementOfType(diagnostic, NapilePropertyAccessor.class);
				if(accessor == null)
					return null;
				return new ChangeAccessorTypeFix(accessor);
			}
		};
	}
}
