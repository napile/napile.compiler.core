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

package org.napile.java2napile.psi.visitor;

import java.util.ArrayList;
import java.util.List;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiVariable;

/**
 * @author VISTALL
 * @since 15:13/04.01.13
 */
public class ModifierConverter
{
	public static void modifiers(PsiModifierListOwner owner, StringBuilder builder)
	{
		PsiModifierList modifierList = owner.getModifierList();
		if(modifierList == null)
			return;


		List<String> modifiers = new ArrayList<String>();
		if(modifierList.hasModifierProperty(PsiModifier.STATIC))
			modifiers.add(PsiModifier.STATIC);

		if(owner instanceof PsiClass && ((PsiClass) owner).isInterface())
			modifiers.add("abstract");
		if(modifierList.hasModifierProperty(PsiModifier.PRIVATE))
			modifiers.add("local");
		if(modifierList.hasModifierProperty(PsiModifier.PROTECTED))
			modifiers.add("covered");
		if(modifierList.hasExplicitModifier(PsiModifier.ABSTRACT))
			modifiers.add("abstract");

		if(owner instanceof PsiVariable)
		{

		}
		else
		{
			if(modifierList.hasExplicitModifier(PsiModifier.FINAL))
				modifiers.add("final");

			if(modifierList.hasExplicitModifier(PsiModifier.NATIVE))
				modifiers.add("native");
		}

		builder.append(StringUtil.join(modifiers, " "));
		if(!modifiers.isEmpty())
			builder.append(ConverterVisitor.SPACE);
	}
}
