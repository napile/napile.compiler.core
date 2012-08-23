/*
 * Copyright 2010-2012 napile.org
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

package org.napile.idea.plugin.highlighter.linemarker;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileClassOrObject;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.idea.plugin.caches.JetShortNamesCache;
import org.napile.idea.plugin.project.WholeProjectAnalyzerFacade;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.util.Function;

/**
 * @author VISTALL
 * @date 17:54/23.08.12
 */
public enum LineMarkers
{
	CLASS_OVERRIDEN
			{
				@Override
				public Icon getIcon()
				{
					return AllIcons.Gutter.OverridenMethod;
				}

				@Override
				public String getTitle()
				{
					return "Go to overriding classes";
				}

				@Override
				protected List<NapileElement> getTargets(PsiElement element)
				{
					if(!(element instanceof NapileClass))
						return Collections.emptyList();

					final NapileClass napileClass = (NapileClass) element;
					AnalyzeExhaust analyzeExhaust = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(napileClass.getContainingFile());

					BindingContext bindingContext = analyzeExhaust.getBindingContext();
					ClassDescriptor classDeclaration = (ClassDescriptor) bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);

					List<NapileElement> result = new ArrayList<NapileElement>();

					Map<NapileClassOrObject, ClassDescriptor> res = JetShortNamesCache.getInstance(napileClass.getProject()).getAllClassesAndDescriptors(napileClass, napileClass.getResolveScope());
					for(Map.Entry<NapileClassOrObject, ClassDescriptor> entry : res.entrySet())
					{
						if(entry.getValue() == classDeclaration)
							continue;

						if(DescriptorUtils.isSubclass(entry.getValue(), classDeclaration))
							result.add(entry.getKey());
					}

					return result;
				}
			};

	public final LineMarkerInfo getLineMarkers(@NotNull PsiElement element)
	{
		if(element instanceof PsiNameIdentifierOwner)
		{
			List<NapileElement> result = getTargets(element);

			if(!result.isEmpty())
			{
				PsiElement name = ((PsiNameIdentifierOwner)element).getNameIdentifier();
				if(name == null)
					return null;

				return new LineMarkerInfo<PsiElement>(name, name.getTextRange(), getIcon(), Pass.UPDATE_OVERRIDEN_MARKERS, new Function<PsiElement, String>()
				{
					@Override
					public String fun(PsiElement element)
					{
						return getTitle();
					}
				}, new GutterIconNavigationHandler<PsiElement>()
				{
					@Override
					public void navigate(MouseEvent e, PsiElement elt)
					{
						List<NapileElement> objects = getTargets(elt.getParent());
						PsiElementListNavigator.openTargets(e, objects.toArray(new NapileElement[objects.size()]), getTitle(), new DefaultPsiElementCellRenderer());
					}
				}, GutterIconRenderer.Alignment.LEFT
				);
			}
		}
		return null;
	}

	protected abstract List<NapileElement> getTargets(PsiElement element);

	protected abstract String getTitle();

	protected abstract Icon getIcon();
}
