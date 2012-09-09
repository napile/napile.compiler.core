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

package org.napile.compiler.lang.resolve.lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.ClassDescriptorBase;
import org.napile.compiler.lang.descriptors.ClassKind;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.Modality;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.Visibility;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.psi.NapileLikeClass;
import org.napile.compiler.lang.psi.NapileEnumEntry;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.NapileTypeParameter;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.lazy.data.FilteringClassLikeInfo;
import org.napile.compiler.lang.resolve.lazy.data.NapileClassLikeInfo;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.resolve.processors.AnnotationResolver;
import org.napile.compiler.lang.resolve.processors.DescriptorResolver;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.RedeclarationHandler;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.lang.resolve.scopes.WritableScopeImpl;
import org.napile.compiler.lang.resolve.scopes.receivers.ClassReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeConstructor;
import org.napile.compiler.lang.types.TypeUtils;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * @author abreslav
 */
public class LazyClassDescriptor extends ClassDescriptorBase
{

	private static final Predicate<Object> ONLY_ENUM_ENTIRES = Predicates.instanceOf(NapileEnumEntry.class);
	private static final Predicate<JetType> VALID_SUPERTYPE = new Predicate<JetType>()
	{
		@Override
		public boolean apply(JetType type)
		{
			return !ErrorUtils.isErrorType(type) && (TypeUtils.getClassDescriptor(type) != null);
		}
	};
	private final ResolveSession resolveSession;
	private final NapileClassLikeInfo originalClassInfo;
	private final ClassMemberDeclarationProvider declarationProvider;

	private final Name name;
	private final DeclarationDescriptor containingDeclaration;
	private final TypeConstructor typeConstructor;
	private final Modality modality;
	private final Visibility visibility;
	private final ClassKind kind;

	private ClassReceiver implicitReceiver;
	private List<AnnotationDescriptor> annotations;

	private final LazyClassMemberScope unsubstitutedMemberScope;

	private final LazyClassMemberScope staticScope;

	private JetScope scopeForClassHeaderResolution;
	private JetScope scopeForMemberDeclarationResolution;


	public LazyClassDescriptor(@NotNull ResolveSession resolveSession, @NotNull DeclarationDescriptor containingDeclaration, @NotNull Name name, @NotNull NapileClassLikeInfo classLikeInfo, boolean isStatic)
	{
		super(isStatic);
		this.resolveSession = resolveSession;

		if(classLikeInfo.getCorrespondingClassOrObject() != null)
		{
			this.resolveSession.getTrace().record(BindingContext.CLASS, classLikeInfo.getCorrespondingClassOrObject(), this);
		}

		this.originalClassInfo = classLikeInfo;
		NapileClassLikeInfo classLikeInfoForMembers = classLikeInfo.getClassKind() != ClassKind.ENUM_CLASS ? classLikeInfo : noEnumEntries(classLikeInfo);
		this.declarationProvider = resolveSession.getDeclarationProviderFactory().getClassMemberDeclarationProvider(classLikeInfoForMembers);
		this.name = name;
		this.containingDeclaration = containingDeclaration;
		this.unsubstitutedMemberScope = new LazyClassMemberScope(resolveSession, declarationProvider, this);
		this.staticScope = new LazyClassMemberScope(resolveSession, declarationProvider, this);

		this.typeConstructor = new LazyClassTypeConstructor();

		this.kind = classLikeInfo.getClassKind();
		if(kind == ClassKind.ANONYM_CLASS)
		{
			this.modality = Modality.FINAL;
		}
		else
		{
			NapileModifierList modifierList = classLikeInfo.getModifierList();
			this.modality = DescriptorResolver.resolveModalityFromModifiers(modifierList, Modality.OPEN);
		}
		NapileModifierList modifierList = classLikeInfo.getModifierList();
		this.visibility = DescriptorResolver.resolveVisibilityFromModifiers(modifierList);
	}

	@Override
	protected JetScope getScopeForMemberLookup()
	{
		return unsubstitutedMemberScope;
	}

	@NotNull
	@Override
	public JetScope getStaticOuterScope()
	{
		return staticScope;
	}

	@NotNull
	public JetScope getScopeForClassHeaderResolution()
	{
		if(scopeForClassHeaderResolution == null)
		{
			WritableScopeImpl scope = new WritableScopeImpl(resolveSession.getResolutionScope(declarationProvider.getOwnerInfo().getScopeAnchor()), this, RedeclarationHandler.DO_NOTHING, "Class Header Resolution");
			for(TypeParameterDescriptor typeParameterDescriptor : getTypeConstructor().getParameters())
			{
				scope.addClassifierDescriptor(typeParameterDescriptor);
			}
			scope.changeLockLevel(WritableScope.LockLevel.READING);
			scopeForClassHeaderResolution = scope;
		}
		return scopeForClassHeaderResolution;
	}

	public JetScope getScopeForMemberDeclarationResolution()
	{
		if(scopeForMemberDeclarationResolution == null)
		{
			WritableScopeImpl scope = new WritableScopeImpl(getScopeForMemberLookup(), this, RedeclarationHandler.DO_NOTHING, "Member Declaration Resolution");

			scope.importScope(getScopeForClassHeaderResolution());

			scope.changeLockLevel(WritableScope.LockLevel.READING);
			scopeForMemberDeclarationResolution = scope;
		}
		return scopeForMemberDeclarationResolution;
	}

	@NotNull
	@Override
	public Set<ConstructorDescriptor> getConstructors()
	{
		return unsubstitutedMemberScope.getConstructors();
	}

	@NotNull
	@Override
	public DeclarationDescriptor getOriginal()
	{
		return this;
	}

	@NotNull
	@Override
	public DeclarationDescriptor getContainingDeclaration()
	{
		return containingDeclaration;
	}

	@NotNull
	@Override
	public TypeConstructor getTypeConstructor()
	{
		return typeConstructor;
	}

	@NotNull
	@Override
	public Collection<JetType> getSupertypes()
	{
		return Collections.<JetType>emptyList();
	}

	@NotNull
	@Override
	public ClassKind getKind()
	{
		return kind;
	}

	@NotNull
	@Override
	public Modality getModality()
	{
		return modality;
	}

	@NotNull
	@Override
	public Visibility getVisibility()
	{
		return visibility;
	}

	@NotNull
	@Override
	public ReceiverDescriptor getImplicitReceiver()
	{
		if(implicitReceiver == null)
		{
			implicitReceiver = new ClassReceiver(this);
		}
		return implicitReceiver;
	}

	@Override
	public List<AnnotationDescriptor> getAnnotations()
	{
		if(annotations == null)
		{
			NapileClassLikeInfo classInfo = declarationProvider.getOwnerInfo();
			NapileModifierList modifierList = classInfo.getModifierList();
			if(modifierList != null)
			{
				AnnotationResolver annotationResolver = resolveSession.getInjector().getAnnotationResolver();
				annotations = annotationResolver.resolveAnnotations(resolveSession.getResolutionScope(classInfo.getScopeAnchor()), modifierList, resolveSession.getTrace());
			}
			else
			{
				annotations = Collections.emptyList();
			}
		}
		return annotations;
	}

	@NotNull
	@Override
	public Name getName()
	{
		return name;
	}

	@Override
	public String toString()
	{
		return "lazy class " + getName().toString();
	}

	private class LazyClassTypeConstructor implements TypeConstructor
	{
		private Collection<JetType> supertypes = null;
		private List<TypeParameterDescriptor> parameters = null;

		@NotNull
		@Override
		public List<TypeParameterDescriptor> getParameters()
		{
			if(parameters == null)
			{
				NapileClassLikeInfo classInfo = declarationProvider.getOwnerInfo();
				List<NapileTypeParameter> typeParameters = classInfo.getTypeParameters();
				parameters = new ArrayList<TypeParameterDescriptor>(typeParameters.size());

				for(int i = 0; i < typeParameters.size(); i++)
				{
					parameters.add(new LazyTypeParameterDescriptor(resolveSession, LazyClassDescriptor.this, typeParameters.get(i), i));
				}
			}
			return parameters;
		}

		@NotNull
		@Override
		public Collection<? extends JetType> getSupertypes()
		{
			if(supertypes == null)
			{
				if(resolveSession.isClassSpecial(DescriptorUtils.getFQName(LazyClassDescriptor.this)))
				{
					this.supertypes = Collections.emptyList();
				}
				else
				{
					NapileLikeClass classOrObject = declarationProvider.getOwnerInfo().getCorrespondingClassOrObject();
					if(classOrObject == null)
					{
						this.supertypes = Collections.emptyList();
					}
					else
					{
						List<JetType> allSupertypes = resolveSession.getInjector().getDescriptorResolver().resolveSupertypes(getScopeForClassHeaderResolution(), classOrObject, resolveSession.getTrace());
						List<JetType> validSupertypes = Lists.newArrayList(Collections2.filter(allSupertypes, VALID_SUPERTYPE));
						this.supertypes = validSupertypes;
						findAndDisconnectLoopsInTypeHierarchy(validSupertypes);
					}
				}
			}
			return supertypes;
		}

		private void findAndDisconnectLoopsInTypeHierarchy(List<JetType> supertypes)
		{
			for(Iterator<JetType> iterator = supertypes.iterator(); iterator.hasNext(); )
			{
				JetType supertype = iterator.next();
				if(isReachable(supertype.getConstructor(), this, new HashSet<TypeConstructor>()))
				{
					iterator.remove();
				}
			}
		}

		private boolean isReachable(TypeConstructor from, TypeConstructor to, Set<TypeConstructor> visited)
		{
			if(!visited.add(from))
				return false;
			for(JetType supertype : from.getSupertypes())
			{
				TypeConstructor supertypeConstructor = supertype.getConstructor();
				if(supertypeConstructor == to)
				{
					return true;
				}
				if(isReachable(supertypeConstructor, to, visited))
				{
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean isSealed()
		{
			return !getModality().isOverridable();
		}

		@Override
		public ClassifierDescriptor getDeclarationDescriptor()
		{
			return LazyClassDescriptor.this;
		}

		@Override
		public List<AnnotationDescriptor> getAnnotations()
		{
			return Collections.emptyList(); // TODO
		}

		@Override
		public String toString()
		{
			return LazyClassDescriptor.this.getName().toString();
		}
	}

	private static NapileClassLikeInfo noEnumEntries(NapileClassLikeInfo classLikeInfo)
	{
		return new FilteringClassLikeInfo(classLikeInfo, Predicates.not(ONLY_ENUM_ENTIRES));
	}

	private static NapileClassLikeInfo onlyEnumEntries(NapileClassLikeInfo classLikeInfo)
	{
		return new FilteringClassLikeInfo(classLikeInfo, ONLY_ENUM_ENTIRES)
		{
			@Override
			public NapileLikeClass getCorrespondingClassOrObject()
			{
				return null;
			}

			@NotNull
			@Override
			public ClassKind getClassKind()
			{
				return ClassKind.ANONYM_CLASS;
			}

			@NotNull
			@Override
			public List<NapileTypeParameter> getTypeParameters()
			{
				return Collections.emptyList();
			}
		};
	}
}
