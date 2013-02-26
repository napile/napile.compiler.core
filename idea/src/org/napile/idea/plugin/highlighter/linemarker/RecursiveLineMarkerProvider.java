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

package org.napile.idea.plugin.highlighter.linemarker;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.psi.NapileCallExpression;
import org.napile.compiler.lang.psi.NapileMethod;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;

/**
 * @author VISTALL
 * @date 22:07/10.02.13
 */
public class RecursiveLineMarkerProvider implements LineMarkerProvider
{
	@Nullable
	@Override
	public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element)
	{
		return null;
	}

	@Override
	public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result)
	{
		for(PsiElement psiElement : elements)
		{
			if(psiElement instanceof NapileCallExpression)
			{
				NapileCallExpression exp = (NapileCallExpression) psiElement;

				AnalyzeExhaust analyzeExhaust = ModuleAnalyzerUtil.analyze(exp.getContainingFile());
				BindingContext bindingContext = analyzeExhaust.getBindingContext();

				ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, exp.getCalleeExpression());
				if(resolvedCall == null)
					continue;

				NapileMethod methodOrMacro = PsiTreeUtil.getParentOfType(exp, NapileMethod.class);
				if(methodOrMacro == null)
					continue;

				SimpleMethodDescriptor methodDescriptor = bindingContext.get(BindingContext.METHOD, methodOrMacro);
				if(methodDescriptor == null)
					continue;

				if(methodDescriptor == resolvedCall.getResultingDescriptor())
				{
					result.add(new LineMarkerInfo<PsiElement>(exp, exp.getTextRange(), AllIcons.Gutter.RecursiveMethod, Pass.UPDATE_OVERRIDEN_MARKERS, Function.NULL, new GutterIconNavigationHandler<PsiElement>()
					{
						@Override
						public void navigate(MouseEvent e, PsiElement elt)
						{

						}
					}, GutterIconRenderer.Alignment.LEFT)
					{
						@Nullable
						@Override
						public String getLineMarkerTooltip()
						{
							return "Recursive call";
						}
					});
				}
			}
		}
	}
}
