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

import static org.napile.compiler.lexer.JetTokens.ABSTRACT_KEYWORD;
import static org.napile.compiler.lexer.JetTokens.FINAL_KEYWORD;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.NapileModifierListOwner;
import org.napile.compiler.lang.psi.NapilePropertyAccessor;
import org.napile.compiler.lang.psi.NapilePsiFactory;
import org.napile.compiler.lexer.NapileKeywordToken;
import org.napile.compiler.lexer.NapileToken;
import org.napile.idea.plugin.JetBundle;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.util.IncorrectOperationException;

/**
 * @author svtk
 */
public class AddModifierFix extends JetIntentionAction<NapileModifierListOwner>
{
	private final NapileKeywordToken modifier;
	private final NapileToken[] modifiersThanCanBeReplaced;

	private AddModifierFix(@NotNull NapileModifierListOwner element, @NotNull NapileKeywordToken modifier, @Nullable NapileToken[] modifiersThanCanBeReplaced)
	{
		super(element);
		this.modifier = modifier;
		this.modifiersThanCanBeReplaced = modifiersThanCanBeReplaced;
	}

	@NotNull
    /*package*/ static String getElementName(@NotNull NapileModifierListOwner modifierListOwner)
	{
		String name = null;
		if(modifierListOwner instanceof PsiNameIdentifierOwner)
		{
			PsiElement nameIdentifier = ((PsiNameIdentifierOwner) modifierListOwner).getNameIdentifier();
			if(nameIdentifier != null)
			{
				name = nameIdentifier.getText();
			}
		}
		else if(modifierListOwner instanceof NapilePropertyAccessor)
		{
			name = ((NapilePropertyAccessor) modifierListOwner).getNamePlaceholder().getText();
		}
		if(name == null)
		{
			name = modifierListOwner.getText();
		}
		return "'" + name + "'";
	}

	@NotNull
	@Override
	public String getText()
	{
		if(modifier == ABSTRACT_KEYWORD)
		{
			return JetBundle.message("make.element.modifier", getElementName(element), modifier.getValue());
		}
		return JetBundle.message("add.modifier", modifier.getValue());
	}

	@NotNull
	@Override
	public String getFamilyName()
	{
		return "Add modifier";
	}

	@Override
	public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException
	{
		element.replace(addModifier(element, modifier, modifiersThanCanBeReplaced, project, false));
	}

	@NotNull
    /*package*/ static NapileModifierListOwner addModifier(@NotNull PsiElement element, @NotNull NapileKeywordToken modifier, @Nullable NapileToken[] modifiersThatCanBeReplaced, @NotNull Project project, boolean toBeginning)
	{
		NapileModifierListOwner newElement = (NapileModifierListOwner) (element.copy());

		NapileModifierList modifierList = newElement.getModifierList();
		NapileModifierList listWithModifier = NapilePsiFactory.createModifier(project, modifier);
		PsiElement whiteSpace = NapilePsiFactory.createWhiteSpace(project);
		if(modifierList == null)
		{
			PsiElement firstChild = newElement.getFirstChild();
			newElement.addBefore(listWithModifier, firstChild);
			newElement.addBefore(whiteSpace, firstChild);
		}
		else
		{
			boolean replaced = false;
			if(modifiersThatCanBeReplaced != null)
			{
				PsiElement toBeReplaced = null;
				PsiElement toReplace = null;
				for(NapileToken modifierThatCanBeReplaced : modifiersThatCanBeReplaced)
				{
					if(modifierList.hasModifier(modifierThatCanBeReplaced))
					{
						PsiElement modifierElement = modifierList.getModifierNode(modifierThatCanBeReplaced).getPsi();
						assert modifierElement != null;
						if(!replaced)
						{
							toBeReplaced = modifierElement;
							toReplace = listWithModifier.getFirstChild();
							//modifierElement.replace(listWithModifier.getFirstChild());
							replaced = true;
						}
						else
						{
							modifierList.deleteChildInternal(modifierElement.getNode());
						}
					}
				}
				if(toBeReplaced != null && toReplace != null)
				{
					toBeReplaced.replace(toReplace);
				}
			}
			if(!replaced)
			{
				if(toBeginning)
				{
					PsiElement firstChild = modifierList.getFirstChild();
					modifierList.addBefore(listWithModifier.getFirstChild(), firstChild);
					modifierList.addBefore(whiteSpace, firstChild);
				}
				else
				{
					PsiElement lastChild = modifierList.getLastChild();
					modifierList.addAfter(listWithModifier.getFirstChild(), lastChild);
					modifierList.addAfter(whiteSpace, lastChild);
				}
			}
		}
		return newElement;
	}

	@Override
	public boolean startInWriteAction()
	{
		return true;
	}

	public static <T extends NapileModifierListOwner> JetIntentionActionFactory createFactory(final NapileKeywordToken modifier, final Class<T> modifierOwnerClass)
	{
		return new JetIntentionActionFactory()
		{
			@Override
			public IntentionAction createAction(Diagnostic diagnostic)
			{
				NapileModifierListOwner modifierListOwner = QuickFixUtil.getParentElementOfType(diagnostic, modifierOwnerClass);
				if(modifierListOwner == null)
					return null;
				return new AddModifierFix(modifierListOwner, modifier, MODIFIERS_THAT_CAN_BE_REPLACED.get(modifier));
			}
		};
	}

	public static JetIntentionActionFactory createFactory(final NapileKeywordToken modifier)
	{
		return createFactory(modifier, NapileModifierListOwner.class);
	}

	private static Map<NapileToken, NapileToken[]> MODIFIERS_THAT_CAN_BE_REPLACED = new HashMap<NapileToken, NapileToken[]>();

	static
	{
		MODIFIERS_THAT_CAN_BE_REPLACED.put(ABSTRACT_KEYWORD, new NapileToken[]{FINAL_KEYWORD});
	}
}
