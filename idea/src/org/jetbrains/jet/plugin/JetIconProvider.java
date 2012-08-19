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

package org.jetbrains.jet.plugin;

import java.util.List;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;
import com.intellij.ide.IconProvider;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.RowIcon;
import com.intellij.util.BitUtil;
import com.intellij.util.PlatformIcons;
import com.intellij.util.containers.ContainerUtil;

/**
 * @author yole
 */
public class JetIconProvider extends IconProvider
{
	private static final Icon FINAL_MARK_ICON = IconLoader.getIcon("/nodes/finalMark.png");

	public static JetIconProvider INSTANCE = new JetIconProvider();

	@Override
	public Icon getIcon(@NotNull PsiElement psiElement, int flags)
	{
		Icon icon = null;
		if(psiElement instanceof JetFile)
		{
			JetFile file = (JetFile) psiElement;
			JetClassOrObject mainClass = getMainClass(file);
			icon = mainClass != null && file.getDeclarations().size() == 1 ? getIcon(mainClass, flags) : JetIcons.FILE;
		}
		else if(psiElement instanceof JetNamespaceHeader)
		{
			icon = (flags & Iconable.ICON_FLAG_OPEN) != 0 ? PlatformIcons.PACKAGE_OPEN_ICON : PlatformIcons.PACKAGE_ICON;
		}
		else if(psiElement instanceof JetNamedFunction)
		{
			icon = PsiTreeUtil.getParentOfType(psiElement, JetNamedDeclaration.class) instanceof JetClass ? PlatformIcons.METHOD_ICON : JetIcons.FUNCTION;
			if(((NapileMethod) psiElement).getReceiverTypeRef() != null)
				icon = JetIcons.EXTENSION_FUNCTION;
		}
		else if(psiElement instanceof JetClass)
		{
			JetClass jetClass = (JetClass) psiElement;
			icon = jetClass.hasModifier(JetTokens.ENUM_KEYWORD) ? PlatformIcons.ENUM_ICON : JetIcons.CLASS;

			if(jetClass.hasModifier(JetTokens.ABSTRACT_KEYWORD))
				icon = JetIcons.ABSTRACT_CLASS;

			if(jetClass instanceof JetEnumEntry)
				icon = PlatformIcons.ENUM_ICON;
		}
		else if(psiElement instanceof JetObjectDeclaration || psiElement instanceof JetClassObject)
		{
			icon = JetIcons.OBJECT;
		}
		else if(psiElement instanceof JetParameter)
		{
			icon = JetIcons.PARAMETER;
			JetParameter parameter = (JetParameter) psiElement;
			if(parameter.getValOrVarNode() != null)
			{
				JetParameterList parameterList = PsiTreeUtil.getParentOfType(psiElement, JetParameterList.class);
				if(parameterList != null && parameterList.getParent() instanceof JetClass)
					icon = parameter.isMutable() ? JetIcons.FIELD_VAR : JetIcons.FIELD_VAL;
			}
		}
		else if(psiElement instanceof JetProperty)
		{
			JetProperty property = (JetProperty) psiElement;
			icon = property.isVar() ? JetIcons.FIELD_VAR : JetIcons.FIELD_VAL;
		}

		return icon == null ? null : modifyIcon(psiElement instanceof JetModifierListOwner ? ((JetModifierListOwner) psiElement) : null, icon, flags);
	}

	@Nullable
	public static JetClassOrObject getMainClass(@NotNull JetFile file)
	{
		List<JetClass> classes = ContainerUtil.filter(file.getDeclarations(), new Condition<JetClass>()
		{
			@Override
			public boolean value(JetClass jetDeclaration)
			{
				return jetDeclaration instanceof JetClassOrObject;
			}
		});
		if(classes.size() == 1)
			return classes.get(0);
		return null;
	}

	public static Icon modifyIcon(JetModifierListOwner modifierList, Icon baseIcon, int flags)
	{
		RowIcon icon = new RowIcon(2);

		if(baseIcon != null)
			icon.setIcon(modifierList == null ? baseIcon : modifierList.hasModifier(JetTokens.FINAL_KEYWORD) ? new LayeredIcon(baseIcon, FINAL_MARK_ICON) : baseIcon, 0);

		if(modifierList != null && BitUtil.isSet(flags, Iconable.ICON_FLAG_VISIBILITY))
		{
			if(modifierList.hasModifier(JetTokens.LOCAL_KEYWORD))
				icon.setIcon(PlatformIcons.PRIVATE_ICON, 1);
			else if(modifierList.hasModifier(JetTokens.COVERED_KEYWORD))
				icon.setIcon(PlatformIcons.PROTECTED_ICON, 1);
			else if(modifierList.hasModifier(JetTokens.HERITABLE_KEYWORD))
				icon.setIcon(JetIcons.C_HERITABLE, 1);
			else
				icon.setIcon(PlatformIcons.PUBLIC_ICON, 1);
		}

		return icon;
	}
}
