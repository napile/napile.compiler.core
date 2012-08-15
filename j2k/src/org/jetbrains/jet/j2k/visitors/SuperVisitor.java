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

package org.jetbrains.jet.j2k.visitors;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.ast.SuperConstructorCall;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReference;


/**
 * @author ignatov
 */
public class SuperVisitor extends JavaRecursiveElementVisitor
{
	@NotNull
	private final List<SuperConstructorCall> superCall = new ArrayList<SuperConstructorCall>();

	private final Converter converter;

	public SuperVisitor(Converter converter)
	{
		this.converter = converter;
	}

	@Override
	public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression)
	{
		super.visitMethodCallExpression(expression);

		String call = getSuperCallName(expression);
		if(call != null)
			superCall.add(new SuperConstructorCall(call, converter.expressionsToExpressionList(expression.getArgumentList().getExpressions())));
	}

	static String getSuperCallName(@NotNull PsiMethodCallExpression expression)
	{
		PsiReference reference = expression.getMethodExpression();

		if(reference.getCanonicalText().equals("super"))
		{
			final PsiElement baseConstructor = reference.resolve();
			if(baseConstructor != null && baseConstructor instanceof PsiMethod && ((PsiMethod) baseConstructor).isConstructor())
				return ((PsiMethod) baseConstructor).getContainingClass().getName();
		}
		else if(reference.getCanonicalText().equals("this"))
		{
			final PsiElement baseConstructor = reference.resolve();
			if(baseConstructor != null && baseConstructor instanceof PsiMethod && ((PsiMethod) baseConstructor).isConstructor())
				return "this";
		}
		return null;
	}

	@NotNull
	public List<SuperConstructorCall> getSuperCall()
	{
		return superCall;
	}
}
