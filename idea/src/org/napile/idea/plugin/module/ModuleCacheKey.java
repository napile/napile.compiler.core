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

package org.napile.idea.plugin.module;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.reference.SoftReference;
import com.intellij.util.Function;

/**
 * Some variant of {@link com.intellij.psi.util.PsiCacheKey}
 */
public class ModuleCacheKey<T> extends Key<SoftReference<Pair<Long, T>>>
{
	private final Function<Module, T> myFunction;

	private ModuleCacheKey(@NonNls @NotNull String name, @NotNull Function<Module, T> function)
	{
		super(name);
		myFunction = function;
	}

	public final T getValue(@NotNull Module h)
	{
		T result = getCachedValueOrNull(h);
		if(result != null)
		{
			return result;
		}

		result = myFunction.fun(h);
		PsiModificationTracker tracker = PsiModificationTracker.SERVICE.getInstance(h.getProject());
		final long count = tracker.getModificationCount();
		h.putUserData(this, new SoftReference<Pair<Long, T>>(new Pair<Long, T>(count, result)));
		return result;
	}

	@Nullable
	public final T getCachedValueOrNull(@NotNull Module h)
	{
		PsiModificationTracker tracker = PsiModificationTracker.SERVICE.getInstance(h.getProject());

		SoftReference<Pair<Long, T>> ref = h.getUserData(this);
		Pair<Long, T> data = ref == null ? null : ref.get();
		if(data == null || data.getFirst() != tracker.getModificationCount())
		{
			return null;
		}

		return data.getSecond();
	}

	public static <T> ModuleCacheKey<T> create(@NonNls @NotNull String name, @NotNull Function<Module, T> function)
	{
		return new ModuleCacheKey<T>(name, function);
	}
}
