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

package org.napile.compiler.codegen.processors.injection;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.codegen.processors.ExpressionGenerator;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.injection.CodeInjection;
import com.intellij.openapi.util.NotNullLazyKey;
import com.intellij.psi.PsiElement;
import com.intellij.util.NotNullFunction;

/**
 * @author VISTALL
 * @date 18:32/12.11.12
 */
public interface InjectionCodegen<T extends CodeInjection>
{
	InjectionCodegen<CodeInjection> EMPTY = new InjectionCodegen<CodeInjection>()
	{
		@NotNull
		@Override
		public StackValue gen(@NotNull PsiElement block, @NotNull StackValue data, ExpressionGenerator expressionGenerator)
		{
			return StackValue.none();
		}

		@NotNull
		@Override
		public Class<CodeInjection> getInjectionType()
		{
			return CodeInjection.class;
		}
	};

	NotNullLazyKey<InjectionCodegen<?>, CodeInjection> INJECTION_CODEGEN = NotNullLazyKey.create("injection-codegen", new NotNullFunction<CodeInjection, InjectionCodegen<?>>()
	{
		@NotNull
		@Override
		public InjectionCodegen<?> fun(CodeInjection dom)
		{
			InjectionCodegen<?> val = null;
			for(InjectionCodegen it : InjectionCodegenManager.INSTANCE)
				if(it.getInjectionType() == dom.getClass())
				{
					val = it;
					break;
				}

			return val == null ? EMPTY : val;
		}
	});

	@NotNull
	StackValue gen(@NotNull PsiElement block, @NotNull StackValue data, ExpressionGenerator expressionGenerator);

	@NotNull
	Class<T> getInjectionType();
}
