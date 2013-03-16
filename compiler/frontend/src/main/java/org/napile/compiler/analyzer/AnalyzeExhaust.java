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

package org.napile.compiler.analyzer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.di.InjectorForTopDownAnalyzerBasic;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BodiesResolveContext;

/**
 * @author Stepan Koltsov
 */
public class AnalyzeExhaust
{
	@NotNull
	private final BindingContext bindingContext;
	private final Throwable error;

	@NotNull
	private final BodiesResolveContext bodiesResolveContext;
	@Nullable
	private final InjectorForTopDownAnalyzerBasic injector;

	private AnalyzeExhaust(@NotNull BindingContext bindingContext, @NotNull BodiesResolveContext bodiesResolveContext, @Nullable Throwable error, InjectorForTopDownAnalyzerBasic analyzerBasic)
	{
		this.bindingContext = bindingContext;
		this.error = error;
		this.bodiesResolveContext = bodiesResolveContext;
		this.injector = analyzerBasic;
	}

	public static AnalyzeExhaust success(@NotNull BindingContext bindingContext, @NotNull BodiesResolveContext bodiesResolveContext, InjectorForTopDownAnalyzerBasic analyzerBasic)
	{
		return new AnalyzeExhaust(bindingContext, bodiesResolveContext, null, analyzerBasic);
	}

	public static AnalyzeExhaust error(@NotNull BindingContext bindingContext, @NotNull BodiesResolveContext bodiesResolveContext, @NotNull Throwable error)
	{
		return new AnalyzeExhaust(bindingContext, bodiesResolveContext, error, null);
	}

	@NotNull
	public BodiesResolveContext getBodiesResolveContext()
	{
		return bodiesResolveContext;
	}

	@NotNull
	public BindingContext getBindingContext()
	{
		return bindingContext;
	}

	@Nullable
	public Throwable getError()
	{
		return error;
	}

	public boolean isError()
	{
		return error != null;
	}

	public void throwIfError()
	{
		if(isError())
		{
			throw new IllegalStateException("failed to analyze: " + error, error);
		}
	}

	@Nullable
	public InjectorForTopDownAnalyzerBasic getInjector()
	{
		return injector;
	}
}
