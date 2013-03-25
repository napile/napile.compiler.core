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

package org.napile.compiler.lang.types;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.util.Box;
import org.napile.compiler.util.lazy.LazyValue;
import org.napile.compiler.util.lazy.ReenteringLazyValueComputationException;

/**
 * @author abreslav
 */
public class DeferredType implements JetType
{

	public static DeferredType create(BindingTrace trace, LazyValue<JetType> lazyValue)
	{
		DeferredType deferredType = new DeferredType(lazyValue);
		trace.record(BindingTraceKeys.DEFERRED_TYPE, new Box<DeferredType>(deferredType));
		return deferredType;
	}

	private final LazyValue<JetType> lazyValue;

	private DeferredType(LazyValue<JetType> lazyValue)
	{
		this.lazyValue = lazyValue;
	}

	public boolean isComputed()
	{
		return lazyValue.isComputed();
	}

	public JetType getActualType()
	{
		return lazyValue.get();
	}

	@Override
	@NotNull
	public JetScope getMemberScope()
	{
		return getActualType().getMemberScope();
	}

	@Override
	@NotNull
	public TypeConstructor getConstructor()
	{
		return getActualType().getConstructor();
	}

	@Override
	@NotNull
	public List<JetType> getArguments()
	{
		return getActualType().getArguments();
	}

	@Override
	public boolean isNullable()
	{
		return getActualType().isNullable();
	}

	@Override
	public List<AnnotationDescriptor> getAnnotations()
	{
		return getActualType().getAnnotations();
	}

	@Override
	public String toString()
	{
		try
		{
			if(lazyValue.isComputed())
			{
				return getActualType().toString();
			}
			else
			{
				return "<Not computed yet>";
			}
		}
		catch(ReenteringLazyValueComputationException e)
		{
			return "<Failed to compute this type>";
		}
	}

	@Override
	public boolean equals(Object obj)
	{
		return getActualType().equals(obj);
	}

	@Override
	public <A, R> R accept(@NotNull TypeConstructorVisitor<A, R> visitor, A arg)
	{
		return getActualType().accept(visitor, arg);
	}

	@Override
	public int hashCode()
	{
		return getActualType().hashCode();
	}
}
