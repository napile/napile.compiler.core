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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.psi.NapileAnonymClass;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileMethod;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
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
	METHOD_OVERRIDING
			{
				@NotNull
				@Override
				protected List<NapileElement> getTargets(PsiElement element)
				{
					if(!(element instanceof NapileElement))
						return Collections.emptyList();

					NapileElement napileElement = (NapileElement) element;
					AnalyzeExhaust analyzeExhaust = ModuleAnalyzerUtil.analyze(napileElement.getContainingFile());

					DeclarationDescriptor descriptor = analyzeExhaust.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, napileElement);
					if(!isValidCallable(descriptor))
						return Collections.emptyList();

					List<NapileElement> list = new ArrayList<NapileElement>(((CallableDescriptor) descriptor).getOverriddenDescriptors().size());
					for(CallableDescriptor overrideDescriptor : ((CallableDescriptor) descriptor).getOverriddenDescriptors())
					{
						NapileElement declarationPsiElement = (NapileElement) BindingContextUtils.descriptorToDeclaration(analyzeExhaust.getBindingContext(), overrideDescriptor);
						list.add(declarationPsiElement);
					}

					return list;
				}

				@NotNull
				@Override
				protected String getTitle()
				{
					return "Go to overriding methods";
				}

				@NotNull
				@Override
				protected Icon getIcon()
				{
					return AllIcons.Gutter.OverridingMethod;
				}
			},
	METHOD_OVERRIDEN
			{
				@Override
				public void collectLineMarkers(@NotNull PsiElement element, Collection<LineMarkerInfo> infos)
				{
					if(!(element instanceof NapileClass))
						return;

					NapileClass napileClass = (NapileClass) element;
					AnalyzeExhaust analyzeExhaust = ModuleAnalyzerUtil.analyze(napileClass.getContainingFile());

					BodiesResolveContext context = analyzeExhaust.getBodiesResolveContext();

					ClassDescriptor descriptor = analyzeExhaust.getBindingContext().get(BindingContext.CLASS, napileClass);
					if(descriptor == null)
						return;

					for(NapileDeclaration declaration : napileClass.getDeclarations())
					{
						if(declaration instanceof NapileMethod)
						{
							MethodDescriptor methodDescriptor = analyzeExhaust.getBindingContext().get(BindingContext.METHOD, declaration);
							if(methodDescriptor == null)
								continue;

							final List<NapileElement> elements = new ArrayList<NapileElement>();
							for(Map.Entry<NapileClass, MutableClassDescriptor> entry : context.getClasses().entrySet())
							{
								NapileClass class2 = entry.getKey();
								MutableClassDescriptor descriptor2 = entry.getValue();
								if(DescriptorUtils.isSubclass(descriptor2, descriptor))
								{
									for(NapileDeclaration declaration2 : class2.getDeclarations())
									{
										if(declaration2 instanceof NapileMethod)
										{
											MethodDescriptor methodDescriptor2 = analyzeExhaust.getBindingContext().get(BindingContext.METHOD, declaration2);
											if(methodDescriptor2 != null && methodDescriptor2.getOverriddenDescriptors().contains(methodDescriptor))
												elements.add(declaration2);
										}
									}
								}
							}

							PsiElement name = ((PsiNameIdentifierOwner)declaration).getNameIdentifier();
							if(name == null)
								continue;
							if(elements.isEmpty())
								continue;

							infos.add(new LineMarkerInfo<PsiElement>(name, name.getTextRange(), getIcon(), Pass.UPDATE_OVERRIDEN_MARKERS, new Function<PsiElement, String>()
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
									PsiElementListNavigator.openTargets(e, elements.toArray(new NapileElement[elements.size()]), getTitle(), getTitle(), new DefaultPsiElementCellRenderer());
								}
							}, GutterIconRenderer.Alignment.LEFT
							));
						}
					}
				}

				@NotNull
				@Override
				protected List<NapileElement> getTargets(PsiElement element)
				{
					return Collections.emptyList();
				}

				@NotNull
				@Override
				protected String getTitle()
				{
					return "Go to overrided methods";
				}

				@NotNull
				@Override
				protected Icon getIcon()
				{
					return AllIcons.Gutter.OverridenMethod;
				}
			},
	CLASS_OVERRIDEN
			{
				@NotNull
				@Override
				public Icon getIcon()
				{
					return AllIcons.Gutter.OverridenMethod;
				}

				@NotNull
				@Override
				public String getTitle()
				{
					return "Go to overriding classes";
				}

				@NotNull
				@Override
				protected List<NapileElement> getTargets(PsiElement element)
				{
					if(!(element instanceof NapileClass))
						return Collections.emptyList();

					final NapileClass napileClass = (NapileClass) element;
					AnalyzeExhaust analyzeExhaust = ModuleAnalyzerUtil.analyze(napileClass.getContainingFile());

					BindingContext bindingContext = analyzeExhaust.getBindingContext();
					ClassDescriptor classDeclaration = (ClassDescriptor) bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
					if(classDeclaration == null)
						return Collections.emptyList();
					List<NapileElement> result = new ArrayList<NapileElement>();

					for(Map.Entry<NapileClass, MutableClassDescriptor> entry : analyzeExhaust.getBodiesResolveContext().getClasses().entrySet())
					{
						if(entry.getValue() == classDeclaration)
							continue;

						if(DescriptorUtils.isSubclass(entry.getValue(), classDeclaration))
							result.add(entry.getKey());
					}

					for(Map.Entry<NapileAnonymClass, MutableClassDescriptor> entry : analyzeExhaust.getBodiesResolveContext().getAnonymous().entrySet())
					{
						if(entry.getValue() == classDeclaration)
							continue;

						if(DescriptorUtils.isSubclass(entry.getValue(), classDeclaration))
							result.add(entry.getKey());
					}

					return result;
				}
			};

	protected boolean isValidCallable(@Nullable DeclarationDescriptor declarationDescriptor)
	{
		return declarationDescriptor instanceof CallableDescriptor && !(declarationDescriptor instanceof CallParameterDescriptor);
	}

	public void collectLineMarkers(@NotNull PsiElement element, Collection<LineMarkerInfo> infos)
	{
		LineMarkerInfo lineMarkerInfo = getLineMarkers(element);
		if(lineMarkerInfo != null)
			infos.add(lineMarkerInfo);
	}

	@Nullable
	protected LineMarkerInfo getLineMarkers(@NotNull PsiElement element)
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
						PsiElementListNavigator.openTargets(e, objects.toArray(new NapileElement[objects.size()]), getTitle(), getTitle(), new DefaultPsiElementCellRenderer());
					}
				}, GutterIconRenderer.Alignment.LEFT
				);
			}
		}
		return null;
	}

	@NotNull
	protected abstract List<NapileElement> getTargets(PsiElement element);

	@NotNull
	protected abstract String getTitle();

	@NotNull
	protected abstract Icon getIcon();
}
