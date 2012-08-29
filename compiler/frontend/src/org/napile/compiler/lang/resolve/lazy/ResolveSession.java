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

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.di.InjectorForLazyResolve;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ClassifierDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.ModuleDescriptor;
import org.napile.compiler.lang.descriptors.NamespaceDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BindingTraceContext;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lang.resolve.name.FqNameUnsafe;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;

/**
 * @author abreslav
 */
public class ResolveSession
{
	private static final Function<FqName, Name> NO_ALIASES = new Function<FqName, Name>()
	{

		@Override
		public Name fun(FqName name)
		{
			return null;
		}
	};

	private final ModuleDescriptor module;
	private final LazyPackageDescriptor rootPackage;

	private final BindingTrace trace = new BindingTraceContext();
	private final DeclarationProviderFactory declarationProviderFactory;

	private final Predicate<FqNameUnsafe> specialClasses;


	private final InjectorForLazyResolve injector;

	private final Map<NapileEnumEntry, ClassDescriptor> enumEntryClassDescriptorCache = Maps.newHashMap();
	private final Function<FqName, Name> classifierAliases;

	public ResolveSession(@NotNull Project project, @NotNull ModuleDescriptor rootDescriptor, @NotNull DeclarationProviderFactory declarationProviderFactory)
	{
		this(project, rootDescriptor, declarationProviderFactory, NO_ALIASES, Predicates.<FqNameUnsafe>alwaysFalse());
	}

	@Deprecated // Internal use only
	public ResolveSession(@NotNull Project project, @NotNull ModuleDescriptor rootDescriptor, @NotNull DeclarationProviderFactory declarationProviderFactory, @NotNull Function<FqName, Name> classifierAliases, @NotNull Predicate<FqNameUnsafe> specialClasses)
	{
		this.classifierAliases = classifierAliases;
		this.specialClasses = specialClasses;
		this.injector = new InjectorForLazyResolve(project, this, trace);
		this.module = rootDescriptor;
		PackageMemberDeclarationProvider provider = declarationProviderFactory.getPackageMemberDeclarationProvider(FqName.ROOT);
		assert provider != null : "No declaration provider for root package in " + rootDescriptor;
		this.rootPackage = new LazyPackageDescriptor(rootDescriptor, FqNameUnsafe.ROOT_NAME, this, provider);
		rootDescriptor.setRootNamespace(rootPackage);

		this.declarationProviderFactory = declarationProviderFactory;
	}

	@NotNull
    /*package*/ InjectorForLazyResolve getInjector()
	{
		return injector;
	}

	/*package*/ boolean isClassSpecial(@NotNull FqNameUnsafe fqName)
	{
		return specialClasses.apply(fqName);
	}

	@Nullable
	public NamespaceDescriptor getPackageDescriptor(@NotNull Name shortName)
	{
		return rootPackage.getMemberScope().getNamespace(shortName);
	}

	@Nullable
	public NamespaceDescriptor getPackageDescriptorByFqName(FqName fqName)
	{
		if(fqName.isRoot())
		{
			return rootPackage;
		}
		List<Name> names = fqName.pathSegments();
		NamespaceDescriptor current = getPackageDescriptor(names.get(0));
		if(current == null)
			return null;
		for(Name name : names.subList(1, names.size()))
		{
			current = current.getMemberScope().getNamespace(name);
			if(current == null)
				return null;
		}
		return current;
	}

	@NotNull
	public ClassDescriptor getClassDescriptor(@NotNull NapileLikeClass classOrObject)
	{
		if(classOrObject instanceof NapileEnumEntry)
		{
			NapileEnumEntry enumEntry = (NapileEnumEntry) classOrObject;
			//return getEnumEntryClassDescriptor(enumEntry);
		}

		JetScope resolutionScope = getInjector().getScopeProvider().getResolutionScopeForDeclaration((NapileDeclaration) classOrObject);
		Name name = classOrObject.getNameAsName();
		assert name != null : "Name is null for " + classOrObject + " " + classOrObject.getText();
		ClassifierDescriptor classifier = resolutionScope.getClassifier(name);
		if(classifier == null)
		{
			throw new IllegalArgumentException("Could not find a classifier for " + classOrObject + " " + classOrObject.getText());
		}
		return (ClassDescriptor) classifier;
	}

	/*@NotNull
	private ClassDescriptor getEnumEntryClassDescriptor(@NotNull NapileEnumEntry jetEnumEntry)
	{
		ClassDescriptor classDescriptor = enumEntryClassDescriptorCache.get(jetEnumEntry);
		if(classDescriptor != null)
		{
			return classDescriptor;
		}

		DeclarationDescriptor containingDeclaration = getInjector().getScopeProvider().getResolutionScopeForDeclaration(jetEnumEntry).getContainingDeclaration();
		LazyClassDescriptor newClassDescriptor = new LazyClassDescriptor(this, containingDeclaration, jetEnumEntry.getNameAsName(), JetClassInfoUtil.createClassLikeInfo(jetEnumEntry), false);
		enumEntryClassDescriptorCache.put(jetEnumEntry, newClassDescriptor);
		return newClassDescriptor;
	}    */

	/* LazyClassDescriptor getClassObjectDescriptor(NapileClassObject classObject)
	{
		LazyClassDescriptor classDescriptor = (LazyClassDescriptor) getClassDescriptor(PsiTreeUtil.getParentOfType(classObject, NapileClass.class));
		LazyClassDescriptor classObjectDescriptor = (LazyClassDescriptor) classDescriptor.getClassObjectDescriptor();
		assert classObjectDescriptor != null : "Class object is declared, but is null for " + classDescriptor;
		return classObjectDescriptor;
	}            */

	@NotNull
	public BindingContext getBindingContext()
	{
		return trace.getBindingContext();
	}

	@NotNull
    /*package*/ BindingTrace getTrace()
	{
		return trace;
	}

	@NotNull
	public DeclarationProviderFactory getDeclarationProviderFactory()
	{
		return declarationProviderFactory;
	}

	@NotNull
	public JetScope getResolutionScope(PsiElement element)
	{
		PsiElement parent = element.getParent();
		if(parent instanceof NapileFile)
		{
			NapileFile file = (NapileFile) parent;
			return getInjector().getScopeProvider().getFileScopeForDeclarationResolution(file);
		}

		if(parent instanceof NapileClassBody)
		{
			return getEnclosingLazyClass(element).getScopeForMemberDeclarationResolution();
		}

		throw new IllegalArgumentException("Unsupported PSI element: " + element);
	}

	private LazyClassDescriptor getEnclosingLazyClass(PsiElement element)
	{
		NapileLikeClass classOrObject = PsiTreeUtil.getParentOfType(element.getParent(), NapileLikeClass.class);
		assert classOrObject != null : "Called for an element that is not a class member: " + element;
		ClassDescriptor classDescriptor = getClassDescriptor(classOrObject);
		assert classDescriptor instanceof LazyClassDescriptor : "Trying to resolve a member of a non-lazily loaded class: " + element;
		return (LazyClassDescriptor) classDescriptor;
	}

	@NotNull
	public DeclarationDescriptor resolveToDescriptor(NapileDeclaration declaration)
	{
		DeclarationDescriptor result = declaration.accept(new NapileVisitor<DeclarationDescriptor, Void>()
		{
			@Override
			public DeclarationDescriptor visitClass(NapileClass klass, Void data)
			{
				return getClassDescriptor(klass);
			}

			@Override
			public DeclarationDescriptor visitObjectDeclaration(NapileAnonymousClass declaration, Void data)
			{
				PsiElement parent = declaration.getParent();

				return getClassDescriptor(declaration);
			}

			@Override
			public DeclarationDescriptor visitTypeParameter(NapileTypeParameter parameter, Void data)
			{
				NapileTypeParameterListOwner ownerElement = PsiTreeUtil.getParentOfType(parameter, NapileTypeParameterListOwner.class);
				DeclarationDescriptor ownerDescriptor = resolveToDescriptor(ownerElement);

				List<TypeParameterDescriptor> typeParameters;
				Name name = parameter.getNameAsName();
				if(ownerDescriptor instanceof CallableDescriptor)
				{
					CallableDescriptor callableDescriptor = (CallableDescriptor) ownerDescriptor;
					typeParameters = callableDescriptor.getTypeParameters();
				}
				else if(ownerDescriptor instanceof ClassDescriptor)
				{
					ClassDescriptor classDescriptor = (ClassDescriptor) ownerDescriptor;
					typeParameters = classDescriptor.getTypeConstructor().getParameters();
				}
				else
				{
					throw new IllegalStateException("Unknown owner kind for a type parameter: " + ownerDescriptor);
				}

				for(TypeParameterDescriptor typeParameterDescriptor : typeParameters)
				{
					if(typeParameterDescriptor.getName().equals(name))
					{
						return typeParameterDescriptor;
					}
				}

				throw new IllegalStateException("Type parameter " + name + " not found for " + ownerDescriptor);
			}

			@Override
			public DeclarationDescriptor visitNamedFunction(NapileNamedFunction function, Void data)
			{
				JetScope scopeForDeclaration = getInjector().getScopeProvider().getResolutionScopeForDeclaration(function);
				scopeForDeclaration.getFunctions(function.getNameAsName());
				return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, function);
			}

			@Override
			public DeclarationDescriptor visitProperty(NapileProperty property, Void data)
			{
				JetScope scopeForDeclaration = getInjector().getScopeProvider().getResolutionScopeForDeclaration(property);
				scopeForDeclaration.getProperties(property.getNameAsName());
				return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, property);
			}

			@Override
			public DeclarationDescriptor visitObjectDeclarationName(NapileObjectDeclarationName declarationName, Void data)
			{
				JetScope scopeForDeclaration = getInjector().getScopeProvider().getResolutionScopeForDeclaration((NapileDeclaration) declarationName.getParent());
				scopeForDeclaration.getProperties(declarationName.getNameAsName());
				return getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, declarationName);
			}

			@Override
			public DeclarationDescriptor visitJetElement(NapileElement element, Void data)
			{
				throw new IllegalArgumentException("Unsupported declaration type: " + element + " " + element.getText());
			}
		}, null);
		if(result == null)
		{
			throw new IllegalStateException("No descriptor resolved for " + declaration + " " + declaration.getText());
		}
		return result;
	}

	@NotNull
    /*package*/ Name resolveClassifierAlias(@NotNull FqName packageName, @NotNull Name alias)
	{
		// TODO: creating a new FqName object every time...
		Name actualName = classifierAliases.fun(packageName.child(alias));
		if(actualName == null)
		{
			return alias;
		}
		return actualName;
	}
}
