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

package org.napile.idea.plugin.editor.presentation;

import javax.swing.Icon;

import org.napile.asm.resolve.name.FqName;
import org.napile.compiler.lang.psi.NapilePsiUtil;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.util.QualifiedNamesUtil;
import org.napile.idea.plugin.NapileIconProvider;
import org.napile.idea.plugin.util.IdePsiUtil;
import com.intellij.navigation.ColoredItemPresentation;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProvider;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.Iconable;

/**
 * @author VISTALL
 * @date 11:24/27.02.13
 */
public class NapileVariablePresenter implements ItemPresentationProvider<NapileVariable>
{
	@Override
	public ItemPresentation getPresentation(final NapileVariable variable)
	{
		return new ColoredItemPresentation()
		{
			@Override
			public TextAttributesKey getTextAttributesKey()
			{
				if(IdePsiUtil.isDeprecated(variable))
					return CodeInsightColors.DEPRECATED_ATTRIBUTES;
				return null;
			}

			@Override
			public String getPresentableText()
			{
				return variable.getName();
			}

			@Override
			public String getLocationString()
			{
				FqName name = NapilePsiUtil.getFQName(variable);
				if(name != null)
				{
					return String.format("(in %s)", QualifiedNamesUtil.withoutLastSegment(name));
				}

				return "";
			}

			@Override
			public Icon getIcon(boolean open)
			{
				return NapileIconProvider.INSTANCE.getIcon(variable, Iconable.ICON_FLAG_VISIBILITY);
			}
		};
	}
}
