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

import static org.napile.compiler.lang.diagnostics.Errors.REDECLARATION;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptorLite;
import org.napile.compiler.lang.descriptors.NamespaceDescriptor;
import org.napile.compiler.lang.descriptors.NamespaceDescriptorImpl;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.SimpleFunctionDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.name.Name;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
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

	public void process(@NotNull JetScope rootScope)
	{
		resolveAnnotationStubsOnClassesAndConstructors();
		resolveFunctionAndPropertyHeaders();
		importsResolver.processMembersImports(rootScope);
		checkRedeclarationsInNamespaces();
		checkRedeclarationsInInnerClassNames();
	}


	private void resolveAnnotationStubsOnClassesAndConstructors()
	{
		for(Map.Entry<NapileClass, MutableClassDescriptor> entry : context.getClasses().entrySet())
		{
			NapileClass napileClass = entry.getKey();
			MutableClassDescriptor descriptor = entry.getValue();
			resolveAnnotationsForClassOrObject(annotationResolver, napileClass, descriptor);
		}
		for(Map.Entry<NapileObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet())
		{
			NapileObjectDeclaration objectDeclaration = entry.getKey();
			MutableClassDescriptor descriptor = entry.getValue();
			resolveAnnotationsForClassOrObject(annotationResolver, objectDeclaration, descriptor);
		}
	}

	private void resolveAnnotationsForClassOrObject(AnnotationResolver annotationResolver, NapileClassOrObject jetClass, MutableClassDescriptor descriptor)
	{
		NapileModifierList modifierList = jetClass.getModifierList();
		if(modifierList != null)
		{
			descriptor.getAnnotations().addAll(annotationResolver.resolveAnnotations(descriptor.getScopeForSupertypeResolution(), modifierList.getAnnotationEntries(), trace));
		}
	}

	private void resolveFunctionAndPropertyHeaders()
	{
		for(Map.Entry<NapileClass, MutableClassDescriptor> entry : context.getClasses().entrySet())
		{
			NapileClass napileClass = entry.getKey();
			MutableClassDescriptor classDescriptor = entry.getValue();

			resolveFunctionAndPropertyHeaders(napileClass.getDeclarations(), classDescriptor.getScopeForMemberResolution(), classDescriptor.getScopeForInitializers(), classDescriptor.getScopeForMemberResolution(), classDescriptor);
		}

		for(Map.Entry<NapileObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet())
		{
			NapileObjectDeclaration object = entry.getKey();
			MutableClassDescriptor classDescriptor = entry.getValue();

			resolveFunctionAndPropertyHeaders(object.getDeclarations(), classDescriptor.getScopeForMemberResolution(), classDescriptor.getScopeForInitializers(), classDescriptor.getScopeForMemberResolution(), classDescriptor);
		}
	}

	private void resolveFunctionAndPropertyHeaders(@NotNull List<? extends NapileDeclaration> declarations, final @NotNull JetScope scopeForFunctions, final @NotNull JetScope scopeForPropertyInitializers, final @NotNull JetScope scopeForPropertyAccessors, final @NotNull MutableClassDescriptor ownerDescription)
	{
		for(NapileDeclaration declaration : declarations)
		{
			declaration.accept(new NapileVisitorVoid()
			{
				@Override
				public void visitNamedFunction(NapileNamedFunction function)
				{
					SimpleFunctionDescriptor functionDescriptor = descriptorResolver.resolveFunctionDescriptor(ownerDescription, scopeForFunctions, function, trace);
					ownerDescription.getBuilder().addFunctionDescriptor(functionDescriptor);

					context.getFunctions().put(function, functionDescriptor);
					context.getDeclaringScopes().put(function, scopeForFunctions);
				}

				@Override
				public void visitConstructor(NapileConstructor function)
				{
					ConstructorDescriptor napileConstructor = descriptorResolver.resolveConstructorDescriptor(scopeForFunctions, ownerDescription, function, trace);

					context.getConstructors().put(function, napileConstructor);
					context.getDeclaringScopes().put(function, scopeForFunctions);
				}

				@Override
				public void visitProperty(NapileProperty property)
				{
					PropertyDescriptor propertyDescriptor = descriptorResolver.resolvePropertyDescriptor(ownerDescription, scopeForPropertyInitializers, property, trace);

					ownerDescription.getBuilder().addPropertyDescriptor(propertyDescriptor);

					context.getProperties().put(property, propertyDescriptor);
					context.getDeclaringScopes().put(property, scopeForPropertyInitializers);
					if(property.getGetter() != null)
						context.getDeclaringScopes().put(property.getGetter(), scopeForPropertyAccessors);
					if(property.getSetter() != null)
						context.getDeclaringScopes().put(property.getSetter(), scopeForPropertyAccessors);
				}

				@Override
				public void visitObjectDeclaration(NapileObjectDeclaration declaration)
				{
					PropertyDescriptor propertyDescriptor = descriptorResolver.resolveObjectDeclarationAsPropertyDescriptor(ownerDescription, declaration, context.getObjects().get(declaration), trace);

					//ownerDescription.addPropertyDescriptor(propertyDescriptor);
				}

				@Override
				public void visitEnumEntry(NapileEnumEntry enumEntry)
				{
					PropertyDescriptor propertyDescriptor = descriptorResolver.resolvePropertyDescriptorFromEnumEntry(ownerDescription, scopeForPropertyInitializers, enumEntry, trace);

					//context.getProperties().put(property, propertyDescriptor);
					//context.getDeclaringScopes().put(enumEntry, scopeForPropertyInitializers);

					//if(enumEntry.getPrimaryConstructorParameterList() == null)
					{
						// FIX: Bad cast
						//MutableClassDescriptorLite classObjectDescriptor = ((MutableClassDescriptorLite) ownerDescription).getClassObjectDescriptor();
						//assert classObjectDescriptor != null;
						//PropertyDescriptor propertyDescriptor = descriptorResolver.resolveObjectDeclarationAsPropertyDescriptor(ownerDescription, enumEntry, context.getClasses().get(enumEntry), trace);
					//	ownerDescription.getBuilder().addPropertyDescriptor(propertyDescriptor);
					//	classObjectDescriptor.getBuilder().addPropertyDescriptor(propertyDescriptor);
					}
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
				// Keep only properties with no receiver
				Collection<DeclarationDescriptor> descriptors = Collections2.filter(simpleNameDescriptors.get(name), new Predicate<DeclarationDescriptor>()
				{
					@Override
					public boolean apply(@Nullable DeclarationDescriptor descriptor)
					{
						if(descriptor instanceof PropertyDescriptor)
						{
							PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
							return !propertyDescriptor.getReceiverParameter().exists();
						}
						return true;
					}
				});
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

			MutableClassDescriptorLite classObj = classDescriptor.getClassObjectDescriptor();
			if(classObj != null)
			{
				Collection<DeclarationDescriptor> classObjDescriptors = classObj.getScopeForMemberLookup().getOwnDeclaredDescriptors();
				if(!classObjDescriptors.isEmpty())
				{
					allDescriptors = Lists.newArrayList(allDescriptors);
					allDescriptors.addAll(classObjDescriptors);
				}
			}

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
