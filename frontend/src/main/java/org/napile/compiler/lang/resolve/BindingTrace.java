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

package org.napile.compiler.lang.resolve;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.lang.diagnostics.DiagnosticHolder;
import org.napile.compiler.util.slicedmap.ReadOnlySlice;
import org.napile.compiler.util.slicedmap.WritableSlice;

/**
 * @author abreslav
 */
public interface BindingTrace extends DiagnosticHolder
{
	BindingTrace EMPTY = new BindingTrace()
	{
		@Override
		public <K, V> void record(WritableSlice<K, V> slice, K key, V value)
		{
		}

		@Override
		public <K> void record(WritableSlice<K, Boolean> slice, K key)
		{
		}

		@Nullable
		@Override
		public BindingTrace getParent()
		{
			return null;
		}

		@Nullable
		@Override
		public <K, V> V get(ReadOnlySlice<K, V> slice, K key)
		{
			return null;
		}

		@NotNull
		@Override
		public <K, V> V safeGet(ReadOnlySlice<K, V> slice, K key)
		{
			throw new IllegalArgumentException();
		}

		@NotNull
		@Override
		public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice)
		{
			return Collections.emptyList();
		}

		@Override
		public void report(@NotNull Diagnostic diagnostic)
		{
		}

		@NotNull
		@Override
		public List<Diagnostic> getDiagnostics()
		{
			return Collections.emptyList();
		}
	};

	<K, V> void record(WritableSlice<K, V> slice, K key, V value);

	// Writes TRUE for a boolean value
	<K> void record(WritableSlice<K, Boolean> slice, K key);

	@Nullable
	BindingTrace getParent();

	@Nullable
	<K, V> V get(ReadOnlySlice<K, V> slice, K key);

	@NotNull
	<K, V> V safeGet(ReadOnlySlice<K, V> slice, K key);

	// slice.isCollective() must be true
	@NotNull
	<K, V> Collection<K> getKeys(WritableSlice<K, V> slice);
}
