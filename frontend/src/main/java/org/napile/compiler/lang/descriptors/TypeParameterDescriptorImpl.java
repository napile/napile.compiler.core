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

package org.napile.compiler.lang.descriptors;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.resolve.scopes.LazyScopeAdapter;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeSubstitutor;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.lang.types.checker.NapileTypeChecker;
import org.napile.compiler.lang.types.impl.NapileTypeImpl;
import org.napile.compiler.lang.types.impl.TypeConstructorImpl;
import org.napile.compiler.render.DescriptorRenderer;
import org.napile.compiler.util.lazy.LazyValue;
import com.google.common.collect.Sets;

/**
 * @author abreslav
 */
public class TypeParameterDescriptorImpl extends DeclarationDescriptorNonRootImpl implements TypeParameterDescriptor
{
	// 0-based
	private final int index;

	private Set<ConstructorDescriptor> constructorDescriptors = new HashSet<ConstructorDescriptor>(0);

	private final Set<NapileType> upperBounds;
	private NapileType upperBoundsAsType;
	private final TypeConstructor typeConstructor;
	private NapileType defaultType;

	private boolean initialized = false;

	public TypeParameterDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull Name name, int index)
	{
		super(containingDeclaration, annotations, name);
		this.index = index;
		this.upperBounds = Sets.newLinkedHashSet();

		// TODO: Should we actually pass the annotations on to the type constructor?
		this.typeConstructor = new TypeConstructorImpl(this, annotations, false, name.getName(), Collections.<TypeParameterDescriptor>emptyList(), upperBounds);
	}

	private void checkInitialized()
	{
		if(!initialized)
		{
			throw new IllegalStateException("Type parameter descriptor in not initialized: " + nameForAssertions());
		}
	}

	private void checkUninitialized()
	{
		if(initialized)
		{
			throw new IllegalStateException("Type parameter descriptor is already initialized: " + nameForAssertions());
		}
	}

	private String nameForAssertions()
	{
		DeclarationDescriptor owner = getContainingDeclaration();
		return getName() + " declared in " + (owner == null ? "<no owner>" : owner.getName());
	}

	public void setInitialized()
	{
		checkUninitialized();
		initialized = true;
	}

	public void addConstructor(@NotNull ConstructorDescriptor constructorDescriptor)
	{
		constructorDescriptors.add(constructorDescriptor);

		constructorDescriptor.setReturnType(getDefaultType());
	}

	public void addUpperBound(@NotNull NapileType bound)
	{
		checkUninitialized();
		upperBounds.add(bound);
	}

	@Deprecated
	public void addDefaultUpperBound(NapileScope napileScope)
	{
		checkUninitialized();

		if(upperBounds.isEmpty())
			upperBounds.add(TypeUtils.getTypeOfClassOrErrorType(napileScope, NapileLangPackage.ANY, true));
	}

	@Override
	@NotNull
	public Set<NapileType> getUpperBounds()
	{
		checkInitialized();
		return upperBounds;
	}

	@Override
	@NotNull
	public NapileType getUpperBoundsAsType()
	{
		checkInitialized();
		if(upperBoundsAsType == null)
		{
			assert upperBounds != null : "Upper bound list is null in " + getName();
			assert upperBounds.size() > 0 : "Upper bound list is empty in " + getName();

			final NapileScope napileScope = TypeUtils.getChainedScope(upperBounds);

			upperBoundsAsType = TypeUtils.intersect(NapileTypeChecker.INSTANCE, upperBounds, napileScope);
			if(upperBoundsAsType == null)
				upperBoundsAsType = TypeUtils.getTypeOfClassOrErrorType(napileScope, NapileLangPackage.NULL, false);
		}
		return upperBoundsAsType;
	}

	@NotNull
	@Override
	public TypeConstructor getTypeConstructor()
	{
		//checkInitialized();
		return typeConstructor;
	}

	@Override
	public String toString()
	{
		try
		{
			return DescriptorRenderer.TEXT.render(this);
		}
		catch(Exception e)
		{
			return this.getClass().getName() + "@" + System.identityHashCode(this);
		}
	}

	@NotNull
	@Override
	@Deprecated
	public TypeParameterDescriptor substitute(TypeSubstitutor substitutor)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data)
	{
		checkInitialized();
		return visitor.visitTypeParameterDescriptor(this, data);
	}

	@NotNull
	@Override
	public NapileType getDefaultType()
	{
		//checkInitialized();
		if(defaultType == null)
		{
			defaultType = new NapileTypeImpl(Collections.<AnnotationDescriptor>emptyList(), getTypeConstructor(), false, Collections.<NapileType>emptyList(), new LazyScopeAdapter(new LazyValue<NapileScope>()
			{
				@Override
				protected NapileScope compute()
				{
					return getUpperBoundsAsType().getMemberScope();
				}
			}));
		}
		return defaultType;
	}

	@NotNull
	@Override
	public NapileScope getStaticOuterScope()
	{
		return NapileScope.EMPTY;
	}

	@NotNull
	@Override
	public Collection<NapileType> getSupertypes()
	{
		return Collections.emptySet();
	}

	@NotNull
	@Override
	public Set<ConstructorDescriptor> getConstructors()
	{
		return constructorDescriptors;
	}

	@Override
	public int getIndex()
	{
		checkInitialized();
		return index;
	}
}
