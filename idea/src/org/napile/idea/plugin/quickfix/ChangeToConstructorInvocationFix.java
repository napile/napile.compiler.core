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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileDelegationSpecifier;
import org.napile.compiler.lang.psi.NapileDelegatorToSuperClass;
import org.napile.compiler.lang.psi.NapilePsiFactory;
import org.napile.idea.plugin.JetBundle;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;

/**
 * @author svtk
 */
public class ChangeToConstructorInvocationFix extends JetIntentionAction<NapileDelegatorToSuperClass>
{

	public ChangeToConstructorInvocationFix(@NotNull NapileDelegatorToSuperClass element)
	{
		super(element);
	}

	@NotNull
	@Override
	public String getText()
	{
		return JetBundle.message("change.to.constructor.invocation");
	}

	@NotNull
	@Override
	public String getFamilyName()
	{
		return JetBundle.message("change.to.constructor.invocation");
	}

	@Override
	public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException
	{
		NapileDelegatorToSuperClass delegator = (NapileDelegatorToSuperClass) element.copy();
		NapileClass aClass = NapilePsiFactory.createClass(project, "class A : " + delegator.getText() + "()");
		List<NapileDelegationSpecifier> delegationSpecifiers = aClass.getDelegationSpecifiers();
		assert delegationSpecifiers.size() == 1;
		NapileDelegationSpecifier specifier = delegationSpecifiers.iterator().next();
		element.replace(specifier);
	}

	public static JetIntentionActionFactory createFactory()
	{
		return new JetIntentionActionFactory()
		{
			@Override
			public JetIntentionAction<NapileDelegatorToSuperClass> createAction(Diagnostic diagnostic)
			{
				if(diagnostic.getPsiElement() instanceof NapileDelegatorToSuperClass)
				{
					return new ChangeToConstructorInvocationFix((NapileDelegatorToSuperClass) diagnostic.getPsiElement());
				}
				return null;
			}
		};
	}
}
