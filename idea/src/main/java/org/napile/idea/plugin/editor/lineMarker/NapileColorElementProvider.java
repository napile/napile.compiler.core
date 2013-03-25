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

package org.napile.idea.plugin.editor.lineMarker;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.FqNameUnsafe;
import org.napile.compiler.analyzer.AnalyzeExhaust;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.psi.NapileCallExpression;
import org.napile.compiler.lang.psi.NapileDelegationToSuperCall;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.ValueArgument;
import org.napile.compiler.lang.psi.util.Constant;
import org.napile.compiler.lang.psi.util.ConstantUtil;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.idea.plugin.module.ModuleAnalyzerUtil;
import com.intellij.openapi.editor.ElementColorProvider;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 12:23/27.02.13
 */
public class NapileColorElementProvider implements ElementColorProvider
{

	private static final FqNameUnsafe RGB_COLOR = new FqNameUnsafe("napile.ui.RgbColor");

	private enum ConstructorOfRgbColor
	{
		INT_INT_INT(NapileLangPackage.INT, NapileLangPackage.INT, NapileLangPackage.INT)
				{
					@NotNull
					@Override
					Color getColor(@NotNull List<Constant> constants)
					{
						return new Color((Integer) constants.get(0).getValue(), (Integer) constants.get(1).getValue(), (Integer) constants.get(2).getValue());
					}
				};

		private static final ConstructorOfRgbColor[] VALUES = values();

		private FqName[] fqNames;

		ConstructorOfRgbColor(FqName... fqNames)
		{
			this.fqNames = fqNames;
		}

		@NotNull
		abstract Color getColor(List<Constant> constants);

		@Nullable
		static ConstructorOfRgbColor findIt(@NotNull List<Constant> constants)
		{
			loop: for(ConstructorOfRgbColor constructorOfRgbColor : values())
			{
				final FqName[] fq = constructorOfRgbColor.fqNames;
				if(fq.length != constants.size())
				{
					continue;
				}

				for(int i = 0; i < fq.length; i++)
				{
					FqName fqName = fq[i];
					Constant constant = constants.get(i);

					if(!fqName.equals(constant.getFqName()))
					{
						continue loop;
					}
				}

				return constructorOfRgbColor;
			}

			return null;
		}
	}

	@Nullable
	@Override
	public Color getColorFrom(@NotNull PsiElement element)
	{
		if(!(element instanceof NapileElement))
		{
			return null;
		}

		if(element instanceof NapileCallExpression)
		{
			final AnalyzeExhaust analyzeExhaust = ModuleAnalyzerUtil.lastAnalyze((NapileFile) element.getContainingFile());

			final ResolvedCall<? extends CallableDescriptor> resolvedCall = analyzeExhaust.getBindingTrace().get(BindingTraceKeys.RESOLVED_CALL, ((NapileCallExpression) element).getCalleeExpression());

			CallableDescriptor descriptor = resolvedCall == null ? null : resolvedCall.getResultingDescriptor();

			if(descriptor instanceof ConstructorDescriptor && DescriptorUtils.getFQName(descriptor.getContainingDeclaration()).equals(RGB_COLOR))
			{
				return getColorOf(analyzeExhaust.getBindingTrace(), ((NapileCallExpression) element).getValueArguments());
			}
		}
		else if(element instanceof NapileDelegationToSuperCall)
		{
			final AnalyzeExhaust analyzeExhaust = ModuleAnalyzerUtil.lastAnalyze((NapileFile) element.getContainingFile());

			final JetType type = analyzeExhaust.getBindingTrace().get(BindingTraceKeys.TYPE, ((NapileDelegationToSuperCall) element).getTypeReference());
			if(type == null)
			{
				return null;
			}

			if(RGB_COLOR.equals(TypeUtils.getFqName(type)))
			{
				return getColorOf(analyzeExhaust.getBindingTrace(), ((NapileDelegationToSuperCall) element).getValueArguments());
			}
		}
		return null;
	}

	private static Color getColorOf(BindingTrace bindingTrace, List<? extends ValueArgument> valueArgumentList)
	{
		List<Constant> constants = new ArrayList<Constant>(valueArgumentList.size());

		//FIXME [VISTALL] named creating?
		for(ValueArgument valueArgument : valueArgumentList)
		{
			constants.add(ConstantUtil.getConstant(bindingTrace, valueArgument.getArgumentExpression()));
		}

		ConstructorOfRgbColor constructorOfRgbColor = ConstructorOfRgbColor.findIt(constants);
		if(constructorOfRgbColor == null)
		{
			return null;
		}

		return constructorOfRgbColor.getColor(constants);
	}

	@Override
	public void setColorTo(@NotNull PsiElement element, @NotNull Color color)
	{
		JOptionPane.showMessageDialog(null, "Currently not supported");
	}
}
