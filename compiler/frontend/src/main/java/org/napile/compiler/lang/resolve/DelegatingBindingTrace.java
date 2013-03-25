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
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.diagnostics.Diagnostic;
import org.napile.compiler.util.slicedmap.MutableSlicedMap;
import org.napile.compiler.util.slicedmap.ReadOnlySlice;
import org.napile.compiler.util.slicedmap.SlicedMapImpl;
import org.napile.compiler.util.slicedmap.SlicedMapKey;
import org.napile.compiler.util.slicedmap.Slices;
import org.napile.compiler.util.slicedmap.WritableSlice;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

/**
 * @author abreslav
 */
public class DelegatingBindingTrace implements BindingTrace
{
	private final BindingTrace parentTrace;
	private final MutableSlicedMap map = SlicedMapImpl.create();
	private final List<Diagnostic> diagnostics = Lists.newArrayList();

	public DelegatingBindingTrace(BindingTrace parentTrace)
	{
		this.parentTrace = parentTrace;
	}

	@Override
	public <K, V> void record(WritableSlice<K, V> slice, K key, V value)
	{
		map.put(slice, key, value);
	}

	@Override
	public <K> void record(WritableSlice<K, Boolean> slice, K key)
	{
		record(slice, key, true);
	}

	@Nullable
	@Override
	public BindingTrace getParent()
	{
		return parentTrace;
	}

	@Override
	public <K, V> V get(ReadOnlySlice<K, V> slice, K key)
	{
		V value = map.get(slice, key);
		if(slice instanceof Slices.SetSlice)
		{
			assert value != null;
			if(value.equals(true))
				return value;
		}
		else if(value != null)
		{
			return value;
		}

		return parentTrace.get(slice, key);
	}

	@Override
	@NotNull
	public <K, V> V safeGet(ReadOnlySlice<K, V> slice, K key)
	{
		return get(slice, key);
	}

	@NotNull
	@Override
	public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice)
	{
		Collection<K> keys = map.getKeys(slice);
		Collection<K> fromParent = parentTrace.getKeys(slice);
		if(keys.isEmpty())
			return fromParent;
		if(fromParent.isEmpty())
			return keys;

		List<K> result = Lists.newArrayList(keys);
		result.addAll(fromParent);
		return result;
	}

	public void addAllMyDataTo(@NotNull BindingTrace trace)
	{
		addAllMyDataTo(trace, null, true);
	}

	public void addAllMyDataTo(@NotNull BindingTrace trace, @Nullable Predicate<WritableSlice> filter, boolean commitDiagnostics)
	{
		for(Map.Entry<SlicedMapKey<?, ?>, ?> entry : map)
		{
			SlicedMapKey slicedMapKey = entry.getKey();
			Object value = entry.getValue();

			WritableSlice slice = slicedMapKey.getSlice();
			if(filter == null || filter.apply(slice))
			{
				//noinspection unchecked
				trace.record(slice, slicedMapKey.getKey(), value);
			}
		}

		if(!commitDiagnostics)
			return;

		for(Diagnostic diagnostic : diagnostics)
		{
			trace.report(diagnostic);
		}
	}

	public void clear()
	{
		map.clear();
		diagnostics.clear();
	}

	@Override
	public void report(@NotNull Diagnostic diagnostic)
	{
		diagnostics.add(diagnostic);
	}

	@NotNull
	@Override
	public List<Diagnostic> getDiagnostics()
	{
		return diagnostics;
	}
}
