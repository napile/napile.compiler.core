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

package org.napile.idea.plugin;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.lib.NapileAnnotationPackage;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptorWithVisibility;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.Modality;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.resolve.AnnotationUtils;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.util.RunUtil;
import com.intellij.openapi.util.Iconable;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.RowIcon;
import com.intellij.util.BitUtil;
import com.intellij.util.PlatformIcons;

/**
 * @author VISTALL
 * @date 20:35/24.01.13
 * FIXME [VISTALL] rework
 */
public class NapileIconProvider2
{
	public static Icon getIcon(DeclarationDescriptor declarationDescriptor)
	{
		if(declarationDescriptor instanceof TypeParameterDescriptor)
			return NapileIcons.TYPE_PARAMETER;

		if(!(declarationDescriptor instanceof DeclarationDescriptorWithVisibility))
			return null;

		boolean isRunnable = false;
		DeclarationDescriptorWithVisibility descriptorWithVisibility = (DeclarationDescriptorWithVisibility) declarationDescriptor;
		Icon baseIcon = null;
		if(declarationDescriptor instanceof ClassDescriptor)
		{
			ClassDescriptor classDescriptor = (ClassDescriptor) descriptorWithVisibility;
			baseIcon = descriptorWithVisibility.getModality() == Modality.ABSTRACT ? NapileIcons.ABSTRACT_CLASS : NapileIcons.CLASS;


			if(classDescriptor.isTraited())
				baseIcon = descriptorWithVisibility.getModality() == Modality.ABSTRACT ? NapileIcons.ABSTRACT_CLASS_TRAITED : NapileIcons.CLASS_TRAITED;

			//if(napileClass.hasModifier(NapileTokens.UTIL_KEYWORD))
			//	baseIcon = NapileIcons.UTIL;

			if(AnnotationUtils.isAnnotation(classDescriptor))
			{
				baseIcon = NapileIcons.ANNOTATION;
				if(AnnotationUtils.hasAnnotation(descriptorWithVisibility, NapileAnnotationPackage.REPEATABLE))
					baseIcon = NapileIcons.REPEATABLE_ANNOTATION;
			}

			if(DescriptorUtils.isSubclassOf(classDescriptor, NapileLangPackage.EXCEPTION))
				baseIcon = descriptorWithVisibility.getModality() == Modality.ABSTRACT ? NapileIcons.ABSTRACT_THROWABLE : NapileIcons.THROWABLE;

			if(classDescriptor instanceof MutableClassDescriptor)
				for(MethodDescriptor m : ((MutableClassDescriptor) classDescriptor).getMethods())
					if(RunUtil.isRunPoint(m))
					{
						isRunnable = true;
						break;
					}
		}
		else if(declarationDescriptor instanceof VariableDescriptor)
			baseIcon = NapileIcons.VARIABLE;
		else if(declarationDescriptor instanceof MethodDescriptor)
			baseIcon = NapileIcons.METHOD;

		if(baseIcon == null)
			return null;

		return modifyIcon(descriptorWithVisibility, baseIcon, Iconable.ICON_FLAG_VISIBILITY, isRunnable);
	}

	public static Icon modifyIcon(@NotNull DeclarationDescriptorWithVisibility descriptorWithVisibility, Icon baseIcon, int flags, boolean isRunnable)
	{
		RowIcon icon = new RowIcon(2);

		if(baseIcon != null)
		{
			if(descriptorWithVisibility.getModality() == Modality.FINAL || isRunnable)
			{
				List<Icon> icons = new ArrayList<Icon>(2);
				icons.add(baseIcon);
				if(descriptorWithVisibility.getModality() == Modality.FINAL)
					icons.add(NapileIconProvider.FINAL_MARK_ICON);
				if(isRunnable)
					icons.add(NapileIconProvider.RUNNABLE_MARK);

				icon.setIcon(new LayeredIcon(icons.toArray(new Icon[icons.size()])), 0);
			}
			else
				icon.setIcon(baseIcon, 0);
		}

		if(BitUtil.isSet(flags, Iconable.ICON_FLAG_VISIBILITY))
		{
			switch(descriptorWithVisibility.getVisibility())
			{
				case LOCAL:
					icon.setIcon(PlatformIcons.PRIVATE_ICON, 1);
					break;
				case HERITABLE:
					icon.setIcon(NapileIcons.C_HERITABLE, 1);
					break;
				case COVERED:
					icon.setIcon(PlatformIcons.PROTECTED_ICON, 1);
					break;
				default:
					icon.setIcon(PlatformIcons.PUBLIC_ICON, 1);
					break;
			}
		}

		return icon;
	}
}
