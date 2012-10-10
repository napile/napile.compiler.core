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

package org.napile.compiler.lang.resolve.processors;

import static org.napile.compiler.lang.diagnostics.Errors.REDECLARATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.TopDownAnalysisContext;
import org.napile.compiler.lang.resolve.processors.members.AnnotationResolver;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.impl.JetTypeImpl;
import org.napile.compiler.psi.NapileClass;
import org.napile.compiler.psi.NapileClassLike;
import org.napile.compiler.psi.NapileDeclaration;
import org.napile.compiler.psi.NapileDeclarationContainer;
import org.napile.compiler.psi.NapileFile;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.psi.PsiElement;

/**
 * @author abreslav
 */
public class DeclarationResolver
{
	@NotNull
	private AnnotationResolver annotationResolver;
	@NotNull
	private TopDownAnalysisContext context;
	@NotNull
	private ImportsResolver importsResolver;
	@NotNull
	private DescriptorResolver descriptorResolver;
	@NotNull
	private BindingTrace trace;
	@NotNull
	private TypeResolver typeResolver;


	@Inject
	public void setAnnotationResolver(@NotNull AnnotationResolver annotationResolver)
	{
		this.annotationResolver = annotationResolver;
	}

	@Inject
	public void setContext(@NotNull TopDownAnalysisContext context)
	{
		this.context = context;
	}

	@Inject
	public void setImportsResolver(@NotNull ImportsResolver importsResolver)
	{
		this.importsResolver = importsResolver;
	}

	@Inject
	public void setDescriptorResolver(@NotNull DescriptorResolver descriptorResolver)
	{
		this.descriptorResolver = descriptorResolver;
	}

	@Inject
	public void setTrace(@NotNull BindingTrace trace)
	{
		this.trace = trace;
	}

	@Inject
	public void setTypeResolver(@NotNull TypeResolver typeResolver)
	{
		this.typeResolver = typeResolver;
	}

	public void process(@NotNull JetScope rootScope)
	{
		resolveDeclarations();
		resolveAnnotations();
		importsResolver.processMembersImports(rootScope);
		checkRedeclarationsInNamespaces();
		checkRedeclarationsInInnerClassNames();
	}

	private void resolveAnnotations()
	{
		for(Map.Entry<NapileClass, MutableClassDescriptor> entry : context.getClasses().entrySet())
		{
			NapileClass napileClass = entry.getKey();
			MutableClassDescriptor descriptor = entry.getValue();
			resolveAnnotationsForClassOrObject(annotationResolver, napileClass, descriptor);
		}
		for(Map.Entry<NapileAnonymClass, MutableClassDescriptor> entry : context.getAnonymous().entrySet())
		{
			NapileAnonymClass objectDeclaration = entry.getKey();
			MutableClassDescriptor descriptor = entry.getValue();
			resolveAnnotationsForClassOrObject(annotationResolver, objectDeclaration, descriptor);
		}
	}

	private void resolveAnnotationsForClassOrObject(AnnotationResolver annotationResolver, NapileClassLike jetClass, MutableClassDescriptor descriptor)
	{
		NapileModifierList modifierList = jetClass.getModifierList();
		if(modifierList != null)
		{
			descriptor.getAnnotations().addAll(annotationResolver.resolveAnnotations(descriptor.getScopeForSupertypeResolution(), modifierList.getAnnotationEntries(), trace));
		}
	}

	private void resolveDeclarations()
	{
		for(Map.Entry<NapileClass, MutableClassDescriptor> entry : context.getClasses().entrySet())
		{
			NapileClass napileClass = entry.getKey();
			MutableClassDescriptor classDescriptor = entry.getValue();

			resolveInsideDeclarations(napileClass, classDescriptor.getScopeForMemberResolution(), classDescriptor);
		}

		for(Map.Entry<NapileAnonymClass, MutableClassDescriptor> entry : context.getAnonymous().entrySet())
		{
			NapileAnonymClass object = entry.getKey();
			MutableClassDescriptor classDescriptor = entry.getValue();

			resolveInsideDeclarations(object, classDescriptor.getScopeForMemberResolution(), classDescriptor);
		}


		for(Map.Entry<NapileEnumEntry, MutableClassDescriptor> entry : context.getEnumEntries().entrySet())
		{
			NapileEnumEntry enumEntry = entry.getKey();
			MutableClassDescriptor enumEntryDescriptor = entry.getValue();

			resolveInsideDeclarations(enumEntry, enumEntryDescriptor.getScopeForMemberResolution(), enumEntryDescriptor);
		}
	}

	private void resolveInsideDeclarations(@NotNull NapileDeclarationContainer<NapileDeclaration> declarationOwner, final @NotNull JetScope scope, final @NotNull MutableClassDescriptor ownerDescription)
	{
		for(NapileDeclaration declaration : declarationOwner.getDeclarations())
		{
			declaration.accept(new NapileVisitorVoid()
			{
				@Override
				public void visitNamedMethod(NapileNamedMethod function)
				{
					SimpleMethodDescriptor functionDescriptor = descriptorResolver.resolveFunctionDescriptor(ownerDescription, scope, function, trace);
					ownerDescription.getBuilder().addMethodDescriptor(functionDescriptor);

					context.getMethods().put(function, functionDescriptor);
					context.getDeclaringScopes().put(function, scope);
				}

				@Override
				public void visitConstructor(NapileConstructor constructor)
				{
					ConstructorDescriptor constructorDescriptor = descriptorResolver.resolveConstructorDescriptor(scope, ownerDescription, constructor, trace);

					ownerDescription.getBuilder().addConstructorDescriptor(constructorDescriptor);

					context.getConstructors().put(constructor, constructorDescriptor);
					context.getDeclaringScopes().put(constructor, scope);
				}

				@Override
				public void visitStaticConstructor(NapileStaticConstructor staticConstructor)
				{
					ConstructorDescriptor constructorDescriptor = descriptorResolver.resolveStaticConstructorDescriptor(scope, ownerDescription, staticConstructor, trace);

					ownerDescription.getBuilder().addStaticConstructorDescriptor(constructorDescriptor);

					context.getDeclaringScopes().put(staticConstructor, scope);
				}

				@Override
				public void visitProperty(NapileProperty property)
				{
					PropertyDescriptor propertyDescriptor = descriptorResolver.resolvePropertyDescriptor(ownerDescription, scope, property, trace);

					ownerDescription.getBuilder().addPropertyDescriptor(propertyDescriptor);

					context.getProperties().put(property, propertyDescriptor);
					context.getDeclaringScopes().put(property, scope);
				}

				@Override
				public void visitRetellEntry(NapileRetellEntry retellEntry)
				{
					PropertyDescriptor propertyDescriptor = descriptorResolver.resolvePropertyDescriptor(ownerDescription, scope, retellEntry, trace);

					ownerDescription.getBuilder().addPropertyDescriptor(propertyDescriptor);

					context.getRetellEntries().put(retellEntry, propertyDescriptor);
					context.getDeclaringScopes().put(retellEntry, scope);
				}

				@Override
				public void visitEnumEntry(NapileEnumEntry enumEntry)
				{
					PropertyDescriptor propertyDescriptor = new PropertyDescriptor(ownerDescription, new ArrayList<AnnotationDescriptor>(), Modality.FINAL, Visibility.PUBLIC, NapilePsiUtil.safeName(enumEntry.getName()), CallableMemberDescriptor.Kind.DECLARATION, true);
					trace.record(BindingContext.VARIABLE, enumEntry, propertyDescriptor);

					MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(ownerDescription, scope, ClassKind.ENUM_ENTRY, propertyDescriptor.getName(), true);
					mutableClassDescriptor.setModality(Modality.FINAL);
					mutableClassDescriptor.setVisibility(Visibility.PUBLIC);
					mutableClassDescriptor.setTypeParameterDescriptors(new ArrayList<TypeParameterDescriptor>());
					mutableClassDescriptor.addSupertype(new JetTypeImpl(Collections.<AnnotationDescriptor>emptyList(), ownerDescription.getTypeConstructor(), false, typeResolver.resolveTypes(scope, enumEntry.getTypeArguments(), trace, false), scope));
					mutableClassDescriptor.createTypeConstructor();

					trace.record(BindingContext.CLASS, enumEntry, mutableClassDescriptor);

					propertyDescriptor.setType(new JetTypeImpl(mutableClassDescriptor), Collections.<TypeParameterDescriptor>emptyList(), ReceiverDescriptor.NO_RECEIVER);

					context.getEnumEntries().put(enumEntry, mutableClassDescriptor);

					context.getDeclaringScopes().put(enumEntry, scope);
					ownerDescription.getBuilder().addPropertyDescriptor(propertyDescriptor);
				}
			});
		}
	}

	private void checkRedeclarationsInNamespaces()
	{
		for(NamespaceDescriptorImpl descriptor : context.getNamespaceDescriptors().values())
		{
			Multimap<Name, DeclarationDescriptor> simpleNameDescriptors = descriptor.getMemberScope().getDeclaredDescriptorsAccessibleBySimpleName();
			for(Name name : simpleNameDescriptors.keySet())
			{
				Collection<DeclarationDescriptor> descriptors = simpleNameDescriptors.get(name);

				if(descriptors.size() > 1)
				{
					for(DeclarationDescriptor declarationDescriptor : descriptors)
					{
						for(PsiElement declaration : getDeclarationsByDescriptor(declarationDescriptor))
						{
							assert declaration != null;
							trace.report(REDECLARATION.on(declaration, declarationDescriptor.getName().getName()));
						}
					}
				}
			}
		}
	}

	private Collection<PsiElement> getDeclarationsByDescriptor(DeclarationDescriptor declarationDescriptor)
	{
		Collection<PsiElement> declarations;
		if(declarationDescriptor instanceof NamespaceDescriptor)
		{
			final NamespaceDescriptor namespace = (NamespaceDescriptor) declarationDescriptor;
			Collection<NapileFile> files = trace.get(BindingContext.NAMESPACE_TO_FILES, namespace);

			if(files == null)
			{
				throw new IllegalStateException("declarations corresponding to " + namespace + " are not found");
			}

			declarations = Collections2.transform(files, new Function<NapileFile, PsiElement>()
			{
				@Override
				public PsiElement apply(@Nullable NapileFile file)
				{
					assert file != null : "File is null for namespace " + namespace;
					return file.getNamespaceHeader().getNameIdentifier();
				}
			});
		}
		else
		{
			declarations = Collections.singletonList(BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), declarationDescriptor));
		}
		return declarations;
	}

	private void checkRedeclarationsInInnerClassNames()
	{
		for(MutableClassDescriptor classDescriptor : context.getClasses().values())
		{
			Collection<DeclarationDescriptor> allDescriptors = classDescriptor.getScopeForMemberLookup().getOwnDeclaredDescriptors();

			Multimap<Name, DeclarationDescriptor> descriptorMap = HashMultimap.create();
			for(DeclarationDescriptor desc : allDescriptors)
			{
				if(desc instanceof ClassDescriptor)
				{
					descriptorMap.put(desc.getName(), desc);
				}
			}

			for(Name name : descriptorMap.keySet())
			{
				Collection<DeclarationDescriptor> descriptors = descriptorMap.get(name);
				if(descriptors.size() > 1)
				{
					for(DeclarationDescriptor descriptor : descriptors)
					{
						trace.report(REDECLARATION.on(BindingContextUtils.classDescriptorToDeclaration(trace.getBindingContext(), (ClassDescriptor) descriptor), descriptor.getName().getName()));
					}
				}
			}
		}
	}
}
