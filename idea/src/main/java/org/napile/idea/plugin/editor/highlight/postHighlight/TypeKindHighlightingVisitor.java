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

package org.napile.idea.plugin.editor.highlight.postHighlight;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.lib.NapileAnnotationPackage;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.Modality;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.NapileTypeParameter;
import org.napile.compiler.lang.resolve.AnnotationUtils;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.idea.plugin.editor.highlight.NapileHighlightingColors;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

/**
 * @author VISTALL
 * @since 21:44/26.02.13
 */
public class TypeKindHighlightingVisitor extends PostHighlightVisitor
{
	public TypeKindHighlightingVisitor(BindingContext context, List<HighlightInfo> holder)
	{
		super(context, holder);
	}

	@Override
	public void visitSimpleNameExpression(NapileSimpleNameExpression expression)
	{
		super.visitSimpleNameExpression(expression);
		PsiReference ref = expression.getReference();
		if(ref == null)
			return;

		if(NapileTokens.KEYWORDS.contains(expression.getReferencedNameElementType()))
			return;
		DeclarationDescriptor referenceTarget = bindingContext.get(BindingContext.REFERENCE_TARGET, expression);
		if(referenceTarget instanceof ConstructorDescriptor)
		{
			referenceTarget = referenceTarget.getContainingDeclaration();
		}

		if(referenceTarget instanceof ClassDescriptor)
		{
			highlightClassByKind((ClassDescriptor) referenceTarget, expression);
		}
		else if(referenceTarget instanceof TypeParameterDescriptor)
		{
			highlightName(expression, NapileHighlightingColors.TYPE_PARAMETER, referenceTarget);
		}
	}

	@Override
	public void visitTypeParameter(NapileTypeParameter parameter)
	{
		PsiElement identifier = parameter.getNameIdentifier();
		if(identifier != null)
		{
			highlightName(identifier, NapileHighlightingColors.TYPE_PARAMETER, null);
		}
		super.visitTypeParameter(parameter);
	}

	@Override
	public void visitClass(NapileClass klass)
	{
		PsiElement identifier = klass.getNameIdentifier();
		ClassDescriptor classDescriptor = bindingContext.get(BindingContext.CLASS, klass);
		if(identifier != null && classDescriptor != null)
		{
			highlightClassByKind(classDescriptor, identifier);
		}
		super.visitClass(klass);
	}

	private void highlightClassByKind(@NotNull ClassDescriptor classDescriptor, @NotNull PsiElement whatToHighlight)
	{
		TextAttributesKey textAttributes;
		if(AnnotationUtils.hasAnnotation(classDescriptor, NapileAnnotationPackage.ANNOTATION))
			textAttributes = NapileHighlightingColors.ANNOTATION;
		else
			textAttributes = classDescriptor.getModality() == Modality.ABSTRACT ? NapileHighlightingColors.ABSTRACT_CLASS : NapileHighlightingColors.CLASS;

		highlightName(whatToHighlight, textAttributes, classDescriptor);
	}
}
