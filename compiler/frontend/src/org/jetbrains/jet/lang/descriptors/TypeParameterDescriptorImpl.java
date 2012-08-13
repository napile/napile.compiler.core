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

package org.jetbrains.jet.lang.descriptors;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.LazyScopeAdapter;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeImpl;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeConstructorImpl;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.rt.NapileLangPackage;
import org.jetbrains.jet.resolve.DescriptorRenderer;
import org.jetbrains.jet.util.lazy.LazyValue;
import com.google.common.collect.Sets;

/**
 * @author abreslav
 */
public class TypeParameterDescriptorImpl extends DeclarationDescriptorNonRootImpl implements TypeParameterDescriptor
{
	public static TypeParameterDescriptor createWithDefaultBound(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, boolean reified, @NotNull Variance variance, @NotNull Name name, int index)
	{
		TypeParameterDescriptorImpl typeParameterDescriptor = createForFurtherModification(containingDeclaration, annotations, reified, variance, name, index);
		typeParameterDescriptor.addUpperBound(JetStandardClasses.getDefaultBound());
		typeParameterDescriptor.setInitialized();
		return typeParameterDescriptor;
	}

	public static TypeParameterDescriptorImpl createForFurtherModification(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, boolean reified, @NotNull Variance variance, @NotNull Name name, int index)
	{
		return new TypeParameterDescriptorImpl(containingDeclaration, annotations, reified, variance, name, index);
	}

	// 0-based
	private final int index;
	private final Variance variance;
	private final Set<JetType> upperBounds;
	private JetType upperBoundsAsType;
	private final TypeConstructor typeConstructor;
	private JetType defaultType;
	private final Set<JetType> classObjectUpperBounds = Sets.newLinkedHashSet();
	private JetType classObjectBoundsAsType;

	private final boolean reified;

	private boolean initialized = false;

	private TypeParameterDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, boolean reified, @NotNull Variance variance, @NotNull Name name, int index)
	{
		super(containingDeclaration, annotations, name);
		this.index = index;
		this.variance = variance;
		this.upperBounds = Sets.newLinkedHashSet();
		this.reified = reified;
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

	@Override
	public boolean isReified()
	{
		checkInitialized();
		return reified;
	}

	@Override
	public Variance getVariance()
	{
		checkInitialized();
		return variance;
	}

	public void addUpperBound(@NotNull JetType bound)
	{
		checkUninitialized();
		doAddUpperBound(bound);
	}

	private void doAddUpperBound(JetType bound)
	{
		upperBounds.add(bound); // TODO : Duplicates?
	}

	public void addDefaultUpperBound(JetScope jetScope)
	{
		checkUninitialized();

		if(upperBounds.isEmpty())
			doAddUpperBound(NapileLangPackage.ANY.getTypeSafe(jetScope, true));
	}

	@Override
	@NotNull
	public Set<JetType> getUpperBounds()
	{
		checkInitialized();
		return upperBounds;
	}

	@Override
	@NotNull
	public JetType getUpperBoundsAsType()
	{
		checkInitialized();
		if(upperBoundsAsType == null)
		{
			assert upperBounds != null : "Upper bound list is null in " + getName();
			assert upperBounds.size() > 0 : "Upper bound list is empty in " + getName();

			final JetScope jetScope = TypeUtils.getChainedScope(upperBounds);

			upperBoundsAsType = TypeUtils.intersect(JetTypeChecker.INSTANCE, upperBounds, jetScope);
			if(upperBoundsAsType == null)
				upperBoundsAsType = NapileLangPackage.NULL.getTypeSafe(jetScope, false);
		}
		return upperBoundsAsType;
	}

	@Override
	@NotNull
	public Set<JetType> getLowerBounds()
	{
		//checkInitialized();
		return Collections.singleton(NapileLangPackage.NULL.getTypeSafe(TypeUtils.getChainedScope(upperBounds), false));
	}

	@Override
	@NotNull
	public JetType getLowerBoundsAsType()
	{
		checkInitialized();
		return NapileLangPackage.NULL.getTypeSafe(TypeUtils.getChainedScope(upperBounds), false);
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
	public JetType getDefaultType()
	{
		//checkInitialized();
		if(defaultType == null)
		{
			defaultType = new JetTypeImpl(Collections.<AnnotationDescriptor>emptyList(), getTypeConstructor(), TypeUtils.hasNullableLowerBound(this), Collections.<TypeProjection>emptyList(), new LazyScopeAdapter(new LazyValue<JetScope>()
			{
				@Override
				protected JetScope compute()
				{
					return getUpperBoundsAsType().getMemberScope();
				}
			}));
		}
		return defaultType;
	}

	@Override
	public JetType getClassObjectType()
	{
		checkInitialized();
		if(classObjectUpperBounds.isEmpty())
			return null;

		if(classObjectBoundsAsType == null)
		{
			final JetScope jetScope = TypeUtils.getChainedScope(classObjectUpperBounds);
			classObjectBoundsAsType = TypeUtils.intersect(JetTypeChecker.INSTANCE, classObjectUpperBounds, jetScope);
			if(classObjectBoundsAsType == null)
				classObjectBoundsAsType = NapileLangPackage.NULL.getTypeSafe(jetScope, false);
		}
		return classObjectBoundsAsType;
	}

	@Override
	public boolean isClassObjectAValue()
	{
		return true;
	}

	public void addClassObjectBound(@NotNull JetType bound)
	{
		checkUninitialized();
		classObjectUpperBounds.add(bound); // TODO : Duplicates?
	}

	@Override
	public int getIndex()
	{
		checkInitialized();
		return index;
	}
}
