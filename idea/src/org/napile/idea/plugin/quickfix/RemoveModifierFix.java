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
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.impl.NapileModifierListImpl;
import org.napile.compiler.lang.lexer.NapileKeywordToken;
import org.napile.compiler.lang.lexer.NapileToken;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileModifierListOwner;
import org.napile.idea.plugin.JetBundle;
import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;

/**
 * @author svtk
 */
public class RemoveModifierFix extends JetIntentionAction<NapileModifierListOwner>
{
	private final NapileKeywordToken modifier;
	private final boolean isRedundant;

	public RemoveModifierFix(@NotNull NapileModifierListOwner element, @NotNull NapileKeywordToken modifier, boolean isRedundant)
	{
		super(element);
		this.modifier = modifier;
		this.isRedundant = isRedundant;
	}

	private static String makeText(@Nullable NapileModifierListOwner element, NapileKeywordToken modifier, boolean isRedundant)
	{
		if(isRedundant)
		{
			return JetBundle.message("remove.redundant.modifier", modifier.getValue());
		}
		if(element != null && (modifier == NapileTokens.ABSTRACT_KEYWORD))
		{
			return JetBundle.message("make.element.not.modifier", AddModifierFix.getElementName(element), modifier.getValue());
		}
		return JetBundle.message("remove.modifier", modifier.getValue());
	}

	@NotNull
	@Override
	public String getFamilyName()
	{
		return JetBundle.message("remove.modifier.family");
	}

	@NotNull
	private static <T extends NapileModifierListOwner> T removeModifier(T element, NapileToken modifier)
	{
		NapileModifierListImpl modifierList = (NapileModifierListImpl)element.getModifierList();
		assert modifierList != null;
		removeModifierFromList(modifierList, modifier);
		if(modifierList.getFirstChild() == null)
		{
			PsiElement whiteSpace = modifierList.getNextSibling();
			assert element instanceof ASTDelegatePsiElement;
			((ASTDelegatePsiElement) element).deleteChildInternal(modifierList.getNode());
			QuickFixUtil.removePossiblyWhiteSpace((ASTDelegatePsiElement) element, whiteSpace);
		}
		return element;
	}

	@NotNull
	private static NapileModifierList removeModifierFromList(@NotNull NapileModifierListImpl modifierList, NapileToken modifier)
	{
		assert modifierList.hasModifier(modifier);
		ASTNode modifierNode = modifierList.getModifierNode(modifier);
		PsiElement whiteSpace = modifierNode.getPsi().getNextSibling();
		boolean wsRemoved = QuickFixUtil.removePossiblyWhiteSpace(modifierList, whiteSpace);
		modifierList.deleteChildInternal(modifierNode);
		if(!wsRemoved)
		{
			QuickFixUtil.removePossiblyWhiteSpace(modifierList, modifierList.getLastChild());
		}
		return modifierList;
	}

	@NotNull
	@Override
	public String getText()
	{
		return makeText(element, modifier, isRedundant);
	}

	@Override
	public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException
	{
		NapileModifierListOwner newElement = (NapileModifierListOwner) element.copy();
		element.replace(removeModifier(newElement, modifier));
	}


	public static JetIntentionActionFactory createRemoveModifierFromListOwnerFactory(final NapileKeywordToken modifier)
	{
		return createRemoveModifierFromListOwnerFactory(modifier, false);
	}

	public static JetIntentionActionFactory createRemoveModifierFromListOwnerFactory(final NapileKeywordToken modifier, final boolean isRedundant)
	{
		return new JetIntentionActionFactory()
		{
			@Nullable
			@Override
			public JetIntentionAction<NapileModifierListOwner> createAction(Diagnostic diagnostic)
			{
				NapileModifierListOwner modifierListOwner = QuickFixUtil.getParentElementOfType(diagnostic, NapileModifierListOwner.class);
				if(modifierListOwner == null)
					return null;
				return new RemoveModifierFix(modifierListOwner, modifier, isRedundant);
			}
		};
	}

	public static JetIntentionActionFactory createRemoveModifierFactory()
	{
		return createRemoveModifierFactory(false);
	}

	public static JetIntentionActionFactory createRemoveModifierFactory(final boolean isRedundant)
	{
		return new JetIntentionActionFactory()
		{
			@Nullable
			@Override
			public JetIntentionAction<NapileModifierListOwner> createAction(Diagnostic diagnostic)
			{
				NapileModifierListOwner modifierListOwner = QuickFixUtil.getParentElementOfType(diagnostic, NapileModifierListOwner.class);
				if(modifierListOwner == null)
					return null;
				PsiElement psiElement = diagnostic.getPsiElement();
				IElementType elementType = psiElement.getNode().getElementType();
				if(!(elementType instanceof NapileKeywordToken))
					return null;
				NapileKeywordToken modifier = (NapileKeywordToken) elementType;
				return new RemoveModifierFix(modifierListOwner, modifier, isRedundant);
			}
		};
	}
}
