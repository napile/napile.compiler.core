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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.PackageDescriptor;
import org.napile.compiler.lang.descriptors.SimpleMethodDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingContextUtils;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.TopDownAnalysisContext;
import org.napile.compiler.lang.resolve.processors.members.AnnotationResolver;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.resolve.scopes.WritableScope;
import org.napile.compiler.util.PluginKeys;
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
		annotationResolver.resolveBindAnnotations(trace);
		importsResolver.processMembersImports(rootScope);
		checkRedeclarationsInNamespaces();
		checkRedeclarationsInInnerClassNames();
	}

	private void resolveDeclarations()
	{
		for(Map.Entry<NapileClass, MutableClassDescriptor> entry : context.getClasses().entrySet())
		{
			NapileClass napileClass = entry.getKey();
			MutableClassDescriptor classDescriptor = entry.getValue();

			resolveInsideDeclarations(napileClass, classDescriptor.getScopeForMemberResolution(), classDescriptor);
		}

		//for(Map.Entry<NapileAnonymClass, MutableClassDescriptor> entry : context.getAnonymous().entrySet())
		//{
		//	NapileAnonymClass object = entry.getKey();
		//	MutableClassDescriptor classDescriptor = entry.getValue();

		//	resolveInsideDeclarations(object, classDescriptor.getScopeForMemberResolution(), classDescriptor);
		//}
	}

	public void resolveInsideDeclarations(@NotNull NapileDeclarationContainer<NapileDeclaration> declarationOwner, final @NotNull JetScope scope, final @NotNull MutableClassDescriptor ownerDescription)
	{
		for(NapileDeclaration declaration : declarationOwner.getDeclarations())
		{
			declaration.accept(new NapileVisitorVoid()
			{
				@Override
				public void visitNamedMethod(NapileNamedMethod method)
				{
					SimpleMethodDescriptor methodDescriptor = descriptorResolver.resolveMethodDescriptor(ownerDescription, scope, method, trace);
					method.putUserData(PluginKeys.DESCRIPTOR_KEY, methodDescriptor);
					ownerDescription.getBuilder().addMethodDescriptor(methodDescriptor);

					context.getMethods().put(method, methodDescriptor);
					context.getDeclaringScopes().put(method, scope);
				}

				@Override
				public void visitNamedMacro(NapileNamedMacro macro)
				{
					SimpleMethodDescriptor methodDescriptor = descriptorResolver.resolveMacroDescriptor(ownerDescription, scope, macro, trace);
					macro.putUserData(PluginKeys.DESCRIPTOR_KEY, methodDescriptor);
					ownerDescription.getBuilder().addMethodDescriptor(methodDescriptor);

					context.getMethods().put(macro, methodDescriptor);
					context.getDeclaringScopes().put(macro, scope);
				}

				@Override
				public void visitConstructor(NapileConstructor constructor)
				{
					ConstructorDescriptor constructorDescriptor = descriptorResolver.resolveConstructorDescriptor(scope, ownerDescription, constructor, trace);
					constructor.putUserData(PluginKeys.DESCRIPTOR_KEY, constructorDescriptor);
					ownerDescription.getBuilder().addConstructorDescriptor(constructorDescriptor);

					context.getConstructors().put(constructor, constructorDescriptor);
					context.getDeclaringScopes().put(constructor, scope);
				}

				@Override
				public void visitVariable(NapileVariable variable)
				{
					VariableDescriptor variableDescriptor = descriptorResolver.resolveVariableDescriptor(ownerDescription, scope, variable, trace);
					variable.putUserData(PluginKeys.DESCRIPTOR_KEY, variableDescriptor);
					ownerDescription.getBuilder().addVariableDescriptor(variableDescriptor);

					context.getVariables().put(variable, variableDescriptor);
					context.getDeclaringScopes().put(variable, scope);
				}

				@Override
				public void visitEnumValue(NapileEnumValue value)
				{
					MutableClassDescriptor mutableClassDescriptor = context.getEnumValues().get(value);
					VariableDescriptor variableDescriptor = descriptorResolver.resolveVariableDescriptor(ownerDescription, scope, value, mutableClassDescriptor, trace);
					value.putUserData(PluginKeys.DESCRIPTOR_KEY, variableDescriptor);
					ownerDescription.getBuilder().addVariableDescriptor(variableDescriptor);

					context.getVariables().put(value, variableDescriptor);
					context.getDeclaringScopes().put(value, scope);
				}
			});
		}
	}

	private void checkRedeclarationsInNamespaces()
	{
		for(PackageDescriptor descriptor : context.getPackages().values())
		{
			Multimap<Name, DeclarationDescriptor> simpleNameDescriptors = ((WritableScope) descriptor.getMemberScope()).getDeclaredDescriptorsAccessibleBySimpleName();
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
		if(declarationDescriptor instanceof PackageDescriptor)
		{
			final PackageDescriptor namespace = (PackageDescriptor) declarationDescriptor;
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
					return file.getPackage().getLastPartExpression();
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
